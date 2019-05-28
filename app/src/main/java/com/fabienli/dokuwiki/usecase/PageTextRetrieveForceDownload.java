package com.fabienli.dokuwiki.usecase;

import android.os.AsyncTask;

import com.fabienli.dokuwiki.WikiCacheUiOrchestrator;
import com.fabienli.dokuwiki.db.AppDatabase;
import com.fabienli.dokuwiki.db.Page;
import com.fabienli.dokuwiki.db.PageUpdateText;
import com.fabienli.dokuwiki.db.SyncAction;
import com.fabienli.dokuwiki.sync.PageTextDownUpLoader;
import com.fabienli.dokuwiki.sync.XmlRpcAdapter;
import com.fabienli.dokuwiki.usecase.callback.PageHtmlRetrieveCallback;

import java.util.List;

public class PageTextRetrieveForceDownload extends PageTextRetrieve {

    public PageTextRetrieveForceDownload(AppDatabase db, XmlRpcAdapter xmlRpcAdapter) {
        super(db, xmlRpcAdapter);
    }

    @Override
    public String retrievePage(String pagename) {
        String pageContent = "";

        WikiCacheUiOrchestrator.instance()._logs.add("page "+pagename+" forced retrieved from server" );
        PageTextDownUpLoader pageTextDownUpLoader = new PageTextDownUpLoader(_xmlRpcAdapter);
        pageContent = pageTextDownUpLoader.retrievePageText(pagename);

        // store it in DB cache
        PageUpdateText pageUpdateText = new PageUpdateText(_db, pagename, pageContent);
        pageUpdateText.doSync();

        return pageContent;
    }
}
