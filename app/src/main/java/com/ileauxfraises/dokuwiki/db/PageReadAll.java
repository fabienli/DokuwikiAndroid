package com.ileauxfraises.dokuwiki.db;

import android.os.AsyncTask;
import android.util.Log;

import com.ileauxfraises.dokuwiki.WikiManager;
import com.ileauxfraises.dokuwiki.cache.WikiPage;
import com.ileauxfraises.dokuwiki.cache.WikiPageList;

import java.util.List;

public class PageReadAll extends AsyncTask<String, Void, String> {
    WikiManager _wikiManager = null;
    WikiPageList _wikiPageList = null;
    AppDatabase _db = null;
    String TAG = "PageReadAll";

    public PageReadAll(AppDatabase db, WikiManager pagelist) {
        _db = db;
        _wikiManager = pagelist;
        _wikiPageList = _wikiManager._wikiPageList;
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
        _wikiManager.allPagesRetrieved();
    }
}