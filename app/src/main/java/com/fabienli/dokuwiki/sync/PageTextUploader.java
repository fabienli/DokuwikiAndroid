package com.fabienli.dokuwiki.sync;

import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;

import com.fabienli.dokuwiki.WikiCacheUiOrchestrator;

public class PageTextUploader extends XmlRpcDownload {
    public PageTextUploader(Context context) {
        super(context);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        setDialogMsg("Uploading "+_pagename);
    }

    public void uploadPageText(String pagename, String textcontent){
        _pagename = pagename;
        Log.d(TAG,"Upload Text for: "+pagename);
        execute("wiki.putPage", pagename, textcontent, "{}");
    }

}