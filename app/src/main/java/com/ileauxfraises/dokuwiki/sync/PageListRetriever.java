package com.ileauxfraises.dokuwiki.sync;

import android.content.Context;

import com.ileauxfraises.dokuwiki.WikiManager;

public class PageListRetriever extends XmlRpcDownload {
    WikiManager _wikiMngr;
    public PageListRetriever(Context context, WikiManager wikiManager) {
        super(context);
        _wikiMngr = wikiManager;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        _wikiMngr.retrievedPageList(results);

    }

}
