package com.fabienli.dokuwiki.sync;

import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.Base64;


public class MediaDownloader {
    protected String TAG = "MediaDownloader";
    protected XmlRpcAdapter _xmlRpcAdapter = null;

    public MediaDownloader(XmlRpcAdapter xmlRpcAdapter) {
        _xmlRpcAdapter = xmlRpcAdapter;
    }

    public byte[] retrieveMedia(String medianame){
        if(_xmlRpcAdapter.useOldApi())
            return retrieveMediaDeprecated(medianame);
        Log.d(TAG,"Get Media file "+medianame);
        ArrayList<String> resultList = _xmlRpcAdapter.callMethod("core.getMedia", medianame);
        byte[] resultMedia = null;
        for (String item : resultList) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                resultMedia = Base64.getDecoder().decode(item);
            }
        }
        return resultMedia;
    }

    public boolean uploadMedia(String medianame, String filename){
        if(true || _xmlRpcAdapter.useOldApi()) // to be implemented
            return uploadMediaDeprecated(medianame, filename);
        Log.d(TAG,"Put Media file "+filename);
        ArrayList<String> result = _xmlRpcAdapter.callMethod("core.saveMedia", medianame, "file://"+filename, "{}");
        return (result!= null && result.size()>0);
    }

    public boolean deleteMedia(String medianame){
        if(true || _xmlRpcAdapter.useOldApi()) // to be implemented
            return deleteMediaDeprecated(medianame);
        Log.d(TAG,"Delete Media file "+medianame);
        ArrayList<String> result = _xmlRpcAdapter.callMethod("core.deleteMedia", medianame);
        return (result!= null && result.size()>0);
    }


    public byte[] retrieveMediaDeprecated(String medianame){
        Log.d(TAG,"Get Media file "+medianame);
        byte[] resultMedia = _xmlRpcAdapter.callMethodBinary("wiki.getAttachment", medianame);
        return resultMedia;
    }

    public boolean uploadMediaDeprecated(String medianame, String filename){
        Log.d(TAG,"Put Media file "+filename);
        ArrayList<String> result = _xmlRpcAdapter.callMethod("wiki.putAttachment", medianame, "file://"+filename, "{}");
        return (result!= null && result.size()>0);
    }

    public boolean deleteMediaDeprecated(String medianame){
        Log.d(TAG,"Delete Media file "+medianame);
        ArrayList<String> result = _xmlRpcAdapter.callMethod("wiki.deleteAttachment", medianame);
        return (result!= null && result.size()>0);
    }
}
