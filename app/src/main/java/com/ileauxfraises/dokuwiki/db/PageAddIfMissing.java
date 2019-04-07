package com.ileauxfraises.dokuwiki.db;

import android.os.AsyncTask;
import android.util.Log;

public class PageAddIfMissing extends AsyncTask<String, Void, String> {
    Page _item = null;
    AppDatabase _db = null;
    String TAG = "PageAddIfMissing";

    public PageAddIfMissing(AppDatabase db, Page page) {
        _db = db;
        _item = page;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... params) {
        Page existing_item = _db.pageDao().findByName(_item.pagename);

        if(existing_item == null)
        {
            Log.d(TAG, "no existing item found ");
            _db.pageDao().insertAll(_item);
            return "insert done";
        }
        else
        {
            Log.d(TAG, " existing item found "+existing_item.pagename);

            return "nothing done";
        }
        //return "done";
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        Log.d(TAG, "DB "+result);

    }
}