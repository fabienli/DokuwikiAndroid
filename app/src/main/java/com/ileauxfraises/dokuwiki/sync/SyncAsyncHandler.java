package com.ileauxfraises.dokuwiki.sync;

import android.content.Context;

import com.ileauxfraises.dokuwiki.WikiCacheUiOrchestrator;

public class SyncAsyncHandler {

    protected WikiCacheUiOrchestrator _wikiCacheUiOrchestrator = null;

    public void setWikiManagerCallback(WikiCacheUiOrchestrator wikiCacheUiOrchestrator) {
        _wikiCacheUiOrchestrator = wikiCacheUiOrchestrator;
    }

    public PageTextDownloader getPageTextDownloader(Context context, Boolean directDisplay) {
        return new PageTextDownloader(context, _wikiCacheUiOrchestrator, directDisplay);
    }

    public PageListRetriever getPageListRetriever(Context context) {
        return new PageListRetriever(context, _wikiCacheUiOrchestrator);
    }

    public PageHtmlDownloader getPageHtmlDownloader(Context context, Boolean directDisplay) {
        return new PageHtmlDownloader(context, _wikiCacheUiOrchestrator, directDisplay);
    }

    public PageTextUploader getPageTextUploader(Context context) {
        return new PageTextUploader(context, _wikiCacheUiOrchestrator);
    }

    public MultiPageHtmlDownloader getMultiPageHtmlDownloader(Context context) {
        return new MultiPageHtmlDownloader(context, _wikiCacheUiOrchestrator);
    }
}
