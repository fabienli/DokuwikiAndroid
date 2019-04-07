package com.ileauxfraises.dokuwiki;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.webkit.WebView;
import android.widget.EditText;

import com.ileauxfraises.dokuwiki.cache.WikiPage;
import com.ileauxfraises.dokuwiki.cache.WikiPageList;
import com.ileauxfraises.dokuwiki.db.AppDatabase;
import com.ileauxfraises.dokuwiki.db.Page;
import com.ileauxfraises.dokuwiki.db.PageAddIfMissing;
import com.ileauxfraises.dokuwiki.db.PageReadAll;
import com.ileauxfraises.dokuwiki.db.PageUpdateHtml;
import com.ileauxfraises.dokuwiki.db.PageUpdateText;
import com.ileauxfraises.dokuwiki.sync.MultiPageHtmlDownloader;
import com.ileauxfraises.dokuwiki.sync.PageHtmlDownloader;
import com.ileauxfraises.dokuwiki.sync.PageListRetriever;
import com.ileauxfraises.dokuwiki.sync.PageTextDownloader;
import com.ileauxfraises.dokuwiki.sync.PageTextUploader;
import com.ileauxfraises.dokuwiki.sync.XmlRpcDownload;

import java.util.ArrayList;

import androidx.room.Room;

public class WikiManager {
    static WikiManager _instance = null;
    static String TAG = "WikiManager";
    Context context;
    public WikiPageList _wikiPageList;
    AppDatabase _db;
    ArrayList<String> _logs = new ArrayList<>();
    public String _currentPageName = "";
    public Boolean _initDone = false;

    public static WikiManager instance(Context ictx){
        if(_instance == null){
            _instance = new WikiManager(ictx);
        }
        // always keep current context up to date
        _instance.context = ictx;
        return _instance;
    }
    public static WikiManager instance(){
        if(_instance == null){
            Log.e(TAG, "instantiated without context, this might fail !!!");
        }
        return _instance;
    }

    private WikiManager(Context ictx) {
        context = ictx;
        _wikiPageList = new WikiPageList();
        _db = Room.databaseBuilder(ictx.getApplicationContext(),
                AppDatabase.class, "localcache").build();
        initFromDb();

        //updatePageListFromServer();
    }

    void initFromDb(){
        _logs.add("Init applicative memory from local db");
        PageReadAll aExecutor = new PageReadAll(_db, this);
        aExecutor.execute();
    }

    void updatePageListFromServer(){
        _logs.add("Retrieve the list of pages from server");
        XmlRpcDownload aExecutor = new PageListRetriever(context, this);
        aExecutor.retrievePageList("");
    }

    public String retrievePageHTML(String pagename, Boolean directDisplay){
        if(directDisplay) _currentPageName = pagename;
        String unencodedHtml = "<html><body>Please wait ...</body></html>";
        if(_wikiPageList._pages.containsKey(pagename) && _wikiPageList._pages.get(pagename)._html.compareTo("")!=0){
            _logs.add("using page "+pagename+" from local db" );
            Log.d(TAG, "Page is there, no need to download it !");
            unencodedHtml = _wikiPageList._pages.get(pagename)._html;
            Log.d(TAG, "html content:"+_wikiPageList._pages.get(pagename)._html);
        }
        else {
            _logs.add("page "+pagename+" not in local db, get it from server" );
            Log.d(TAG, "Page not found here, need to download it !");
            unencodedHtml = "<html><body>Please wait ...</body></html>";
            //meantime, try to retrieve it
            XmlRpcDownload aExecutor = new PageHtmlDownloader(context, this, directDisplay);
            aExecutor.retrievePageHTML(pagename);
        }
        return unencodedHtml;
    }

