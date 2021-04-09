package com.fabienli.dokuwiki;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Toast;

import com.fabienli.dokuwiki.db.AppDatabase;
import com.fabienli.dokuwiki.sync.StaticDownloader;
import com.fabienli.dokuwiki.sync.XmlRpcAdapter;
import com.fabienli.dokuwiki.tools.Logs;
import com.fabienli.dokuwiki.usecase.ActionListRetrieve;
import com.fabienli.dokuwiki.usecase.MediaImport;
import com.fabienli.dokuwiki.usecase.MediaRetrieve;
import com.fabienli.dokuwiki.usecase.NotificationHandler;
import com.fabienli.dokuwiki.usecase.PageHtmlRetrieve;
import com.fabienli.dokuwiki.usecase.PageHtmlRetrieveForceDownload;
import com.fabienli.dokuwiki.usecase.PageListRetrieve;
import com.fabienli.dokuwiki.usecase.PageTextRetrieve;
import com.fabienli.dokuwiki.usecase.PageTextRetrieveForceDownload;
import com.fabienli.dokuwiki.usecase.PageTextSave;
import com.fabienli.dokuwiki.usecase.SearchWikiListRetrieve;
import com.fabienli.dokuwiki.usecase.StaticMediaManagerDisplay;
import com.fabienli.dokuwiki.usecase.StaticPagesDisplay;
import com.fabienli.dokuwiki.usecase.UrlConverter;
import com.fabienli.dokuwiki.usecase.WikiSynchronizer;
import com.fabienli.dokuwiki.usecase.callback.MediaRetrieveCallback;
import com.fabienli.dokuwiki.usecase.callback.PageHtmlRetrieveCallback;
import com.fabienli.dokuwiki.usecase.callback.WikiSynchroCallback;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Stack;
import androidx.room.Room;

public class WikiCacheUiOrchestrator {
    // only one instance of this class
    static WikiCacheUiOrchestrator _instance = null;
    // used for logs
    static String TAG = "WikiCacheUiOrchestrator";
    // handy common object
    protected Context context;
    // Cache and db accessor
    protected AppDatabase _db;
    // ongoing status
    public String _currentPageName = "";
    // Status on current Search
    public boolean _isSearchDone = false;
    // UI access
    protected WebView _webView = null;
    protected EditText _editTextView = null;
    protected Stack<String> _pageHistory;
    // strings for history pages
    final String APP_INTERNAL_PAGE_PREFIX = "#*com.fabienli.dokuwiki#";
    final String PAGE_MEDIA_FULLSCREEN = "PAGE_MEDIA_FULLSCREEN";
    final String PAGE_MEDIA_MANAGER = "PAGE_MEDIA_MANAGER";
    final String PAGE_CREATE_PAGE = "PAGE_CREATE_PAGE";
    final String PAGE_ACTION_LIST = "PAGE_ACTION_LIST";
    final String PAGE_PAGES_LIST = "PAGE_PAGES_LIST";
    final String PAGE_LOGS = "PAGE_LOGS";
    private static final String SEARCH_QUERY = "SEARCH_QUERY";

    // initialisation
    public static WikiCacheUiOrchestrator instance(Context ictx){
        if(_instance == null){
            _instance = new WikiCacheUiOrchestrator(ictx);
        }
        // always keep current context up to date
        _instance.context = ictx;
        return _instance;
    }

    public static WikiCacheUiOrchestrator instance(){
        if(_instance == null){
            Log.e(TAG, "instantiated without context, this might fail !!!");
            _instance = new WikiCacheUiOrchestrator(null);
        }
        return _instance;
    }

    private WikiCacheUiOrchestrator(Context ictx) {
        context = ictx;
        _pageHistory = new Stack<>();
        if(context != null)
          _db = Room.databaseBuilder(ictx.getApplicationContext(),
                AppDatabase.class, "localcache")
                .addMigrations(AppDatabase.MIGRATION_1_2)
                .addMigrations(AppDatabase.MIGRATION_2_3)
                .addMigrations(AppDatabase.MIGRATION_3_4)
                .build();
        writeDefaultCss();
    }

