package com.fabienli.dokuwiki.sync;

import android.content.Context;
import android.util.Log;

import com.fabienli.dokuwiki.WikiCacheUiOrchestrator;

public class PageTextDownloader extends XmlRpcDownload {
    WikiCacheUiOrchestrator _wikiMngr;
    Boolean _directDisplay = false;

    public PageTextDownloader(Context context, WikiCacheUiOrchestrator wikiCacheUiOrchestrator, Boolean directDisplay) {
        super(context);
        _wikiMngr = wikiCacheUiOrchestrator;
        _directDisplay = directDisplay;
    }

    public void retrievePageText(String pagename){
        _isRawResult = true;
        _pagename = pagename;
        Log.d(TAG,"GetPage Text "+pagename);
        execute("wiki.getPage", pagename);
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        _wikiMngr.savePageTextInCache(_xmlrpc_results, _pagename, _directDisplay);

    }

}