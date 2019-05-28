package com.fabienli.dokuwiki.usecase;

import android.os.AsyncTask;

import com.fabienli.dokuwiki.WikiCacheUiOrchestrator;
import com.fabienli.dokuwiki.db.AppDatabase;
import com.fabienli.dokuwiki.db.Page;
import com.fabienli.dokuwiki.db.PageUpdateHtml;
import com.fabienli.dokuwiki.db.SyncAction;
import com.fabienli.dokuwiki.sync.PageHtmlDownloader;
import com.fabienli.dokuwiki.sync.PageInfoRetriever;
import com.fabienli.dokuwiki.sync.XmlRpcAdapter;
import com.fabienli.dokuwiki.usecase.callback.PageHtmlRetrieveCallback;

import java.util.List;

public class PageHtmlRetrieve extends AsyncTask<String, Integer, String> {
    AppDatabase _db = null;
    PageHtmlRetrieveCallback _pageHtmlRetrieveCallback = null;
    String _pageContent = "";
    XmlRpcAdapter _xmlRpcAdapter;

    public PageHtmlRetrieve(AppDatabase db, XmlRpcAdapter xmlRpcAdapter) {
        _db = db;
        _xmlRpcAdapter = xmlRpcAdapter;
    }

    public String retrievePage(String pagename) {
        String pageContent = "";
        String pageVersion = "";
        SyncAction syncActionRelated = null;
        // check it in DB
        Page dbPage = _db.pageDao().findByName(pagename);
        if(dbPage != null) {
            WikiCacheUiOrchestrator.instance()._logs.add("page "+pagename+" loaded from local db" );
            if(!dbPage.isHtmlEmpty()) pageContent = dbPage.html;
            pageVersion = dbPage.rev;
            // Check if a more recent version exists:
            List<SyncAction> synActionItems = _db.syncActionDao().getAll();
            for (SyncAction sa : synActionItems ){
                if(sa.name.compareTo(pagename) == 0 && sa.verb.compareTo("GET") == 0)
                {
                    syncActionRelated = sa;
                    pageVersion = sa.rev;
                }
            }
        }
        // get it from server if not there
        if(pageContent.length() == 0 || syncActionRelated != null)
        {
            WikiCacheUiOrchestrator.instance()._logs.add("page "+pagename+" not in local db, get it from server" );
            PageHtmlDownloader pageHtmlDownloader = new PageHtmlDownloader(_xmlRpcAdapter);
            pageContent = pageHtmlDownloader.retrievePageHTML(pagename);

            // get the version if we need
            if(pageVersion == null || pageVersion.length() == 0) {
                // need to update from server
                PageInfoRetriever pageInfoRetriever = new PageInfoRetriever(_xmlRpcAdapter);
                pageVersion = pageInfoRetriever.retrievePageInfo(pagename);
            }
            // store it in DB cache
            PageUpdateHtml pageUpdateHtml = new PageUpdateHtml(_db, pagename, pageContent, pageVersion);
            pageUpdateHtml.doSync();

            if(syncActionRelated != null){
                _db.syncActionDao().deleteAll(syncActionRelated);
            }
        }
        return pageContent;
    }

    public void retrievePageAsync(String pagename, PageHtmlRetrieveCallback pageHtmlRetrieveCallback) {
        _pageHtmlRetrieveCallback = pageHtmlRetrieveCallback;
        execute(pagename);
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
