package com.ileauxfraises.dokuwiki.sync;

import android.content.Context;
import android.util.Log;

import com.ileauxfraises.dokuwiki.WikiCacheUiOrchestrator;

public class PageListRetriever extends XmlRpcDownload {
    WikiCacheUiOrchestrator _wikiMngr;
    public PageListRetriever(Context context, WikiCacheUiOrchestrator wikiCacheUiOrchestrator) {
        super(context);
        _wikiMngr = wikiCacheUiOrchestrator;
    }

    public void retrievePageList(String namespace){
        Log.d(TAG,"Looking for pages in "+namespace);
        this.execute("dokuwiki.getPagelist",namespace,"{}");
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        _wikiMngr.updateCacheWithPageListReceived(_xmlrpc_results);

    }

}
