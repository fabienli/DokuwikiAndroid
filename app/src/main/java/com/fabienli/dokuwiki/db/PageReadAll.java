package com.fabienli.dokuwiki.db;

import android.os.AsyncTask;
import android.util.Log;

import com.fabienli.dokuwiki.WikiCacheUiOrchestrator;
import com.fabienli.dokuwiki.cache.WikiPage;
import com.fabienli.dokuwiki.cache.WikiPageList;

import java.util.List;

public class PageReadAll extends AsyncTask<String, Void, String> {
    WikiCacheUiOrchestrator _wikiCacheUiOrchestrator = null;
    WikiPageList _wikiPageList = null;
    AppDatabase _db = null;
    String TAG = "PageReadAll";

    public PageReadAll(AppDatabase db, WikiCacheUiOrchestrator pagelist) {
        _db = db;
        _wikiCacheUiOrchestrator = pagelist;
        _wikiPageList = _wikiCacheUiOrchestrator._wikiPageList;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... params) {
        List<Page> plist = _db.pageDao().selectAll();
        for(Page p:plist)
        {
            Log.d(TAG, "found "+p.pagename);
            _wikiPageList._pageversions.put(p.pagename, p.rev);
            _wikiPageList._pages.put(p.pagename, new WikiPage(p));
        }

        return "done";
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        Log.d(TAG, "DB "+result);
        doPostAction();
    }

    public void doPostAction(){
        _wikiCacheUiOrchestrator.postInit();
    }
}