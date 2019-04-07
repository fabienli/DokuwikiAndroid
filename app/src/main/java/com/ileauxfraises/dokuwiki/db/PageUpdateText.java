package com.ileauxfraises.dokuwiki.db;

import android.os.AsyncTask;
import android.util.Log;

public class PageUpdateText extends AsyncTask<String, Void, String> {
    AppDatabase _db = null;
    String TAG = "PageUpdateText";
    String _pagename;
    String _text;

    public PageUpdateText(AppDatabase db, String pagename, String edit_text) {
        _db = db;
        _pagename = pagename;
        _text = edit_text;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... params) {
        Page existing_item = _db.pageDao().findByName(_pagename);

        if(existing_item == null)
        {
            Log.d(TAG, "no existing item found ");
            Page page = new Page();
            page.pagename = _pagename;
            page.html = "";
            page.rev = "0";
            page.text = _text;
            _db.pageDao().insertAll(page);
            return "insert done";
        }
        else
        {
            Log.d(TAG, " existing item found "+existing_item.pagename);
            _db.pageDao().updateText(_pagename, _text);
            return "update done";
        }
        //return "done";
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        Log.d(TAG, "DB "+result);

    }
}