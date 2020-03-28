package com.fabienli.dokuwiki.usecase;

import android.os.AsyncTask;

import com.fabienli.dokuwiki.db.AppDatabase;
import com.fabienli.dokuwiki.db.Page;
import com.fabienli.dokuwiki.usecase.callback.PageHtmlRetrieveCallback;

public class SearchWikiListRetrieve extends PoolAsyncTask {
    protected AppDatabase _db = null;
    PageHtmlRetrieveCallback _pageHtmlRetrieveCallback = null;
    String _pageContent = "";
    protected String _searchData = "";

    public SearchWikiListRetrieve(AppDatabase db) {
        _db = db;
    }

    public void setSearchData(String data){
        _searchData= data;
    }

    public String getPageResultsList() {
        String htmlPage = "<p>Results for: '"+_searchData+"'</p><ul>";
        String localOrNot = "";
        for (Page page : _db.pageDao().search(_searchData)) {
            if(page.isHtmlEmpty())
                localOrNot = " [not in local]";
            else if(_db.syncActionDao().isSyncNeeded(page.pagename).size()>0)
                localOrNot = " [need sync]";
            else
                localOrNot = "";
            htmlPage += "\n<li><a href=\"http://dokuwiki/doku.php?id=" + page.pagename + "\">" + page.pagename + "</a>"+localOrNot+"</li>";
        }
        htmlPage += "\n</ul>";
        return htmlPage;
    }

    public void getPageResultsListAsync(PageHtmlRetrieveCallback pageHtmlRetrieveCallback) {
        _pageHtmlRetrieveCallback = pageHtmlRetrieveCallback;
        executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    @Override
    protected String doInBackground(String... params) {
        _pageContent = getPageResultsList();
        return "ok";
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if(_pageHtmlRetrieveCallback!=null)
            _pageHtmlRetrieveCallback.pageRetrieved(_pageContent);
    }
}
