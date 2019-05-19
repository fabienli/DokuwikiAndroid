package com.fabienli.dokuwiki;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.fabienli.dokuwiki.db.DbCallbackInterface;
import com.fabienli.dokuwiki.db.DbUsecaseHandler;
import com.fabienli.dokuwiki.db.Media;
import com.fabienli.dokuwiki.db.Page;
import com.fabienli.dokuwiki.db.SyncAction;
import com.fabienli.dokuwiki.db.SyncActionListInterface;
import com.fabienli.dokuwiki.sync.SyncUsecaseCallbackInterface;
import com.fabienli.dokuwiki.sync.SyncUsecaseHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    public Boolean _initOngoing = false;
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
                .addMigrations(AppDatabase.MIGRATION_2_3)
                .addMigrations(AppDatabase.MIGRATION_3_4)
                .build();
    }

    void initFromDb(){
        if(!_initOngoing) {
            _initOngoing = true;
            _logs.add("Init applicative memory from local db");
            _dbUsecaseHandler.callPageReadAllUsecase(_db);
        }
    }

    void updatePageListFromServer(){
        _logs.add("Retrieve the list of pages from server");
        _syncUsecaseHandler.callPageListRetrieveUsecase("", context, new SyncUsecaseCallbackInterface() {
            @Override
            public void processResultsList(ArrayList<String> iXmlrpcResults) {
                updateCacheWithPageListReceived(iXmlrpcResults);

                _logs.add("Retrieve the list of medias from server");
                retrieveMediaList("");

                _logs.add("Retry the urgent items to be synced");
                SynchroHandler aSynchroHandler = new SynchroHandler(context, _dbUsecaseHandler, _syncUsecaseHandler);
                aSynchroHandler.syncPrioZero();
            }
            @Override
            public void processResultsBinary(byte[] iXmlrpcResults) {}
        });
    }

    public void forceDownloadPageHTMLforDisplay(WebView webview) {
        this._webView = webview;
        // in such case, the rev version in DB/cache is not guaranteed to be the last one
        _syncUsecaseHandler.callPageHtmlDownloadUsecase(_currentPageName, context, true, new SyncUsecaseCallbackInterface() {
            @Override
            public void processResultsList(ArrayList<String> iXmlrpcResults) {
                //TODO: add something here, unless the whole call here is removed in favor of doing it in SynchroHandler
            }
            @Override
            public void processResultsBinary(byte[] iXmlrpcResults) {}
        });
    }

    public void retrievePageHTMLforDisplay(String pagename, WebView webview){
        this._webView = webview;
        _currentPageName = pagename;
        String unencodedHtml = "<html><body>Please wait ...</body></html>";
        if(_wikiPageList._pages.containsKey(pagename) && _wikiPageList._pages.get(pagename)._html.compareTo("")!=0){
            _logs.add("using page "+pagename+" from local db" );
            Log.d(TAG, "Page is there, no need to download it !");
            unencodedHtml = _wikiPageList._pages.get(pagename)._html;
            Log.d(TAG, "html content: "+_wikiPageList._pages.get(pagename)._html);
            this.loadPage(unencodedHtml);
            // Check if a more recent version exists:
            if(_wikiPageList._pages.get(pagename)._version.compareTo(_wikiPageList._pages.get(pagename)._latest_version) != 0){
                _logs.add("page "+pagename+" needs an update, doing it now" );
                _syncUsecaseHandler.callPageHtmlDownloadUsecase(pagename, context, true, new SyncUsecaseCallbackInterface() {
                    @Override
                    public void processResultsList(ArrayList<String> iXmlrpcResults) {
                        //TODO: add something here, unless the whole call here is removed in favor of doing it in SynchroHandler
                    }
                    @Override
                    public void processResultsBinary(byte[] iXmlrpcResults) {}
                });
            }
        }
        else {
            _logs.add("page "+pagename+" not in local db, get it from server" );
            Log.d(TAG, "Page not found here, need to download it !");
            unencodedHtml = "<html><body>Please wait ...</body></html>";

            //meantime, try to retrieve it
            _syncUsecaseHandler.callPageHtmlDownloadUsecase(pagename, context, true, new SyncUsecaseCallbackInterface() {
                @Override
                public void processResultsList(ArrayList<String> iXmlrpcResults) {
                    //TODO: add something here, unless the whole call here is removed in favor of doing it in SynchroHandler
                }
                @Override
                public void processResultsBinary(byte[] iXmlrpcResults) {}
            });

            this.loadPage(unencodedHtml);
        }
    }

    public void updatePageInCache(String pagename, String html) {
        html = html.replaceAll("href=\"/", "href=\"http://dokuwiki/");
        WikiPage newpage;
        if(_wikiPageList._pages.containsKey(pagename)){
            newpage = _wikiPageList._pages.get(pagename);
            if(newpage._latest_version.length()>0)
                newpage._version = newpage._latest_version;
        }
        else{
            newpage = new WikiPage();
        }
        newpage._html = html;
        // save the page in db for caching
        _dbUsecaseHandler.callPageUpdateHtmlUsecase(_db, pagename, html, newpage._version);
        _wikiPageList._pages.put(pagename, newpage);
        _logs.add("page "+pagename+" stored in local db" );
    }

    public String retrievePageEdit(final String pagename, EditText iEditTextView, final Boolean directDisplay, Boolean forceDownload){
        _editTextView = iEditTextView;
        if(directDisplay) _currentPageName = pagename;
        String edit_text = "...";
        if(_wikiPageList._pages.containsKey(pagename) && _wikiPageList._pages.get(pagename)._text.compareTo("")!=0 && !forceDownload){
            _logs.add("using text page "+pagename+" from local db" );
            Log.d(TAG, "Page is there, no need to download it !");
            edit_text = _wikiPageList._pages.get(pagename)._text;
            Log.d(TAG, "edit_text content:"+edit_text);
            if(directDisplay) {
                if(_editTextView == null) {
                    //try to find it from context
                    _editTextView = (EditText) ((EditActivity)context).findViewById(R.id.edit_text);
                }
                if(_editTextView != null){
                    _editTextView.setText(edit_text);
                }
            }
        }
        else {
            _logs.add("text page "+pagename+" not in local db, get it from server" );
            Log.d(TAG, "Page not found here, need to download it !");
            edit_text = "...";
            //meantime, try to retrieve it
            _syncUsecaseHandler.callPageTextDownloadUsecase(pagename, context, directDisplay, new SyncUsecaseCallbackInterface() {
                @Override
                public void processResultsList(ArrayList<String> iXmlrpcResults) {
                    savePageTextInCache(iXmlrpcResults, pagename, directDisplay);
                }
                @Override
                public void processResultsBinary(byte[] iXmlrpcResults) {}
            });
        }
        return edit_text;
    }

    public void retrieveMediaList(String namespace){
        _syncUsecaseHandler.callMediaListRetrieveUsecase(namespace, context);
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

    public void updateTextPage(final String pagename, String newtext) {
        // 1. upload the new text to wiki
        _logs.add("text page "+pagename+" updated, uploading it to server");
        Log.d(TAG, "text page "+pagename+" updated, uploading it to server");

        SyncAction syncAction = new SyncAction();
        syncAction.priority = "0";
        syncAction.verb = "PUT";
        syncAction.name = pagename;
        syncAction.rev = _wikiPageList._pages.get(pagename)._version;
        syncAction.data = newtext;
        _dbUsecaseHandler.callSyncActionInsertUsecase(_db, syncAction, new DbCallbackInterface() {
            @Override
            public void onceDone() {
                SynchroHandler aSynchroHandler = new SynchroHandler(context, _dbUsecaseHandler, _syncUsecaseHandler);
                aSynchroHandler.syncPrioZero();
            }
        });

        // 2. save also in local DB
        savePageTextInCache(pagename, newtext);
    }

    public String getPageListHtml() {
        boolean debug = false;
        _logs.add("Show the list of pages from local db");
        String unencodedHtml = "<html><body><ul>";
        for (String a : _wikiPageList._pages.keySet()) {
            if (debug) {

                unencodedHtml += "\n<li><a href=\"http://dokuwiki/doku.php?id=" + a + "\">" + a + "</a> " +
                        _wikiPageList._pages.get(a)._version + " - " +
                        _wikiPageList._pages.get(a)._latest_version + " - " +
                        _wikiPageList._pages.get(a)._html.length() + "</li>";
            } else {
                unencodedHtml += "\n<li><a href=\"http://dokuwiki/doku.php?id=" + a + "\">" + a + "</a></li>";
            }
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
    public void updateCacheWithPageListReceived(final ArrayList<String> results) {
        _logs.add("Received info for "+results.size()+" pages from server");
        _dbUsecaseHandler.callSyncActionDeleteLevelUsecase(_db, "2", new DbCallbackInterface() {
            @Override
            public void onceDone() {
                HashSet<String> oldPagesToRemove = new HashSet<>();
                oldPagesToRemove.addAll (_wikiPageList._pages.keySet());
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
                        else if(a.startsWith("rev=")){
                            aRevision = a.substring(4);
                        }
                    }
                    oldPagesToRemove.remove(aPageName);
                    Log.d("updatePageList", "add "+aPageName+" for rev "+aRevision);
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
                    else if(_wikiPageList._pages.get(aPageName)._latest_version.compareTo(aRevision) != 0
                            || _wikiPageList._pages.get(aPageName)._html.length() == 0)
                    {
                        WikiPage aPageItem = _wikiPageList._pages.remove(aPageName);
                        aPageItem._latest_version = aRevision;
                        _wikiPageList._pages.put(aPageName, aPageItem);
                        SyncAction syncAction = new SyncAction();
                        syncAction.priority = "2";
                        syncAction.verb = "GET";
                        syncAction.name = aPageName;
                        syncAction.rev = aRevision;
                        syncAction.data = "";
                        _dbUsecaseHandler.callSyncActionInsertUsecase(_db, syncAction, new DbCallbackInterface() {
                            @Override
                            public void onceDone() {
                            }
                        });
                        // html page is not up-to-date, so text content to edit is deprecated as well
                        if(_wikiPageList._pages.get(aPageName)._text.length()>0){
                            savePageTextInCache(aPageName, "");
                        }
                    }
                }
                //identifyPagesToUpdate(); // used for immediate sync

                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                String typesync = settings.getString("list_type_sync", "a");
                if(typesync.compareTo("b")==0) {
                    SynchroHandler aSynchroHandler = new SynchroHandler(context, _dbUsecaseHandler, _syncUsecaseHandler);
                    aSynchroHandler.syncPrioN(2);
                }

                for(final String oldPage : oldPagesToRemove){
                    Log.d(TAG, "Remove this old page from local DB: "+oldPage);
                    _dbUsecaseHandler.callPageRemoveUsecase(_db, oldPage, new DbCallbackInterface(){
                        @Override
                        public void onceDone() {
                            _logs.add("Removed the old page from local DB: "+oldPage);
                            _wikiPageList._pages.remove(oldPage);
                        }
                    });
                }

            }
        });
    }


    // store in local cache the list of medias received from server
    public void updateCacheWithMediaListReceived(ArrayList<String> results) {
        _logs.add("Received info for "+results.size()+" media from server");
        for (int i = 0; i < results.size(); i++) {
            //Log.d(TAG, "check: "+results.get(i));
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
                    //log an action to update the media
                    SyncAction syncAction = new SyncAction();
                    syncAction.priority = "3";
                    syncAction.verb = "GET";
                    syncAction.name = aMediaId;
                    syncAction.rev = aMediaMTime;
                    syncAction.data = "";
                    _dbUsecaseHandler.callSyncActionInsertUsecase(_db, syncAction, new DbCallbackInterface() {
                        @Override
                        public void onceDone() {
                        }
                    });
                }
            }
            else{
                // log an action do download the media
                SyncAction syncAction = new SyncAction();
                syncAction.priority = "3";
                syncAction.verb = "GET";
                syncAction.name = aMediaId;
                syncAction.rev = aMediaMTime;
                syncAction.data = "";
                _dbUsecaseHandler.callSyncActionInsertUsecase(_db, syncAction, new DbCallbackInterface() {
                    @Override
                    public void onceDone() {
                    }
                });
            }
            _wikiPageList._mediaversions.put(aMediaId, aMediaMTime);
            //Log.d("updatePageList", "add "+aMediaId+" for rev "+aMediaMTime);
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

    public void ensureMediaIsDownloaded(String iMediaId, String iMediaLocalPath, int width, int height, Boolean iDirectDisplay) {
        String newlocalFilename = getLocalFileName(iMediaLocalPath, width, height);
        File originalfile = new File(context.getCacheDir(), iMediaLocalPath);
        File file = new File(context.getCacheDir(), newlocalFilename);
        if(!originalfile.exists())
        {
            Log.d(TAG, "file "+iMediaId+" needs to be downloaded");
            _syncUsecaseHandler.callMediaDownloadUsecase(iMediaId, iMediaLocalPath, context, width, height, iDirectDisplay);
        }
        else if(!file.exists())
        {
            Log.d(TAG, "file "+iMediaId+" is there but local size needs to be adapted");
            createLocalFileResized(iMediaLocalPath, width, height);
        }
        else {
            Log.d(TAG, "Media "+iMediaId+" is there, no need to download it !");
        }
    }

    public void createLocalFile(byte[] fcontent, String localPath){
        if(fcontent == null)
            return;
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

    public void createLocalFileResized(String localPath, int targetW, int targetH){
        String newlocalFilename = getLocalFileName(localPath, targetW, targetH);
        Log.d(TAG, "Resizing "+localPath+" to "+newlocalFilename);

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(context.getCacheDir()+"/"+localPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = 1;
        if ((targetW > 0) && (targetH > 0)) {
            scaleFactor = Math.min(photoW/targetW, photoH/targetH);
        }
        else if(targetW > 0) {
            scaleFactor = photoW/targetW;
        }
        else if(targetH > 0) {
            scaleFactor = photoH/targetH;
        }

        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true; //Deprecated API 21

        Bitmap newBitmap = BitmapFactory.decodeFile(context.getCacheDir()+"/"+localPath, bmOptions);
        if(newBitmap == null) // invalid source file
        {
            Log.e(TAG, "Invalid file ! " + localPath);
            File invalidFile = new File(context.getCacheDir(), localPath);
            invalidFile.delete();
            return;
        }

        if (targetW>0 && targetH>0) {
            Log.d(TAG, "targetW:"+targetW+" targetH:"+targetH+" to w:"+newBitmap.getWidth()+ " h:"+newBitmap.getHeight());
            // should be cropped to ensure width and height
            int startX = (newBitmap.getWidth() - targetW) / 2;
            int startY = (newBitmap.getHeight() - targetH) / 2;
            newBitmap = Bitmap.createBitmap(newBitmap, startX, startY , targetW , targetH);
        }
        try {
            File file = new File(context.getCacheDir(), newlocalFilename);
            file.createNewFile();
            FileOutputStream ostream = new FileOutputStream(file);
            newBitmap.compress(Bitmap.CompressFormat.PNG, 100, ostream);
            ostream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String getLocalFileName(String localPath, int width, int height){
        if(width == 0 && height == 0)
            return localPath;
        else if(width == 0)
            return localPath + "__" +Integer.toString(height);
        else if(height == 0)
            return localPath + "_" +Integer.toString(width)+"_";
        return localPath + "_" + Integer.toString(width)+"_" + Integer.toString(height);
    }

    //TODO: deprecated ?
    public void identifyPagesToUpdate(){
        ArrayList<String> pagestoupdate = new ArrayList<>();
        for(String pagename : _wikiPageList._pages.keySet()){
            WikiPage pageitem = _wikiPageList._pages.get(pagename);
            if(pageitem._latest_version.compareTo(pageitem._version)!=0){
                Log.d(TAG, "I should update "+pagename+" from server "+pageitem._version+" -> "+pageitem._latest_version);
                pagestoupdate.add(pagename);
                //TODO: this first loop is useless if well using sync level 2
            }
        }
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this.context);
        String typesync = settings.getString("list_type_sync", "a");
        if(typesync.compareTo("b")==0) {
            _syncUsecaseHandler.callMultiPageHtmlDownloadUsecase(context, pagestoupdate.toArray(new String[pagestoupdate.size()]));
            //TODO: replace this with a call to sync level 2

        }
        Log.d(TAG, "This is all I should update from server");
    }

    public void postInit() {
        _initDone = true;
        _initOngoing = false;
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
                    .replaceAll("href=\"/", "href=\"http://dokuwiki/");
                    //.replaceAll("src=\"/lib/exe/fetch.php\\S*media=([^\\s^&]*)\"","src=\"$1\"");
                    //.replaceAll("src=\"/lib/exe/fetch.php\\S*media=([^\\s^&]*)\"","src=\"$1\"");
            // find the list of media, to ensure they're here
            Map<String, String> aMediaList = new TreeMap<>();
            Pattern mediaPattern = Pattern.compile("src=\"/lib/exe/fetch.php\\?(\\S+)\"");
            Matcher m = mediaPattern.matcher(iPageData);
            while (m.find()) {
                int width = 0;
                int height = 0;
                String id="";
                String[] args = m.group(1).split("&amp;");
                for (String v:args) {
                    String[] opt = v.split("=");
                    if(opt.length == 2){
                        if(opt[0].compareTo("w") == 0)
                            width = Integer.parseInt(opt[1]);
                        else if(opt[0].compareTo("h") == 0)
                            height = Integer.parseInt(opt[1]);
                        else if(opt[0].compareTo("media") == 0)
                            id = opt[1];
                    }
                }
                Log.d(TAG, "Found image: "+id+ " width="+width+" height="+height);
                // id now contains <namespace:file.ext>
                String imageFilePath = id.replaceAll(":","/");
                ensureMediaIsDownloaded(id, imageFilePath, width, height,true);
                String localFilename = getLocalFileName(imageFilePath, width, height);
                html = html.replaceAll("src=\"/lib/exe/fetch.php\\?"+m.group(1)+"\"", "src=\""+context.getCacheDir().getAbsolutePath()+"/"+localFilename+"\"");
                //aMediaList.put(id, imageFilePath);
            }
            for (String mediaId:aMediaList.keySet()) {
                ensureMediaIsDownloaded(mediaId, aMediaList.get(mediaId), 0, 0,true);
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

    public void displayActionListPage(WebView _webView) {
        _logs.add("Show the list of SyncAction from local db");

        _dbUsecaseHandler.callSyncActionRetrieveUsecase(_db, new SyncActionListInterface() {
            @Override
            public void handle(List<SyncAction> syncActions) {
                String itemsList = "<ul>";
                int[] counter = {0,0,0,0};
                int priority = 0;
                for (SyncAction sa : syncActions) {
                    itemsList += "\n<li>" + sa.toText() + "</li>";
                    priority = Integer.parseInt(sa.priority);
                    counter[priority]++;
                }
                itemsList += "\n</ul>";
                String unencodedHtml = "<html><body>\n<table>"+
                        "\n<tr><td>Prio 0: </td><td>"+counter[0]+
                        "\n<tr><td>Prio 1: </td><td>"+counter[1]+
                        "\n<tr><td>Prio 2: </td><td>"+counter[2]+
                        "\n<tr><td>Prio 3: </td><td>"+counter[3]+"</td></tr>\n</table>\n"+itemsList+"\n</body></html>";
                loadPage(unencodedHtml);
            }
        });
    }
}