    void updatePageListFromServer(){
        final NotificationHandler notificationHandler = new NotificationHandler(context);
        notificationHandler.createNotification("Synchronisation starting");

        WikiSynchronizer wikiSynchronizer = new WikiSynchronizer(PreferenceManager.getDefaultSharedPreferences(context), _db, new XmlRpcAdapter(context), context.getCacheDir().getPath());
        wikiSynchronizer.retrieveDataFromServerAsync(new WikiSynchroCallback() {

            @Override
            public void progressUpdate(String header, String footer, Integer... values) {
                if(values.length>=2){
                    String realFooter = footer;
                    if(footer.length()>0)
                        realFooter= " "+footer;

                    String realHeader = header;
                    if(header.length()>0)
                        realHeader= header + " : ";

                    String notifTitle = realHeader + values[0]+"/"+values[1] + realFooter;
                    notificationHandler.updateNotification(notifTitle);
                    Log.d(TAG, notifTitle);
                }
            }

            @Override
            public void onceDone() {
                notificationHandler.removeNotification();
            }

        });
    }

    public void forceDownloadPageHTMLforDisplay(WebView webview) {
        this._webView = webview;
        PageHtmlRetrieveForceDownload pageHtmlRetrieveForceDownload = new PageHtmlRetrieveForceDownload(_db, new XmlRpcAdapter(context));
        pageHtmlRetrieveForceDownload.retrievePageAsync(_currentPageName, new PageHtmlRetrieveCallback() {
            @Override
            public void pageRetrieved(String content){
                PageTextRetrieve pageTextRetrieve = new PageTextRetrieveForceDownload(_db, new XmlRpcAdapter(context));
                pageTextRetrieve.retrievePageAsync(_currentPageName, new PageHtmlRetrieveCallback() {
                    @Override
                    public void pageRetrieved(String content2) {
                        loadPage(content);
                    }
                });
            }
        });
    }

    public void addPageToHistory(String pageUrl){
        if(_pageHistory.size()==0 || _pageHistory.lastElement().compareTo(pageUrl)!=0){
            _pageHistory.push(pageUrl);
        }

    }

    public void retrievePageHTMLforDisplay(String pagename, final WebView webview){
        this._webView = webview;
        _currentPageName = pagename;
        Log.d(TAG, "Requested page: "+pagename);
        addPageToHistory(pagename);
        PageHtmlRetrieve aPageHtmlRetrieve = new PageHtmlRetrieve(_db, new XmlRpcAdapter(context));
        aPageHtmlRetrieve.retrievePageAsync(pagename, new PageHtmlRetrieveCallback() {
            @Override
            public void pageRetrieved(String content){
                loadPage(content);
            }
        });
        // workaround to avoid intermediate error page
        this.loadPage("<html><body>Please wait ...</body></html>");
    }

    public void retrievePageEdit(final String pagename, EditText iEditTextView, Boolean forceDownload) {
        _editTextView = iEditTextView;
        _currentPageName = pagename;
        PageTextRetrieve pageTextRetrieve;
        if(forceDownload)
            pageTextRetrieve = new PageTextRetrieveForceDownload(_db, new XmlRpcAdapter(context));
        else
            pageTextRetrieve = new PageTextRetrieve(_db, new XmlRpcAdapter(context));
        pageTextRetrieve.retrievePageAsync(pagename, new PageHtmlRetrieveCallback() {
            @Override
            public void pageRetrieved(String content) {
                _editTextView.setText(content);
            }
        });
    }

    public void updateTextPage(final String pagename, String newtext) {
        PageTextSave pageTextSave = new PageTextSave(_db, PreferenceManager.getDefaultSharedPreferences(context), new XmlRpcAdapter(context));
        pageTextSave.savePageTextAsync(pagename, newtext, new WikiSynchroCallback() {
            @Override
            public void onceDone() {
                Logs.getInstance().add("Saved new version of page: "+pagename);
                // refresh the html page
                retrievePageHTMLforDisplay(pagename, _webView);
            }
        });
    }

    public void displayPageListHtml(WebView webview) {
        this._webView = webview;
        addPageToHistory(APP_INTERNAL_PAGE_PREFIX + PAGE_PAGES_LIST);
        PageListRetrieve pageListRetrieve = new PageListRetrieve(_db);
        pageListRetrieve.getPageListAsync(new PageHtmlRetrieveCallback() {
            @Override
            public void pageRetrieved(String content){
                loadPage(content);
            }
        });
        // workaround to avoid intermediate error page
        this.loadPage("<html><body>Please wait ...</body></html>");
    }

