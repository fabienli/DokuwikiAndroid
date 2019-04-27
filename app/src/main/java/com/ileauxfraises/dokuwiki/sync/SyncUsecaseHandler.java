package com.ileauxfraises.dokuwiki.sync;

import android.content.Context;

import com.ileauxfraises.dokuwiki.WikiCacheUiOrchestrator;

public class SyncUsecaseHandler {

    protected WikiCacheUiOrchestrator _wikiCacheUiOrchestrator = null;

    public void setWikiManagerCallback(WikiCacheUiOrchestrator wikiCacheUiOrchestrator) {
        _wikiCacheUiOrchestrator = wikiCacheUiOrchestrator;
    }

    public void callPageTextDownloadUsecase(String pagename, Context context, Boolean directDisplay) {
        PageTextDownloader aPageTextDownloader = new PageTextDownloader(context, _wikiCacheUiOrchestrator, directDisplay);
        aPageTextDownloader.retrievePageText(pagename);
    }

    public void callPageListRetrieveUsecase(String namespace, Context context) {
        PageListRetriever aPageListRetriever = new PageListRetriever(context, _wikiCacheUiOrchestrator);
        aPageListRetriever.retrievePageList(namespace);
    }

    public void callPageHtmlDownloadUsecase(String pagename, Context context, Boolean directDisplay) {
        PageHtmlDownloader aPageHtmlDownloader = new PageHtmlDownloader(context, _wikiCacheUiOrchestrator, directDisplay);
        aPageHtmlDownloader.retrievePageHTML(pagename);
    }

    public void callPageTextUploadUsecase(String pagename, String newtext, Context context) {
        PageTextUploader aPageTextUploader = new PageTextUploader(context, _wikiCacheUiOrchestrator);
        aPageTextUploader.uploadPageText(pagename, newtext);
    }

    public void callMultiPageHtmlDownloadUsecase(Context context, String... pages) {
        MultiPageHtmlDownloader aMultiPageHtmlDownloader = new MultiPageHtmlDownloader(context, _wikiCacheUiOrchestrator);
        aMultiPageHtmlDownloader.execute(pages);
    }
}
