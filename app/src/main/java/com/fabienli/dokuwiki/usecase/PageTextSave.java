package com.fabienli.dokuwiki.usecase;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.fabienli.dokuwiki.db.AppDatabase;
import com.fabienli.dokuwiki.db.Page;
import com.fabienli.dokuwiki.db.SyncAction;
import com.fabienli.dokuwiki.sync.XmlRpcAdapter;
import com.fabienli.dokuwiki.tools.Logs;
import com.fabienli.dokuwiki.usecase.callback.WikiSynchroCallback;

public class PageTextSave extends PoolAsyncTask {
    String TAG = "PageTextSave";
    protected AppDatabase _db;
    WikiSynchroCallback _wikiSynchroCallback = null;
    SharedPreferences _settings;
    XmlRpcAdapter _xmlRpcAdapter;

    public PageTextSave(AppDatabase db, SharedPreferences settings, XmlRpcAdapter xmlRpcAdapter) {
        _db = db;
        _settings = settings;
        _xmlRpcAdapter = xmlRpcAdapter;
    }

    public void savePageText(String pagename, String newtext) {

        // 1. upload the new text to wiki: put it in sync action queue
        Logs.getInstance().add("text page "+pagename+" updated, uploading it to server");
        Log.d(TAG, "text page "+pagename+" updated, uploading it to server");
        String pageCurrentVersion = "";
        Page page = _db.pageDao().findByName(pagename);
        if(page != null) {
            pageCurrentVersion = page.rev;
        }

        SyncAction existingSyncAction = _db.syncActionDao().findUnique("0", "PUT", pagename);
        if(existingSyncAction == null) {
            SyncAction syncAction = new SyncAction();
            syncAction.priority = "0";
            syncAction.verb = "PUT";
            syncAction.name = pagename;
            syncAction.rev = pageCurrentVersion;
            syncAction.data = newtext;
            _db.syncActionDao().insertAll(syncAction);
        }
        else {
            existingSyncAction.rev = pageCurrentVersion;
            existingSyncAction.data = newtext;
            _db.syncActionDao().update(existingSyncAction);
        }

        // 2. save also in local DB
        _db.pageDao().updateText(pagename, newtext);

        // 3. call sync to upload
        SynchroDownloadHandler synchroDownloadHandler = new SynchroDownloadHandler(_settings, _db, _xmlRpcAdapter, "", null);
        Logs.getInstance().add("Retry the urgent items to be synced");
        synchroDownloadHandler.syncPrioZero();
    }

    public void savePageTextAsync(String pagename, String newtext, WikiSynchroCallback wikiSynchroCallback) {
        _wikiSynchroCallback = wikiSynchroCallback;
        executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, pagename, newtext);
    }


    @Override
    protected String doInBackground(String... params) {
        savePageText(params[0], params[1]);
        return "ok";
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if(_wikiSynchroCallback!=null)
            _wikiSynchroCallback.onceDone();
    }

}
