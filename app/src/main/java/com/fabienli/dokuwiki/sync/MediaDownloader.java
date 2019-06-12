package com.fabienli.dokuwiki.sync;

import android.util.Log;

import java.util.ArrayList;


public class MediaDownloader {
    protected String TAG = "MediaDownloader";
    protected XmlRpcAdapter _xmlRpcAdapter = null;

    public MediaDownloader(XmlRpcAdapter xmlRpcAdapter) {
        _xmlRpcAdapter = xmlRpcAdapter;
    }

    public byte[] retrieveMedia(String medianame){
        Log.d(TAG,"Get Media file "+medianame);
        byte[] resultMedia = _xmlRpcAdapter.callMethodBinary("wiki.getAttachment", medianame);
        return resultMedia;
    }
    public boolean uploadMedia(String medianame, String filename){
        Log.d(TAG,"Put Media file "+filename);
        ArrayList<String> result = _xmlRpcAdapter.callMethod("wiki.putAttachment", medianame, "file://"+filename, "{}");
        return (result!= null && result.size()>0);
    }
}
