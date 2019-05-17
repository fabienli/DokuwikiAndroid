package com.fabienli.dokuwiki.db;

import android.os.AsyncTask;
import android.util.Log;

public class PageRemove extends AsyncTask<String, Void, String> {
    AppDatabase _db = null;
    String TAG = "PageRemove";
    String _pagename;
    DbCallbackInterface _dbCallbackInterface = null;

    public PageRemove(AppDatabase db, String pagename, DbCallbackInterface dbCallbackInterface) {
        _db = db;
        _pagename = pagename;
        _dbCallbackInterface = dbCallbackInterface;
    }


    @Override
    protected String doInBackground(String... params) {
        Page existing_item = _db.pageDao().findByName(_pagename);
        if(existing_item != null)
        {
            Log.d(TAG, " existing item found "+existing_item.pagename);
            _db.pageDao().delete(existing_item);
            return "delete done";
        }
        return "no item in DB";
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        Log.d(TAG, "DB "+result);
        if(_dbCallbackInterface != null){
            _dbCallbackInterface.onceDone();
        }
    }
}
