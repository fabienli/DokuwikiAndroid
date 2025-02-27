package com.fabienli.dokuwiki.sync;

import android.util.Log;

import java.util.ArrayList;

public class WikiTitleRetriever {
    protected String TAG = "WikiTitleRetriever";
    protected XmlRpcAdapter _xmlRpcAdapter = null;

    public WikiTitleRetriever(XmlRpcAdapter xmlRpcAdapter) {
        _xmlRpcAdapter = xmlRpcAdapter;
    }

    public String retrieveTitle() {
        if (_xmlRpcAdapter.useOldApi())
            return retrieveTitleDeprecated();
        return retrieveTitleNew();
    }
    public String retrieveTitleDeprecated() {
        Log.d(TAG, "Looking for wiki title, old way");
        ArrayList<String> resultList = _xmlRpcAdapter.callMethod("dokuwiki.getTitle");
        if (resultList != null && resultList.size() > 0)
            return resultList.get(0);
        else
            return "";
    }
    public String retrieveTitleNew() {
        //Log.d(TAG, "Looking for wiki title new");
        ArrayList<String> resultList = _xmlRpcAdapter.callMethod("core.getWikiTitle");

        if (resultList != null && resultList.size() > 0) {
            Log.d(TAG, "Found wiki title: " + resultList.get(0));
            return resultList.get(0);
        }
        else
            return "";
    }
}
