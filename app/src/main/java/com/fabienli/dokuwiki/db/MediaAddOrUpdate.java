package com.fabienli.dokuwiki.db;

import android.os.AsyncTask;
import android.util.Log;

public class MediaAddOrUpdate extends AsyncTask<String, Void, String> {
    Media _item = null;
    AppDatabase _db = null;
    String TAG = "MediaAddOrUpdate";

    public MediaAddOrUpdate(AppDatabase db, Media media) {
        _db = db;
        _item = media;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... params) {
        Media existing_item = _db.mediaDao().findByName(_item.id);

        if(existing_item == null)
        {
            //Log.d(TAG, "no existing item found ");
            _db.mediaDao().insertAll(_item);
            return "insert done: "+_item.id;
        }
        else
        {
            //Log.d(TAG, " existing item found "+existing_item.id);
            _db.mediaDao().updateAll(_item);
            return "update done: "+_item.id;
        }
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        //Log.d(TAG, "DB "+result);
    }
}
