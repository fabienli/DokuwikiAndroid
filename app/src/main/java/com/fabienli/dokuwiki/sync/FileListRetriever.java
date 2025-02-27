package com.fabienli.dokuwiki.sync;

import android.util.Log;

import java.util.ArrayList;

public class FileListRetriever {
    protected String TAG = "FileListRetriever";
    protected XmlRpcAdapter _xmlRpcAdapter = null;

    public FileListRetriever(XmlRpcAdapter xmlRpcAdapter) {
        _xmlRpcAdapter = xmlRpcAdapter;
    }

    public ArrayList<String> retrievePagesList(String namespace){
        if(_xmlRpcAdapter.useOldApi())
            return retrievePagesListDeprecated(namespace);
        Log.d(TAG,"Looking for pages in "+namespace);
        ArrayList<String> resultList = _xmlRpcAdapter.callMethod("core.listPages", namespace,"{}");
        return resultList;
    }
    public ArrayList<String> retrievePagesListDeprecated(String namespace){
        Log.d(TAG,"Looking for pages in "+namespace);
        ArrayList<String> resultList = _xmlRpcAdapter.callMethod("dokuwiki.getPagelist", namespace,"{}");

        return resultList;
    }

    public ArrayList<String> retrieveMediasList(String namespace){
        if(_xmlRpcAdapter.useOldApi())
            return retrieveMediasListDeprecated(namespace);
        Log.d(TAG,"Looking for medias in "+namespace);
        ArrayList<String> resultList = _xmlRpcAdapter.callMethod("core.listMedia", namespace);

        return resultList;
    }
    public ArrayList<String> retrieveMediasListDeprecated(String namespace){
        Log.d(TAG,"Looking for medias in "+namespace);
        ArrayList<String> resultList = _xmlRpcAdapter.callMethod("wiki.getAttachments", namespace);

        return resultList;
    }
}
