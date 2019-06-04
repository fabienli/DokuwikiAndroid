package com.fabienli.dokuwiki.sync;

import android.util.Log;


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

    public void uploadMedia(String filename){
        Log.d(TAG,"Put Media file "+filename);
        // TODO:  _xmlRpcAdapter.callMethodBinary("wiki.", ...);

    }
}
