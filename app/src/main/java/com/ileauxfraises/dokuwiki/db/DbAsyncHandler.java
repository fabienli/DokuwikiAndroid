package com.ileauxfraises.dokuwiki.db;



import com.ileauxfraises.dokuwiki.WikiCacheUiOrchestrator;

public class DbAsyncHandler {
    protected WikiCacheUiOrchestrator _wikiCacheUiOrchestrator = null;
    public void setWikiManagerCallback(WikiCacheUiOrchestrator wikiCacheUiOrchestrator) {
        _wikiCacheUiOrchestrator = wikiCacheUiOrchestrator;
    }

    public PageReadAll getPageReadAll(AppDatabase db){
        return new PageReadAll(db, _wikiCacheUiOrchestrator);
    }

    public PageUpdateHtml getPageUpdateHtml(AppDatabase db, String pagename, String html){
        return new PageUpdateHtml(db, pagename, html);
    }

    public PageUpdateText getPageUpdateText(AppDatabase db, String pagename, String html){
        return new PageUpdateText(db, pagename, html);
    }

    public PageAddIfMissing getPageAddIfMissing(AppDatabase db, Page page){
        return new PageAddIfMissing(db, page);
    }
}
