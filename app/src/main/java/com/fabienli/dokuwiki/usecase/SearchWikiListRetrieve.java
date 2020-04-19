package com.fabienli.dokuwiki.usecase;

import android.os.AsyncTask;
import android.util.Log;

import androidx.sqlite.db.SimpleSQLiteQuery;

import com.fabienli.dokuwiki.db.AppDatabase;
import com.fabienli.dokuwiki.db.Page;
import com.fabienli.dokuwiki.usecase.callback.PageHtmlRetrieveCallback;

import java.util.ArrayList;
import java.util.List;

public class SearchWikiListRetrieve extends PoolAsyncTask {
    protected AppDatabase _db = null;
    PageHtmlRetrieveCallback _pageHtmlRetrieveCallback = null;
    String _pageContent = "";
    protected String _searchDataText = "";
    protected String _searchDataQuery = "";
    protected ArrayList<String> _searchDataQueries = new ArrayList<String>();

    public SearchWikiListRetrieve(AppDatabase db) {
        _db = db;
    }

    public void setSearchData(String data){
        _searchDataText = data;
        if(data.startsWith("\"")) {
            _searchDataQuery = "%"+data.replaceAll("\"","%")+"%";
            _searchDataQuery = _searchDataQuery.replaceAll("%%","%");
        } else if(data.split(" ").length>0) {
            for(String s : data.split(" ")) {
                _searchDataQueries.add("%" + s + "%");
            }
            _searchDataQuery = "";
        } else {
            _searchDataQuery = "%" + data + "%";
            _searchDataQuery = _searchDataQuery.replaceAll(" ", "%");
            _searchDataQuery = _searchDataQuery.replaceAll("%%", "%");
        }
    }

    public String getPageResultsList() {
        String htmlPage = "<p>Results for: '"+_searchDataText+"'</p><ul>";
        String localOrNot = "";
        List<Page> pageListResults;
        if(_searchDataQuery.length()>0 || _searchDataQueries.size() == 0) {
            Log.d(TAG, "search with " + _searchDataQuery);
            pageListResults = _db.pageDao().search(_searchDataQuery);
        } else {
            Log.d(TAG, "search with " + _searchDataQueries);
            String seachQuery = "SELECT * FROM page WHERE ";
            String preQuery = "";
            for (String s : _searchDataQueries) {
                seachQuery += preQuery + "(text LIKE '" + s + "' or html LIKE '" + s + "')";
                preQuery = " AND ";
            }
            pageListResults = _db.pageDao().search(new SimpleSQLiteQuery(seachQuery));
        }
        for (Page page : pageListResults) {
            if (page.isHtmlEmpty())
                localOrNot = " [not in local]";
            else if (_db.syncActionDao().isSyncNeeded(page.pagename).size() > 0)
                localOrNot = " [need sync]";
            else
                localOrNot = "";
            htmlPage += "\n<li><a href=\"http://dokuwiki/doku.php?id=" + page.pagename + "\">" + page.pagename + "</a>" + localOrNot + "</li>";
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
