package com.fabienli.dokuwiki;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Toast;

import com.fabienli.dokuwiki.db.AppDatabase;
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
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    // UI access
    protected WebView _webView = null;
    protected EditText _editTextView = null;


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
            public void progressUpdate(String header, Integer... values) {
                if(values.length>=2){
                    notificationHandler.updateNotification(header +" : "+values[0]+"/"+values[1]);
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
                loadPage(content);
            }
        });
    }

    public void retrievePageHTMLforDisplay(String pagename, final WebView webview){
        this._webView = webview;
        _currentPageName = pagename;
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

    public String getLogsHtml() {
        String unencodedHtml = "<html><body><ul>";
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
            Log.d(TAG, "Display page: "+html);
            String aBaseUrl = "file://"+context.getCacheDir().getAbsolutePath();
            Log.d(TAG, "Base Url: "+aBaseUrl);
            _webView.loadDataWithBaseURL(aBaseUrl, html, "text/html", "UTF-8", null);
            Log.d(TAG, "Loaded page ");
        }
    }



    private void writeDefaultCss() {
        // ugly copy paste of a few CSS properties
        // to be put in a real file
        // or to be downloaded from server
        if (context != null) {
            File cssFile = new File(context.getCacheDir(), "default.css");
            Log.d(TAG, "writing css: " +cssFile.getAbsolutePath());
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(cssFile.getAbsolutePath()), "utf-8"))) {
                writer.write("html,\n" +
                        " body {\n" +
                        "  color:#331;\n" +
                        "  background:#fbfaf9;\n" +

                        " }\n" +
                        " body {\n" +
                        "  font:normal 87.5%/1.4 Arial,sans-serif;\n" +
                        "  -webkit-text-size-adjust:100%;\n" +
                        " }\n" +
                        " h1,\n" +
                        " h2,\n" +
                        " h3,\n" +
                        " h4,\n" +
                        " h5,\n" +
                        " h6 {\n" +
                        "  font-weight:bold;\n" +
                        "  padding:0;\n" +
                        "  line-height:1.2;\n" +
                        "  clear:left;\n" +
                        " }\n" +
                        " [dir=rtl] h1,\n" +
                        " [dir=rtl] h2,\n" +
                        " [dir=rtl] h3,\n" +
                        " [dir=rtl] h4,\n" +
                        " [dir=rtl] h5,\n" +
                        " [dir=rtl] h6 {\n" +
                        "  clear:right;\n" +
                        " }\n" +
                        " h1 {\n" +
                        "  font-size:2em;\n" +
                        "  margin:0 0 .444em;\n" +
                        " }\n" +
                        " h2 {\n" +
                        "  font-size:1.5em;\n" +
                        "  margin:0 0 .666em;\n" +
                        " }\n" +
                        " h3 {\n" +
                        "  font-size:1.125em;\n" +
                        "  margin:0 0 .888em;\n" +
                        " }\n" +
                        " h4 {\n" +
                        "  font-size:1em;\n" +
                        "  margin:0 0 1em;\n" +
                        " }\n" +
                        " h5 {\n" +
                        "  font-size:.875em;\n" +
                        "  margin:0 0 1.1428em;\n" +
                        " }\n" +
                        " h6 {\n" +
                        "  font-size:.75em;\n" +
                        "  margin:0 0 1.333em;\n" +
                        " }\n");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void refreshPage() {
        Log.d(TAG, "refreshing the page");
        //_webView.clearView();
        _webView.reload();
    }

    public void displayActionListPage(WebView webView) {
        Logs.getInstance().add("Show the list of SyncAction from local db");
        _webView = webView;
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
        StaticPagesDisplay staticPagesDisplay = new StaticPagesDisplay(_db, context.getCacheDir().getAbsolutePath());
        staticPagesDisplay.getCreatePageHtmlAsync(new PageHtmlRetrieveCallback() {
            @Override
            public void pageRetrieved(String content) {
                loadPage(content);
            }
        });
    }

    public void mediaManagerPageHtml(WebView webView) {
        Logs.getInstance().add("Show the first page to handle medias");
        _webView = webView;
        StaticPagesDisplay staticPagesDisplay = new StaticPagesDisplay(_db, context.getCacheDir().getAbsolutePath());
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
                mediaManagerPageHtml(webView);
            }
        });
    }
}
