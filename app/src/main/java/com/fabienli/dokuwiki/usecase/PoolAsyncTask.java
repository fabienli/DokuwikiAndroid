package com.fabienli.dokuwiki.usecase;

import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public abstract class PoolAsyncTask extends AsyncTask<String, Integer, String> {
    static List<PoolAsyncTask> _poolAsyncTask = null;
    static String TAG = "PoolAsyncTask";
    public PoolAsyncTask(){
        if(_poolAsyncTask == null)
            _poolAsyncTask = new ArrayList<PoolAsyncTask>() ;
        _poolAsyncTask.add(this);
        Log.d(TAG, "add in pool: "+this);
    }

    public static void cleanPendingTasks(){
        for(PoolAsyncTask p : _poolAsyncTask){
            p.cancel(true);
            Log.d(TAG, "cancel from pool: "+p);
        }
    }


    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        _poolAsyncTask.remove(this);
        Log.d(TAG, "remove from pool: "+this);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        _poolAsyncTask.remove(this);
        Log.d(TAG, "remove from pool as cancelled: "+this);
    }
}
