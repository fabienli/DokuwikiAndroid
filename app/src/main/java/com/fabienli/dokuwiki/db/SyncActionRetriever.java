package com.fabienli.dokuwiki.db;

import android.os.AsyncTask;
import android.util.Log;

import java.util.List;

public class SyncActionRetriever extends AsyncTask<String, Void, String> {
    AppDatabase _db = null;
    String TAG = "SyncActionRetriever";
    SyncActionListInterface _syncActionListInterface;
    List<SyncAction> _syncActionList;

    public SyncActionRetriever(AppDatabase db, SyncActionListInterface syncActionListInterface) {
        _db = db;
        _syncActionListInterface = syncActionListInterface;
    }

    @Override
    protected String doInBackground(String... strings) {
        _syncActionList = _db.syncActionDao().getAll();

        return "OK";
    }


    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        Log.d(TAG, "DB get Actions"+result);
        _syncActionListInterface.handle(_syncActionList);
    }
}

