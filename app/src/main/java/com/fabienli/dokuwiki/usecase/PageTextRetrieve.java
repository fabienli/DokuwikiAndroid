package com.fabienli.dokuwiki.usecase;

import android.os.AsyncTask;

import com.fabienli.dokuwiki.WikiCacheUiOrchestrator;
import com.fabienli.dokuwiki.db.AppDatabase;
import com.fabienli.dokuwiki.db.Page;
import com.fabienli.dokuwiki.db.PageUpdateText;
import com.fabienli.dokuwiki.db.SyncAction;
import com.fabienli.dokuwiki.sync.PageTextDownUpLoader;
import com.fabienli.dokuwiki.sync.XmlRpcAdapter;
import com.fabienli.dokuwiki.tools.Logs;
import com.fabienli.dokuwiki.usecase.callback.PageHtmlRetrieveCallback;

import java.util.List;

public class PageTextRetrieve extends PoolAsyncTask {
    AppDatabase _db;
    PageHtmlRetrieveCallback _pageHtmlRetrieveCallback = null;
    String _pageContent = "";
    XmlRpcAdapter _xmlRpcAdapter;

    public PageTextRetrieve(AppDatabase db, XmlRpcAdapter xmlRpcAdapter) {
        _db = db;
        _xmlRpcAdapter = xmlRpcAdapter;
    }

    public String retrievePage(String pagename) {
        String pageContent = "";
        SyncAction syncActionRelated = null;
        // check it in DB
        Page dbPage = _db.pageDao().findByName(pagename);
        if(dbPage != null) {
            Logs.getInstance().add("page "+pagename+" text loaded from local db" );
            if(dbPage.text != null) pageContent = dbPage.text;
            // Check if a more recent version exists:
            List<SyncAction> synActionItems = _db.syncActionDao().getAll();
            for (SyncAction sa : synActionItems ){
                if(sa.name.compareTo(pagename) == 0 && sa.verb.compareTo("GET") == 0)
                {
                    syncActionRelated = sa;
                }
            }
        }
        // get it from server if not there
        if(pageContent.length() == 0 || syncActionRelated != null)
        {
            Logs.getInstance().add("page "+pagename+" not in local db, get it from server" );
            PageTextDownUpLoader pageTextDownUpLoader = new PageTextDownUpLoader(_xmlRpcAdapter);
            pageContent = pageTextDownUpLoader.retrievePageText(pagename);

            // store it in DB cache
            PageUpdateText pageUpdateText = new PageUpdateText(_db, pagename, pageContent);
            pageUpdateText.doSync();
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