    public void retrieveSearchResultsforDisplay(String searchData, WebView webview) {
        this._webView = webview;
        addPageToHistory(SEARCH_QUERY + searchData);
        SearchWikiListRetrieve searchWikiResultPage = new SearchWikiListRetrieve(_db);
        searchWikiResultPage.setSearchData(searchData);
        searchWikiResultPage.getPageResultsListAsync(new PageHtmlRetrieveCallback() {
            @Override
            public void pageRetrieved(String content){
                loadPage(content);
                ((MainActivity)context).enableSearchButton(true);
                // we block further random popup of the search box with a reset search indicators:
                ((MainActivity)context).preventSearch();
            }
        });
        // workaround to avoid intermediate error page
        this.loadPage("<html><body>Please wait while searching for '"+searchData+"'...</body></html>");
    }

    public String getLogsHtml() {
        addPageToHistory(APP_INTERNAL_PAGE_PREFIX + PAGE_LOGS);
        String unencodedHtml = "<html><body><ul>";
        Logs.getInstance().purgeToMax(); // ensure not too many items
        for (String a : Logs.getInstance()._data) {
            unencodedHtml += "\n<li>" + a + "</li>";
        }
        unencodedHtml += "\n</ul></body></html>";
        Log.d("getLogsHtml", unencodedHtml);
        return unencodedHtml;
    }

