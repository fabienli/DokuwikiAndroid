package com.fabienli.dokuwiki.usecase;

import android.os.AsyncTask;

import com.fabienli.dokuwiki.db.AppDatabase;
import com.fabienli.dokuwiki.db.Page;
import com.fabienli.dokuwiki.usecase.callback.PageHtmlRetrieveCallback;

public class PageListRetrieve extends AsyncTask<String, Integer, String> {
    protected AppDatabase _db = null;
    PageHtmlRetrieveCallback _pageHtmlRetrieveCallback = null;
    String _pageContent = "";

    public PageListRetrieve(AppDatabase db) {
        _db = db;
    }

    public String getPageList() {
        String htmlPage = "<ul>";
        String localOrNot = "";
        for (Page page : _db.pageDao().getAll()) {
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

    public void getPageListAsync(PageHtmlRetrieveCallback pageHtmlRetrieveCallback) {
        _pageHtmlRetrieveCallback = pageHtmlRetrieveCallback;
        execute();
    }


    @Override
    protected String doInBackground(String... params) {
        _pageContent = getPageList();
        return "ok";
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if(_pageHtmlRetrieveCallback!=null)
            _pageHtmlRetrieveCallback.pageRetrieved(_pageContent);
    }
}
