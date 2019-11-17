package com.fabienli.dokuwiki.usecase;

import android.os.AsyncTask;

import com.fabienli.dokuwiki.db.AppDatabase;
import com.fabienli.dokuwiki.db.Page;
import com.fabienli.dokuwiki.db.SyncAction;
import com.fabienli.dokuwiki.usecase.callback.PageHtmlRetrieveCallback;

public class ActionListRetrieve extends PoolAsyncTask {
    protected AppDatabase _db;
    PageHtmlRetrieveCallback _pageHtmlRetrieveCallback = null;
    String _pageContent = "";

    public ActionListRetrieve(AppDatabase db) {
        _db = db;
    }

    public String getSyncActionList() {
        String itemsList = "<ul>";
        int[] counter = {0,0,0,0,0,0};
        int priority = 0;
        for (SyncAction sa : _db.syncActionDao().getAll()) {
            itemsList += "\n<li>" + sa.toText() + "</li>";
            priority = Integer.parseInt(sa.priority);
            counter[priority]++;
        }
        itemsList += "\n</ul>";
        String htmlPage = "<table>"+
                "\n<tr><td>Prio 0: </td><td>"+counter[0]+"</td></tr>"+
                "\n<tr><td>Prio 1: </td><td>"+counter[1]+"</td></tr>"+
                "\n<tr><td>Prio 2: </td><td>"+counter[2]+"</td></tr>"+
                "\n<tr><td>Prio 3: </td><td>"+counter[3]+"</td></tr>"+
                "\n<tr><td>Prio 5: </td><td>"+counter[5]+"</td></tr>\n</table>\n"+itemsList;
        return htmlPage;
    }

    public void getSyncActionListAsync(PageHtmlRetrieveCallback pageHtmlRetrieveCallback) {
        _pageHtmlRetrieveCallback = pageHtmlRetrieveCallback;
        executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    @Override
    protected String doInBackground(String... params) {
        _pageContent = getSyncActionList();
        return "ok";
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if(_pageHtmlRetrieveCallback!=null)
            _pageHtmlRetrieveCallback.pageRetrieved(_pageContent);
    }
}
