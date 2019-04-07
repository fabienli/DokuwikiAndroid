package com.ileauxfraises.dokuwiki.sync;

import android.content.Context;

import com.ileauxfraises.dokuwiki.WikiManager;

public class PageHtmlDownloader extends XmlRpcDownload {
    WikiManager _wikiMngr;
    Boolean _directDisplay = false;

    public PageHtmlDownloader(Context context, WikiManager wikiManager, Boolean directDisplay) {
        super(context);
        _wikiMngr = wikiManager;
        _directDisplay = directDisplay;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        _wikiMngr.retrievedPageHtml(results, _pagename, _directDisplay);

    }

}
