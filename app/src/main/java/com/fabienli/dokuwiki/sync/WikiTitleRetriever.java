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
        Log.d(TAG, "Looking for wiki title");
        ArrayList<String> resultList = _xmlRpcAdapter.callMethod("dokuwiki.getTitle");
        if (resultList != null && resultList.size() > 0)
            return resultList.get(0);
        else
            return "";
    }
}