    public void ensureMediaIsDownloaded(String mediaId, String mediaLocalPath, int width, int height) {
        Log.d(TAG, "Check if file "+mediaId+" needs to be downloaded");
        MediaRetrieve mediaRetrieve = new MediaRetrieve(_db, new XmlRpcAdapter(context), context.getCacheDir().getAbsolutePath());
        mediaRetrieve.getMediaAsync(mediaId, mediaLocalPath, width, height, new MediaRetrieveCallback(){
            @Override
            public void mediaRetrieved(String mediaPathName) {
                //refreshPage();
                Toast.makeText(context,"Media downloaded, you can refresh the page", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void mediaWasAlreadyThere(String mediaPathName) {
            }
        });
    }
    public void ensureStaticIsDownloaded(String imageStr) {
        Log.d(TAG, "Check if file "+imageStr+" needs to be downloaded");
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String urlserver = settings.getString("serverurl", "");
        StaticDownloader staticDownloader = new StaticDownloader(urlserver, context.getCacheDir().getAbsolutePath());
        staticDownloader.getStaticAsync(imageStr, new MediaRetrieveCallback(){
            @Override
            public void mediaRetrieved(String mediaPathName) {
                //refreshPage();
                Toast.makeText(context,"Static downloaded, you can refresh the page", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void mediaWasAlreadyThere(String mediaPathName) {
            }
        });
    }

    public void loadPage(String iPageData) {
        if(_webView == null) {
            // try to guess ourselves where is the webview
            _webView = (WebView) ((MainActivity)context).findViewById(R.id.webview);
        }
        if(_webView != null) {
            UrlConverter urlConverter = new UrlConverter(context.getCacheDir().getAbsolutePath());
            String html = urlConverter.getHtmlContentConverted(iPageData);
            for(UrlConverter.ImageRefData img : urlConverter._imageList){
                ensureMediaIsDownloaded(img.id, img.imageFilePath, img.width, img.height);
            }
            for(String imageUrlStr : urlConverter._staticImageList) {
                ensureStaticIsDownloaded(imageUrlStr);
            }
            Log.d(TAG, "Display page: "+html);
            String aBaseUrl = "file://"+context.getCacheDir().getAbsolutePath();
            Log.d(TAG, "Base Url: "+aBaseUrl);
            _webView.loadDataWithBaseURL(aBaseUrl, html, "text/html", "UTF-8", null);
            Log.d(TAG, "Loaded page ");
            ((MainActivity)context).enableSearchButton(false);
        }
    }

    private void writeDefaultCss() {
        if (context != null) {
            InputStream is = context.getResources().openRawResource(R.raw.default_css);
            try {
                File cssFile = new File(context.getCacheDir().getAbsolutePath(), "default_css.css");
                FileOutputStream fos = new FileOutputStream(cssFile);
                byte[] buffer = new byte[is.available()];
                is.read(buffer);
                fos.write(buffer);
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // other css files ?
        }
    }

    public void refreshPage() {
        Log.d(TAG, "refreshing the page");
        //_webView.clearView();
        //_webView.reload();
        if(! _pageHistory.empty())
            retrievePageHTMLforDisplay(_pageHistory.lastElement(), this._webView);
    }

    public boolean showLastHistory(WebView webView) {
        Logs.getInstance().add("Show last page in history");
        if(_pageHistory.size()>0) {
            if(_pageHistory.lastElement().startsWith(APP_INTERNAL_PAGE_PREFIX)){
                String page = _pageHistory.lastElement().substring(APP_INTERNAL_PAGE_PREFIX.length());
                switch (page){
                    case PAGE_MEDIA_MANAGER:
                        mediaManagerPageHtml(webView, ""); // TODO: handle media manager history
                        break;
                    case PAGE_CREATE_PAGE:
                        createNewPageHtml(webView);
                        break;
                    case PAGE_ACTION_LIST:
                        displayActionListPage(webView);
                        break;
                    case PAGE_PAGES_LIST:
                        displayPageListHtml(_webView);
                        break;
                    case PAGE_LOGS:
                        getLogsHtml(); // TODO: have a real method to display logs
                        break;
                }
            }
            else if(_pageHistory.lastElement().startsWith(SEARCH_QUERY)) {
                String searchData = _pageHistory.lastElement().substring(SEARCH_QUERY.length());
                retrieveSearchResultsforDisplay(searchData, _webView);
            }
            else
                retrievePageHTMLforDisplay(_pageHistory.lastElement(), webView);
            return true;
        }
        return false;
    }

    public boolean backHistory(WebView webView) {
        Logs.getInstance().add("Back one page in history");
        if(_pageHistory.size()>1) {
            // remove current item
            _pageHistory.pop();
            return showLastHistory(webView);
        }
        return false;
    }

    public void displayActionListPage(WebView webView) {
        Logs.getInstance().add("Show the list of SyncAction from local db");
        _webView = webView;
        addPageToHistory(APP_INTERNAL_PAGE_PREFIX + PAGE_ACTION_LIST);
        ActionListRetrieve actionListRetrieve = new ActionListRetrieve(_db);
        actionListRetrieve.getSyncActionListAsync(new PageHtmlRetrieveCallback() {
            @Override
            public void pageRetrieved(String content) {
                loadPage(content);
            }
        });
    }

    public void createNewPageHtml(WebView webView) {
        Logs.getInstance().add("Show the first page to create a new page");
        _webView = webView;
        addPageToHistory(APP_INTERNAL_PAGE_PREFIX + PAGE_CREATE_PAGE);
        StaticPagesDisplay staticPagesDisplay = new StaticPagesDisplay(_db, context.getCacheDir().getAbsolutePath());
        staticPagesDisplay.getCreatePageHtmlAsync(new PageHtmlRetrieveCallback() {
            @Override
            public void pageRetrieved(String content) {
                loadPage(content);
            }
        });
    }

    public void mediaManagerPageHtml(WebView webView, String args) {
        Logs.getInstance().add("Show the first page to handle medias");
        _webView = webView;
        addPageToHistory(APP_INTERNAL_PAGE_PREFIX + PAGE_MEDIA_MANAGER);
        StaticMediaManagerDisplay staticPagesDisplay = new StaticMediaManagerDisplay(_db, context.getCacheDir().getAbsolutePath());
        staticPagesDisplay.setMediaManagerParams(args);
        staticPagesDisplay.getMediaPageHtmlAsync(new PageHtmlRetrieveCallback() {
            @Override
            public void pageRetrieved(String content) {
                loadPage(content);
            }
        });
    }

    public void savePictureAndShowMediaManagerPageHtml(String newFileName, InputStream imageStream, WebView webView) {
        MediaImport mediaImport = new MediaImport(_db, context.getCacheDir().getAbsolutePath());
        mediaImport.importNewMediaAsync(newFileName, imageStream, new MediaRetrieveCallback() {
            @Override
            public void mediaRetrieved(String mediaPathName) {
                mediaManagerPageHtml(webView, ""); // TODO: handle media manager history
            }
        });
    }


}
