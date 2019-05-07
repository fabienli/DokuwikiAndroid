package com.fabienli.dokuwiki.sync;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;

import com.fabienli.dokuwiki.WikiCacheUiOrchestrator;

import java.util.ArrayList;

public class MediaDownloader extends XmlRpcDownload {
    static String TAG = "MediaDownloader";
    WikiCacheUiOrchestrator _wikiMngr;
    Boolean _directDisplay = false;
    String _localPath;
    int _width;
    int _height;

    public MediaDownloader(Context context, WikiCacheUiOrchestrator wikiCacheUiOrchestrator, int width, int height, Boolean directDisplay) {
        super(context);
        _wikiMngr = wikiCacheUiOrchestrator;
        _directDisplay = directDisplay;
        _width = width;
        _height = height;
    }
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        String message= "Get image: "+_pagename;

        SpannableString ss2 =  new SpannableString(message);
        ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
        ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);

        _dialog.setMessage(ss2);
        _dialog.cancel();
    }

    public void downloadMedia(String mediaId, String mediaLocalPath){
        _isRawResult = true;
        _pagename = mediaId;
        _localPath = mediaLocalPath;
        Log.d(TAG,"Get media <"+mediaId+"> to: "+mediaLocalPath);
        execute("wiki.getAttachment", mediaId);
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        processXmlRpcResult(_xmlrpc_binary_results);
    }

    protected void processXmlRpcResult(byte[] results) {
        _wikiMngr.createLocalFile(results, _localPath);
        if(_width != 0 || _height !=0)
            _wikiMngr.createLocalFileResized(_localPath, _width, _height);
        if(_directDisplay) {
            // refresh the page
            _wikiMngr.refreshPage();
        }
    }
}
