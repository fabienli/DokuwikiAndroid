package com.fabienli.dokuwiki.db;

import android.os.AsyncTask;
import android.util.Log;

public class PageUpdateHtml extends AsyncTask<String, Void, String> {
    AppDatabase _db = null;
    String TAG = "PageUpdateHtml";
    String _pagename;
    String _html;
    String _version;

    public PageUpdateHtml(AppDatabase db, String pagename, String html, String version) {
        _db = db;
        _pagename = pagename;
        _html = html;
        _version = version;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    public String doSync()
    {
        Page existing_item = _db.pageDao().findByName(_pagename);

        if(existing_item == null)
        {
            Log.d(TAG, "no existing item found ");
            Page page = new Page();
            page.pagename = _pagename;
            page.html = _html;
            page.rev = _version;
            page.text = "";
            _db.pageDao().insertAll(page);
            return "insert done";
        }
        else
        {
            Log.d(TAG, " existing item found "+existing_item.pagename);
            _db.pageDao().updateHtml(_pagename, _html);
            _db.pageDao().updateVersion(_pagename, _version);
            return "update done";
        }
    }

    @Override
    protected String doInBackground(String... params) {
        return doSync();
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        Log.d(TAG, "DB "+result);

    }
}