    public void retrievedPageHtml(String pagename, String html) {
        html = html.replaceAll("href=\"/", "href=\"http://dokuwiki/");
        // save the page in db for caching
        PageUpdateHtml dbExec = new PageUpdateHtml(_db, pagename, html);
        dbExec.execute();
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

    public void retrievedPageHtml(ArrayList<String> results, String pagename, Boolean directDisplay) {
        if(directDisplay) _currentPageName = pagename;
        String html = "";
        if(results.size()==1){
            _logs.add("page "+pagename+" retrieved from server" );
            html = results.get(0);
            retrievedPageHtml(pagename, html);
        }
        else
        {
            html = "<html><body>Error ...</body></html>";
            _logs.add("error when downloading page "+pagename+" from server");
        }
        if(directDisplay){
            html = html.replaceAll("href=\"/", "href=\"http://dokuwiki/");
            String encodedHtml = Base64.encodeToString(html.getBytes(), Base64.NO_PADDING);
            WebView myWebView = (WebView) ((MainActivity)context).findViewById(R.id.webview);
            myWebView.loadData(encodedHtml, "text/html", "base64");
        }
    }

    public String retrievePageEdit(String pagename, Boolean directDisplay){
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
            PageTextDownloader aExecutor = new PageTextDownloader(context, this, directDisplay);
            aExecutor.retrievePageText(pagename);
        }
        return edit_text;
    }

    public void retrievedPageText(String pagename, String edit_text) {
        // save the page in db for caching
        PageUpdateText dbExec = new PageUpdateText(_db, pagename, edit_text);
        dbExec.execute();
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

    public void retrievedPageText(ArrayList<String> results, String pagename, Boolean directDisplay) {
        if(directDisplay) _currentPageName = pagename;
        String edit_text = "";
        if(results.size()==1){
            _logs.add("page "+pagename+" retrieved from server" );
            edit_text = results.get(0);
            retrievedPageText(pagename, edit_text);
        }
        else
        {
            edit_text = "Error ...<";
            _logs.add("error when downloading page "+pagename+" from server");
        }
        if(directDisplay){
            EditText myEditText = (EditText) ((EditActivity)context).findViewById(R.id.edit_text);
            myEditText.setText(edit_text);
        }
    }

    public void updateTextPage(String pagename, String newtext) {
        // 1. upload the new text to wiki
        _logs.add("text page "+pagename+" updated, uploading it to server");
        Log.d(TAG, "text page "+pagename+" updated, uploading it to server");
        PageTextUploader aExecutor = new PageTextUploader(context, this);
        aExecutor.uploadPageText(pagename, newtext);
        // 2. save also in local DB
        retrievedPageText(pagename, newtext);
    }

    public void uploadedPageText(ArrayList<String> results, String pagename) {
        // try to display the new html version in MainActivity?
        _logs.add("page "+pagename+" should be updated, get it from server");
        Log.d(TAG, "page "+pagename+" should be updated, get it from server");
        //meantime, try to retrieve it
        XmlRpcDownload aExecutor = new PageHtmlDownloader(context, this, true);
        aExecutor.retrievePageHTML(pagename);

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

    public void retrievedPageList(ArrayList<String> results) {
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
            PageAddIfMissing dbExec = new PageAddIfMissing(_db, page);
            dbExec.execute();
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
            }


        }
        listPagesToUpdate();
    }

    public void listPagesToUpdate(){
        ArrayList<String> pagestoupdate = new ArrayList<>();
        for(String pagename : _wikiPageList._pages.keySet()){
            WikiPage pageitem = _wikiPageList._pages.get(pagename);
            if(pageitem._latest_version.compareTo(pageitem._version)!=0){
                Log.d(TAG, "I should update "+pagename+" from server "+pageitem._version+" -> "+pageitem._latest_version);
                pagestoupdate.add(pagename);
            }
        }
        if(false) {
            MultiPageHtmlDownloader aExecutor = new MultiPageHtmlDownloader(context, this);
            aExecutor.execute(pagestoupdate.toArray(new String[pagestoupdate.size()]));
        }
        Log.d(TAG, "This is all I should update from server");

    }

    public void allPagesRetrieved() {
        _initDone = true;
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String startpage = settings.getString("startpage", "start");
        if(_wikiPageList._pages.containsKey(startpage) && _wikiPageList._pages.get(startpage)._html.compareTo("")!=0) {
            String html = retrievePageHTML(startpage, true);
            html = html.replaceAll("href=\"/", "href=\"http://dokuwiki/");
            String encodedHtml = Base64.encodeToString(html.getBytes(), Base64.NO_PADDING);
            WebView myWebView = (WebView) ((MainActivity)context).findViewById(R.id.webview);
            myWebView.loadData(encodedHtml, "text/html", "base64");
        }
    }
}
