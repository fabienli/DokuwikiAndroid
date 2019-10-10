package com.fabienli.dokuwiki.usecase;

import android.os.AsyncTask;

import com.fabienli.dokuwiki.WikiCacheUiOrchestrator;
import com.fabienli.dokuwiki.db.AppDatabase;
import com.fabienli.dokuwiki.db.PageUpdateHtml;
import com.fabienli.dokuwiki.db.SyncAction;
import com.fabienli.dokuwiki.sync.PageHtmlDownloader;
import com.fabienli.dokuwiki.sync.PageInfoRetriever;
import com.fabienli.dokuwiki.sync.XmlRpcAdapter;
import com.fabienli.dokuwiki.tools.Logs;
import com.fabienli.dokuwiki.usecase.callback.PageHtmlRetrieveCallback;

import java.util.List;

public class PageHtmlRetrieveForceDownload extends AsyncTask<String, Integer, String> {
    AppDatabase _db = null;
    PageHtmlRetrieveCallback _pageHtmlRetrieveCallback = null;
    String _pageContent = "";
    XmlRpcAdapter _xmlRpcAdapter;

    public PageHtmlRetrieveForceDownload(AppDatabase db, XmlRpcAdapter xmlRpcAdapter) {
        _db = db;
        _xmlRpcAdapter = xmlRpcAdapter;
    }

    public String retrievePage(String pagename) {
        String pageContent = "";
        String pageVersion = "";

        // get page from server as forced to
        Logs.getInstance().add("page "+pagename+" forced to get it from server" );
        PageHtmlDownloader pageHtmlDownloader = new PageHtmlDownloader(_xmlRpcAdapter);
        pageContent = pageHtmlDownloader.retrievePageHTML(pagename);

        // need to get the version from server
        PageInfoRetriever pageInfoRetriever = new PageInfoRetriever(_xmlRpcAdapter);
        pageVersion = pageInfoRetriever.retrievePageVersion(pagename);

        // store it in DB cache
        PageUpdateHtml pageUpdateHtml = new PageUpdateHtml(_db, pagename, pageContent, pageVersion);
        pageUpdateHtml.doSync();

        // remove from SyncAction if it was there
        List<SyncAction> synActionItems = _db.syncActionDao().getAll();
        for (SyncAction sa : synActionItems ){
            if(sa.name.compareTo(pagename) == 0 && sa.verb.compareTo("GET") == 0)
            {
                _db.syncActionDao().deleteAll(sa);
            }
        }

        return pageContent;
    }

    public void retrievePageAsync(String pagename, PageHtmlRetrieveCallback pageHtmlRetrieveCallback) {
        _pageHtmlRetrieveCallback = pageHtmlRetrieveCallback;
        executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, pagename);
    }

    @Override
    protected String doInBackground(String... pagename) {
        if(pagename.length == 1){
            _pageContent = retrievePage(pagename[0]);
        }
        return "ok";
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if(_pageHtmlRetrieveCallback!=null)
            _pageHtmlRetrieveCallback.pageRetrieved(_pageContent);
    }
}
