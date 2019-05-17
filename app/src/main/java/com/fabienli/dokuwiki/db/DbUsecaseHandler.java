package com.fabienli.dokuwiki.db;



import com.fabienli.dokuwiki.WikiCacheUiOrchestrator;

public class DbUsecaseHandler {
    protected WikiCacheUiOrchestrator _wikiCacheUiOrchestrator = null;
    public void setWikiManagerCallback(WikiCacheUiOrchestrator wikiCacheUiOrchestrator) {
        _wikiCacheUiOrchestrator = wikiCacheUiOrchestrator;
    }

    public void callPageReadAllUsecase(AppDatabase db){
        PageReadAll aPageReadAll = new PageReadAll(db, _wikiCacheUiOrchestrator);
        aPageReadAll.execute();
    }

    public void callPageUpdateHtmlUsecase(AppDatabase db, String pagename, String html, String version){
        PageUpdateHtml aPageUpdateHtml = new PageUpdateHtml(db, pagename, html, version);
        aPageUpdateHtml.execute();
    }

    public void callPageUpdateTextUsecase(AppDatabase db, String pagename, String html){
        PageUpdateText aPageUpdateText = new PageUpdateText(db, pagename, html);
        aPageUpdateText.execute();
    }

    public void callPageAddIfMissingUsecase(AppDatabase db, Page page){
        PageAddIfMissing aPageAddIfMissing = new PageAddIfMissing(db, page);
        aPageAddIfMissing.execute();
    }

    public void callMediaAddOrUpdateUsecase(AppDatabase db, Media media) {
        MediaAddOrUpdate aMediaAddOrUpdate = new MediaAddOrUpdate(db, media);
        aMediaAddOrUpdate.execute();
    }

    public void callSyncActionRetrieveUsecase(AppDatabase db, SyncActionListInterface syncActionListInterface){
        SyncActionRetriever aSyncActionRetriever = new SyncActionRetriever(db, syncActionListInterface);
        aSyncActionRetriever.execute();
    }

    public void callSyncActionInsertUsecase(AppDatabase db, SyncAction syncAction, DbCallbackInterface dbCallbackInterface){
        SyncActionHandler aSyncActionHandler = new SyncActionHandler(db, dbCallbackInterface);
        aSyncActionHandler.insert(syncAction);
    }

    public void callSyncActionDeleteUsecase(AppDatabase db, SyncAction syncAction, DbCallbackInterface dbCallbackInterface){
        SyncActionHandler aSyncActionHandler = new SyncActionHandler(db, dbCallbackInterface);
        aSyncActionHandler.delete(syncAction);
    }
}
