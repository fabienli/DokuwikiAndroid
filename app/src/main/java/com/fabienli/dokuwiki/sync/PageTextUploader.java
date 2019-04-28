package com.fabienli.dokuwiki.sync;

import android.content.Context;

import com.fabienli.dokuwiki.WikiCacheUiOrchestrator;

public class PageTextUploader  extends XmlRpcDownload {
    WikiCacheUiOrchestrator _wikiMngr;

    public PageTextUploader(Context context, WikiCacheUiOrchestrator wikiCacheUiOrchestrator) {
        super(context);
        _wikiMngr = wikiCacheUiOrchestrator;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        _wikiMngr.uploadedPageText(_xmlrpc_results, _pagename);

    }

}