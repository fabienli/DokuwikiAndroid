package com.fabienli.dokuwiki.sync;

import android.os.Build;
import android.util.Log;

import com.fabienli.dokuwiki.tools.Logs;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;


public class MediaDownloader {
    protected String TAG = "MediaDownloader";
    protected XmlRpcAdapter _xmlRpcAdapter = null;

    public MediaDownloader(XmlRpcAdapter xmlRpcAdapter) {
        _xmlRpcAdapter = xmlRpcAdapter;
    }

    public byte[] retrieveMedia(String medianame){
        if(_xmlRpcAdapter.useOldApi() || (Build.VERSION.SDK_INT < Build.VERSION_CODES.O))
            return retrieveMediaDeprecated(medianame);
        Log.d(TAG,"Get Media file "+medianame);
        ArrayList<String> resultList = _xmlRpcAdapter.callMethod("core.getMedia", medianame);
        if (resultList == null)
            return null;
        byte[] resultMedia = null;
        for (String item : resultList) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                resultMedia = Base64.getDecoder().decode(item);
            }
        }
        return resultMedia;
    }

    public boolean uploadMedia(String medianame, String filename) {
        if(_xmlRpcAdapter.useOldApi() || (Build.VERSION.SDK_INT < Build.VERSION_CODES.O))
            return uploadMediaDeprecated(medianame, filename);
        Log.d(TAG,"Put Media file "+filename+" as "+medianame);
        ArrayList<String> result = null;
        try {
            FileInputStream fis = new FileInputStream(filename);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            byte[] fileBytes = baos.toByteArray();
            String b64file = Base64.getEncoder().encodeToString(fileBytes);
            result = _xmlRpcAdapter.callMethod("core.saveMedia", medianame, b64file);
        } catch (FileNotFoundException e) {
            Log.e(TAG,"Upload media error: " + e);
            Logs.getInstance().add("Upload media error: " + e);
        } catch (IOException e) {
            Log.e(TAG,"Upload media error: " + e);
            Logs.getInstance().add("Upload media error: " + e);
        }
        return (result!= null && result.size()>0);
    }

    public boolean deleteMedia(String medianame){
        if(_xmlRpcAdapter.useOldApi())
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
