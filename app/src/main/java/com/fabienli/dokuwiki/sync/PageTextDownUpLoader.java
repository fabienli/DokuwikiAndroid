package com.fabienli.dokuwiki.sync;

import android.util.Log;

import java.util.ArrayList;

public class PageTextDownUpLoader {
    protected String TAG = "PageTextDownUpLoader";
    protected XmlRpcAdapter _xmlRpcAdapter = null;

    public PageTextDownUpLoader(XmlRpcAdapter xmlRpcAdapter) {
        _xmlRpcAdapter = xmlRpcAdapter;
    }

    public String retrievePageText(String pagename){
        Log.d(TAG,"GetPage TEXT "+pagename);
        ArrayList<String> resultList = _xmlRpcAdapter.callMethod("wiki.getPage", pagename);
        if((resultList != null) && (resultList.size() == 1))
            return resultList.get(0);
        return "";
    }
    public String sendPageText(String pagename, String textcontent){
        Log.d(TAG,"GetPage TEXT "+pagename);
        ArrayList<String> resultList = _xmlRpcAdapter.callMethod("wiki.putPage", pagename, textcontent, "{}");
        if((resultList != null) && (resultList.size() == 1))
            return resultList.get(0);
        return "ok";
    }
}
