package com.fabienli.dokuwiki.sync;

import android.util.Log;

import java.util.ArrayList;

public class PageHtmlDownloader {
    protected String TAG = "PageHtmlDownloader";
    protected XmlRpcAdapter _xmlRpcAdapter = null;

    public PageHtmlDownloader(XmlRpcAdapter xmlRpcAdapter) {
        _xmlRpcAdapter = xmlRpcAdapter;
    }

    public String retrievePageHTML(String pagename){
        if(_xmlRpcAdapter.useOldApi())
            return retrievePageHTMLDeprecated(pagename);
        Log.d(TAG,"GetPage HTML "+pagename);
        ArrayList<String> resultList = _xmlRpcAdapter.callMethod("core.getPageHTML", pagename);
        if(resultList!=null && resultList.size() == 1)
            return resultList.get(0);
        return "";
    }
    public String retrievePageHTMLDeprecated(String pagename){
        Log.d(TAG,"GetPage HTML "+pagename);
        ArrayList<String> resultList = _xmlRpcAdapter.callMethod("wiki.getPageHTML", pagename);
        if(resultList!=null && resultList.size() == 1)
            return resultList.get(0);
        return "";
    }

}
