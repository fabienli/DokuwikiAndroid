package com.fabienli.dokuwiki.sync;

import android.content.Context;

import com.fabienli.dokuwiki.WikiCacheUiOrchestrator;

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

    public void callPageHtmlDownloadUsecase(String pagename, Context context, Boolean directDisplay, SyncUsecaseCallbackInterface iCallback) {
        PageHtmlDownloader aPageHtmlDownloader = new PageHtmlDownloader(context, _wikiCacheUiOrchestrator, directDisplay);
        aPageHtmlDownloader._syncUsecaseCallbackInterface = iCallback;
        aPageHtmlDownloader.retrievePageHTML(pagename);
    }

    public void callPageTextUploadUsecase(String pagename, String newtext, Context context, SyncUsecaseCallbackInterface iCallback) {
        PageTextUploader aPageTextUploader = new PageTextUploader(context);
        aPageTextUploader._syncUsecaseCallbackInterface = iCallback;
        aPageTextUploader.uploadPageText(pagename, newtext);
    }

    public void callMultiPageHtmlDownloadUsecase(Context context, String... pages) {
        MultiPageHtmlDownloader aMultiPageHtmlDownloader = new MultiPageHtmlDownloader(context, _wikiCacheUiOrchestrator);
        aMultiPageHtmlDownloader.execute(pages);
    }

    public void callMediaListRetrieveUsecase(String namespace, Context context) {
        MediaListRetriever aMediaListRetriever = new MediaListRetriever(context, _wikiCacheUiOrchestrator);
        aMediaListRetriever.retrieveMediaList(namespace);
    }

    public void callMediaDownloadUsecase(String mediaId, String mediaLocalPath, Context context, int width, int height, Boolean directDisplay) {
        MediaDownloader aMediaDownloader = new MediaDownloader(context, _wikiCacheUiOrchestrator, width, height, directDisplay);
        aMediaDownloader.downloadMedia(mediaId, mediaLocalPath);
    }
}
