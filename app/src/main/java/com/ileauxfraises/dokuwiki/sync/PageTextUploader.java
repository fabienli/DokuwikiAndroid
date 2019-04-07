package com.ileauxfraises.dokuwiki.sync;

import android.content.Context;

import com.ileauxfraises.dokuwiki.WikiManager;

public class PageTextUploader  extends XmlRpcDownload {
    WikiManager _wikiMngr;

    public PageTextUploader(Context context, WikiManager wikiManager) {
        super(context);
        _wikiMngr = wikiManager;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        _wikiMngr.uploadedPageText(results, _pagename);

    }

}