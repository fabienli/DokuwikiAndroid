package com.fabienli.dokuwiki.sync;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;

import com.fabienli.dokuwiki.WikiCacheUiOrchestrator;

import java.util.ArrayList;

public class PageHtmlDownloader extends XmlRpcDownload {
    WikiCacheUiOrchestrator _wikiMngr;
    Boolean _directDisplay = false;

    public PageHtmlDownloader(Context context, WikiCacheUiOrchestrator wikiCacheUiOrchestrator, Boolean directDisplay) {
        super(context);
        _wikiMngr = wikiCacheUiOrchestrator;
        _directDisplay = directDisplay;
    }

    public void retrievePageHTML(String pagename){
        _isRawResult = true;
        _pagename = pagename;
        Log.d(TAG,"GetPage HTML "+pagename);
        execute("wiki.getPageHTML", pagename);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        String message= "Get page: "+_pagename;

        SpannableString ss2 =  new SpannableString(message);
        ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
        ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);

        _dialog.setMessage(ss2);
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        processXmlRpcResult(_xmlrpc_results);
    }

    protected void processXmlRpcResult(ArrayList<String> results) {
        String html = "";
        if(results.size()==1){
            Log.d(TAG,"page "+_pagename+" retrieved from server" );
            html = results.get(0);
            _wikiMngr.updatePageInCache(_pagename, html);
        }
        else
        {
            html = "<html><body>Error ...</body></html>";
            Log.d(TAG,"error when downloading page "+_pagename+" from server");
        }
        if(_directDisplay)
            _wikiMngr.loadPage(html);
    }

}
