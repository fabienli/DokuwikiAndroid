package com.fabienli.dokuwiki.db;

import android.os.AsyncTask;
import android.util.Log;

public class SyncActionHandler extends AsyncTask<String, Void, String> {
    AppDatabase _db = null;
    String TAG = "SyncActionHandler";
    SyncAction _syncAction;
    String _level;
    Boolean _insert = true;
    DbCallbackInterface _dbCallbackInterface;

    public SyncActionHandler(AppDatabase db, DbCallbackInterface dbCallbackInterface) {
        _db = db;
        _dbCallbackInterface = dbCallbackInterface;
    }

    public void insert(SyncAction syncAction){
        _syncAction = syncAction;
        _insert = true;
        execute();
    }

    public void delete(SyncAction syncAction){
        _syncAction = syncAction;
        _insert = false;
        execute();
    }

    public void deleteLevel(String level){
        _syncAction = null;
        _insert = false;
        _level = level;
        execute();
    }

    @Override
    protected String doInBackground(String... strings) {
        if(_insert) {
            SyncAction existing_item = _db.syncActionDao().findUnique(_syncAction.priority, _syncAction.verb, _syncAction.name);
            if(existing_item != null) {
                //Log.d(TAG, "item already there ! "+ existing_item.verb + " : "+ existing_item.name);
                _db.syncActionDao().deleteAll(_syncAction);
            }
            _db.syncActionDao().insertAll(_syncAction);
        }
        else if (_syncAction != null) {
            _db.syncActionDao().deleteAll(_syncAction);
        }
        else {
            _db.syncActionDao().deleteLevel(_level);
        }
        return "OK";
    }


    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        /*
        if(_insert)
            Log.d(TAG, "DB Insert Sync Action "+result);
        else
            Log.d(TAG, "DB Delete Sync Action "+result);
        */
        if(_dbCallbackInterface!=null)
            _dbCallbackInterface.onceDone();
    }
}

