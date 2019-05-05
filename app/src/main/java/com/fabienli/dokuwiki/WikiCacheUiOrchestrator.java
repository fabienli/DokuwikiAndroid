package com.fabienli.dokuwiki;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Base64;
import android.util.Log;
import android.webkit.WebView;
import android.widget.EditText;

import com.fabienli.dokuwiki.cache.WikiPage;
import com.fabienli.dokuwiki.cache.WikiPageList;
import com.fabienli.dokuwiki.db.AppDatabase;
import com.fabienli.dokuwiki.db.DbUsecaseHandler;
import com.fabienli.dokuwiki.db.Media;
import com.fabienli.dokuwiki.db.Page;
import com.fabienli.dokuwiki.sync.SyncUsecaseHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
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
    public WikiPageList _wikiPageList;
    protected AppDatabase _db;
    protected DbUsecaseHandler _dbUsecaseHandler = null;
    // Wiki accessor
    protected SyncUsecaseHandler _syncUsecaseHandler = null;
    // ongoing status
    public String _currentPageName = "";
    public Boolean _initDone = false;
    protected ArrayList<String> _logs = new ArrayList<>();
    // UI access
    protected WebView _webView = null;
    protected EditText _editTextView = null;


    // initialisation

    public static WikiCacheUiOrchestrator instance(Context ictx, DbUsecaseHandler iDbUsecaseHandler, SyncUsecaseHandler iSyncUsecaseHandler){
        if(_instance == null){
            _instance = new WikiCacheUiOrchestrator(ictx);
        }
        // always keep current context up to date
        _instance.context = ictx;
        _instance._dbUsecaseHandler = iDbUsecaseHandler;
        _instance._dbUsecaseHandler.setWikiManagerCallback(_instance);
        _instance._syncUsecaseHandler = iSyncUsecaseHandler;
        _instance._syncUsecaseHandler.setWikiManagerCallback(_instance);
        if(!_instance._initDone) _instance.initFromDb();
        return _instance;
    }

    public static WikiCacheUiOrchestrator instance(Context ictx){
        if(_instance == null){
            _instance = new WikiCacheUiOrchestrator(ictx);
            Log.d(TAG, "instantiated without Async Handler, we will use default ones!");
        }
        // always keep current context up to date
        _instance.context = ictx;
        if(_instance._dbUsecaseHandler == null) {
            _instance._dbUsecaseHandler = new DbUsecaseHandler();
            _instance._dbUsecaseHandler.setWikiManagerCallback(_instance);
        }
        if(_instance._syncUsecaseHandler == null) {
            _instance._syncUsecaseHandler = new SyncUsecaseHandler();
            _instance._syncUsecaseHandler.setWikiManagerCallback(_instance);
        }
        if(!_instance._initDone) _instance.initFromDb();
        return _instance;
    }
    public static WikiCacheUiOrchestrator instance(){
        if(_instance == null){
            Log.e(TAG, "instantiated without context, this might fail !!!");
        }
        return _instance;
    }

    private WikiCacheUiOrchestrator(Context ictx) {
        context = ictx;
        _wikiPageList = new WikiPageList();
        _db = Room.databaseBuilder(ictx.getApplicationContext(),
                AppDatabase.class, "localcache")
                .addMigrations(AppDatabase.MIGRATION_1_2)
                .build();
    }

    void initFromDb(){
        _logs.add("Init applicative memory from local db");
        _dbUsecaseHandler.callPageReadAllUsecase(_db);
    }

    void updatePageListFromServer(){
        _logs.add("Retrieve the list of pages from server");
        _syncUsecaseHandler.callPageListRetrieveUsecase("", context);
        _logs.add("Retrieve the list of medias from server");
        retrieveMediaList("", false);
    }

    public void retrievePageHTMLforDisplay(String pagename, WebView webview){
        this._webView = webview;
        _currentPageName = pagename;
        String unencodedHtml = "<html><body>Please wait ...</body></html>";
        if(_wikiPageList._pages.containsKey(pagename) && _wikiPageList._pages.get(pagename)._html.compareTo("")!=0){
            _logs.add("using page "+pagename+" from local db" );
            Log.d(TAG, "Page is there, no need to download it !");
            unencodedHtml = _wikiPageList._pages.get(pagename)._html;
            Log.d(TAG, "html content:"+_wikiPageList._pages.get(pagename)._html);
            this.loadPage(unencodedHtml);
        }
        else {
            _logs.add("page "+pagename+" not in local db, get it from server" );
            Log.d(TAG, "Page not found here, need to download it !");
            unencodedHtml = "<html><body>Please wait ...</body></html>";

            //meantime, try to retrieve it
            _syncUsecaseHandler.callPageHtmlDownloadUsecase(pagename, context, true);

            this.loadPage(unencodedHtml);
        }
    }

    public void updatePageInCache(String pagename, String html) {
        html = html.replaceAll("href=\"/", "href=\"http://dokuwiki/");
        // save the page in db for caching
        _dbUsecaseHandler.callPageUpdateHtmlUsecase(_db, pagename, html);
        WikiPage newpage;
        if(_wikiPageList._pages.containsKey(pagename)){
            newpage = _wikiPageList._pages.get(pagename);
        }
        else{
            newpage = new WikiPage();
        }
        newpage._html = html;
        _wikiPageList._pages.put(pagename, newpage);
        _logs.add("page "+pagename+" stored in local db" );
    }

    public String retrievePageEdit(String pagename, EditText iEditTextView, Boolean directDisplay){
        _editTextView = iEditTextView;
        if(directDisplay) _currentPageName = pagename;
        String edit_text = "...";
        if(_wikiPageList._pages.containsKey(pagename) && _wikiPageList._pages.get(pagename)._text.compareTo("")!=0){
            _logs.add("using text page "+pagename+" from local db" );
            Log.d(TAG, "Page is there, no need to download it !");
            edit_text = _wikiPageList._pages.get(pagename)._text;
            Log.d(TAG, "edit_text content:"+edit_text);
        }
        else {
            _logs.add("text page "+pagename+" not in local db, get it from server" );
            Log.d(TAG, "Page not found here, need to download it !");
            edit_text = "...";
            //meantime, try to retrieve it
            _syncUsecaseHandler.callPageTextDownloadUsecase(pagename, context, directDisplay);
        }
        return edit_text;
    }

    public void retrieveMediaList(String namespace, Boolean directDisplay){
        _syncUsecaseHandler.callMediaListRetrieveUsecase(namespace, context, directDisplay);
    }

    public void savePageTextInCache(String pagename, String edit_text) {
        // save the page in db for caching
        _dbUsecaseHandler.callPageUpdateTextUsecase(_db, pagename, edit_text);

        WikiPage newpage;
        if(_wikiPageList._pages.containsKey(pagename)){
            newpage = _wikiPageList._pages.get(pagename);
        }
        else{
            newpage = new WikiPage();
        }
        newpage._text = edit_text;
        _wikiPageList._pages.put(pagename, newpage);
        _logs.add("page "+pagename+" stored in local db" );

    }

    public void savePageTextInCache(ArrayList<String> results, String pagename, Boolean directDisplay) {
        if(directDisplay) _currentPageName = pagename;
        String edit_text = "";
        if(results.size()==1){
            _logs.add("page "+pagename+" retrieved from server" );
            edit_text = results.get(0);
            savePageTextInCache(pagename, edit_text);
        }
        else
        {
            edit_text = "Error ...<";
            _logs.add("error when downloading page "+pagename+" from server");
        }
        if(directDisplay){
            if(_editTextView == null) {
                //try to find it from context
                _editTextView = (EditText) ((EditActivity)context).findViewById(R.id.edit_text);
            }
            if(_editTextView != null){
                _editTextView.setText(edit_text);
            }
        }
    }

    public void updateTextPage(String pagename, String newtext) {
        // 1. upload the new text to wiki
        _logs.add("text page "+pagename+" updated, uploading it to server");
        Log.d(TAG, "text page "+pagename+" updated, uploading it to server");
        _syncUsecaseHandler.callPageTextUploadUsecase(pagename, newtext, context);

        // 2. save also in local DB
        savePageTextInCache(pagename, newtext);
    }

    public void uploadedPageText(ArrayList<String> results, String pagename) {
        // try to display the new html version in _webview
        _logs.add("page "+pagename+" should be updated, get it from server");
        Log.d(TAG, "page "+pagename+" should be updated, get it from server");
        // try to retrieve it, in case of failure, consider to flag it for future retrieve + try a best effort display
        _syncUsecaseHandler.callPageHtmlDownloadUsecase(pagename, context, true);
    }

    public String getPageListHtml() {
        _logs.add("Show the list of pages from local db");
        String unencodedHtml = "<html><body><ul>";
        for (String a : _wikiPageList._pageversions.keySet()) {
            unencodedHtml += "\n<li><a href=\"http://dokuwiki/doku.php?id="+a+"\">" + a + "</a></li>";
        }
        unencodedHtml += "\n</ul></body></html>";
        Log.d("getPageListHtml", unencodedHtml);
        return unencodedHtml;
    }

    public String getLogsHtml() {
        String unencodedHtml = "<html><body><ul>";
        for (String a : _logs) {
            unencodedHtml += "\n<li>" + a + "</li>";
        }
        unencodedHtml += "\n</ul></body></html>";
        Log.d("getLogsHtml", unencodedHtml);
        return unencodedHtml;
    }

    // store in local cache the list of pages received from server
    public void updateCacheWithPageListReceived(ArrayList<String> results) {
        _logs.add("Received info for "+results.size()+" pages from server");
        for (int i = 0; i < results.size(); i++) {
            Log.d(TAG, "check: "+results.get(i));
            String pageinfo = results.get(i).replace("{","").replace("}","").replace(", ",",");
            String[] parts = pageinfo.split(",");
            String aPageName = "";
            String aRevision = "";
            for (String a : parts) {
                if(a.startsWith("id=")){
                    aPageName = a.substring(3);
                }
                else if(a.startsWith(" id=")){
                    aPageName = a.substring(4);
                }
                else if(a.startsWith("rev=")){
                    aRevision = a.substring(4);
                }
                else if(a.startsWith(" rev=")){
                    aRevision = a.substring(5);
                }
                else if(a.startsWith("{rev=")){
                    aRevision = a.substring(5);
                }
            }
            _wikiPageList._pageversions.put(aPageName, aRevision);
            Log.d("updatePageList", "add "+aPageName+" for rev "+aRevision);
            _wikiPageList._pagelist.add(results.get(i));
            Page page = new Page();
            page.pagename = aPageName;
            page.html = "";
            page.text = "";
            page.rev = aRevision;
            _dbUsecaseHandler.callPageAddIfMissingUsecase(_db, page);

            if(!_wikiPageList._pages.containsKey(aPageName))
            {
                WikiPage aPageItem = new WikiPage(page);
                _wikiPageList._pages.put(aPageName, aPageItem);
            }
            else if(_wikiPageList._pages.get(aPageName)._latest_version != aRevision)
            {
                WikiPage aPageItem = _wikiPageList._pages.remove(aPageName);
                aPageItem._latest_version = aRevision;
                _wikiPageList._pages.put(aPageName, aPageItem);
                // TODO: store an action to update the page
            }


        }
        identifyPagesToUpdate();
    }


    // store in local cache the list of medias received from server
    public void updateCacheWithMediaListReceived(ArrayList<String> results) {
        _logs.add("Received info for "+results.size()+" media from server");
        for (int i = 0; i < results.size(); i++) {
            Log.d(TAG, "check: "+results.get(i));
            String pageinfo = results.get(i).replace("{","").replace("}","").replace(", ",",");
            String[] parts = pageinfo.split(",");
            String aMediaFile = "";
            String aMediaId = "";
            String aMediaSize = "";
            String aMediaMTime = "";
            String aMediaLastModified = "";
            String aMediaIsImg = "";
            for (String a : parts) {
                if(a.startsWith("id=")){
                    aMediaId = a.substring(3);
                }
                else if(a.startsWith("file=")){
                    aMediaFile = a.substring(5);
                }
                else if(a.startsWith("size=")){
                    aMediaSize = a.substring(5);
                }
                else if(a.startsWith("isimg=")){
                    aMediaIsImg = a.substring(6);
                }
                else if(a.startsWith("mtime=")){
                    aMediaMTime = a.substring(6);
                }
                else if(a.startsWith("lastModified=")){
                    aMediaLastModified = a.substring(13);
                }
            }
            if(_wikiPageList._mediaversions.containsKey(aMediaId))
            {
                if(_wikiPageList._mediaversions.get(aMediaId).compareTo(aMediaMTime) != 0)
                {
                    Log.d(TAG, "Media "+aMediaId+" should be updated");
                    //TODO: log an action to update the media
                }
            }
            _wikiPageList._mediaversions.put(aMediaId, aMediaMTime);
            Log.d("updatePageList", "add "+aMediaId+" for rev "+aMediaMTime);
            _wikiPageList._pagelist.add(results.get(i));
            Media media = new Media();
            media.file = aMediaFile;
            media.id = aMediaId;
            media.size = aMediaSize;
            media.isimg = aMediaIsImg;
            media.mtime = aMediaMTime;
            media.lastModified = aMediaLastModified;
            _dbUsecaseHandler.callMediaAddOrUpdateUsecase(_db, media);
        }
    }

    public void ensureMediaIsDownloaded(String iMediaId, String iMediaLocalPath, Boolean iDirectDisplay) {
        File file = new File(context.getCacheDir(), iMediaLocalPath);
        if(!file.exists())
        {
            Log.d(TAG, "file "+iMediaId+" needs to be downloaded");
            _syncUsecaseHandler.callMediaDownloadUsecase(iMediaId, iMediaLocalPath, context, iDirectDisplay);
        }
        else {
            Log.d(TAG, "Media "+iMediaId+" is there, no need to download it !");
        }
    }

    public void createLocalFile(byte[] fcontent, String localPath){
        File file = new File(context.getCacheDir(), localPath);
        File parent = file.getParentFile();
        if (!parent.exists())
            parent.mkdirs();
        try {
            FileOutputStream fw = new FileOutputStream(file.getAbsoluteFile());
            fw.write(fcontent);
            fw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void identifyPagesToUpdate(){
        ArrayList<String> pagestoupdate = new ArrayList<>();
        for(String pagename : _wikiPageList._pages.keySet()){
            WikiPage pageitem = _wikiPageList._pages.get(pagename);
            if(pageitem._latest_version.compareTo(pageitem._version)!=0){
                Log.d(TAG, "I should update "+pagename+" from server "+pageitem._version+" -> "+pageitem._latest_version);
                pagestoupdate.add(pagename);
            }
        }
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this.context);
        String typesync = settings.getString("list_type_sync", "a");
        if(typesync.compareTo("b")==0) {
            _syncUsecaseHandler.callMultiPageHtmlDownloadUsecase(context, pagestoupdate.toArray(new String[pagestoupdate.size()]));

        }
        Log.d(TAG, "This is all I should update from server");
    }

    public void postInit() {
        _initDone = true;
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String startpage = settings.getString("startpage", "start");
        retrievePageHTMLforDisplay(startpage, _webView);
    }

    public void loadPage(String iPageData) {
        if(_webView == null) {
            // try to guess ourselves where is the webview
            _webView = (WebView) ((MainActivity)context).findViewById(R.id.webview);
        }
        if(_webView != null) {
            String html = iPageData
                    .replaceAll("href=\"/", "href=\"http://dokuwiki/")
                    .replaceAll("src=\"/lib/exe/fetch.php\\S*media=([^\\s^&]*)\"","src=\"$1\"");
            // find the list of media, to ensure they're here
            Map<String, String> aMediaList = new TreeMap<>();
            Pattern mediaPattern = Pattern.compile("src=\"/lib/exe/fetch.php\\S*media=([^\\s^&]*)\"");
            Matcher m = mediaPattern.matcher(iPageData);
            while (m.find()) {
                String s = m.group(1);
                // s now contains <namespace:file.ext>
                String p = s.replaceAll(":","/");
                aMediaList.put(s, p);
            }
            for (String mediaId:aMediaList.keySet()) {
                ensureMediaIsDownloaded(mediaId, aMediaList.get(mediaId), true);
                html = html.replaceAll("src=\""+mediaId+"\"", "src=\""+context.getCacheDir().getAbsolutePath()+"/"+aMediaList.get(mediaId)+"\"");
            }
            Log.d(TAG, "Display page: "+html);
            String aBaseUrl = "file://"+context.getCacheDir().getAbsolutePath();
            Log.d(TAG, "Base Url: "+aBaseUrl);
            _webView.loadDataWithBaseURL(aBaseUrl, html, "text/html", "UTF-8", null);
        }
    }

    public void refreshPage() {
        Log.d(TAG, "refreshing the page");
        _webView.reload();
    }
}
