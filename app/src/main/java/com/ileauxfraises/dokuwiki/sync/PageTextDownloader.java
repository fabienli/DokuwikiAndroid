package com.ileauxfraises.dokuwiki.sync;

import android.content.Context;

import com.ileauxfraises.dokuwiki.WikiCacheUiOrchestrator;

public class PageTextDownloader extends XmlRpcDownload {
    WikiCacheUiOrchestrator _wikiMngr;
    Boolean _directDisplay = false;

    public PageTextDownloader(Context context, WikiCacheUiOrchestrator wikiCacheUiOrchestrator, Boolean directDisplay) {
        super(context);
        _wikiMngr = wikiCacheUiOrchestrator;
        _directDisplay = directDisplay;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        _wikiMngr.savePageTextInCache(_xmlrpc_results, _pagename, _directDisplay);

    }

}