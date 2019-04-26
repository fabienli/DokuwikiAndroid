package com.ileauxfraises.dokuwiki;

import android.content.Context;

import com.ileauxfraises.dokuwiki.cache.WikiPage;
import com.ileauxfraises.dokuwiki.db.AppDatabase;
import com.ileauxfraises.dokuwiki.db.DbAsyncHandler;
import com.ileauxfraises.dokuwiki.db.PageReadAll;
import com.ileauxfraises.dokuwiki.db.PageUpdateHtml;
import com.ileauxfraises.dokuwiki.sync.PageListRetriever;
import com.ileauxfraises.dokuwiki.sync.SyncAsyncHandler;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class WikiCacheUiOrchestratorUnitTest {
    @Test
    public void WikiManager_instance() {
        // mock initialisation
        Context mockedContext = mock(Context.class);
        when(mockedContext.getApplicationContext()).thenReturn(mockedContext);
        DbAsyncHandler aDbAsyncHandler = mock(DbAsyncHandler.class);
        SyncAsyncHandler aSyncAsyncHandler = mock(SyncAsyncHandler.class);
        PageReadAll aPageReadAll = mock(PageReadAll.class);
        when(aDbAsyncHandler.getPageReadAll(any(AppDatabase.class))).thenReturn(aPageReadAll);

        // main call to test
        WikiCacheUiOrchestrator aWikiCacheUiOrchestrator = WikiCacheUiOrchestrator.instance(mockedContext, aDbAsyncHandler, aSyncAsyncHandler);

        // verification
        verify(aDbAsyncHandler, times(1)).setWikiManagerCallback(aWikiCacheUiOrchestrator);
        verify(aSyncAsyncHandler, times(1)).setWikiManagerCallback(aWikiCacheUiOrchestrator);
        verify(aPageReadAll, times(1)).execute();

    }

    @Test
    public void WikiManager_updatePageListFromServer() {
        // mock initialisation
        Context mockedContext = mock(Context.class);
        when(mockedContext.getApplicationContext()).thenReturn(mockedContext);
        DbAsyncHandler aDbAsyncHandler = mock(DbAsyncHandler.class);
        SyncAsyncHandler aSyncAsyncHandler = mock(SyncAsyncHandler.class);
        PageReadAll aPageReadAll = mock(PageReadAll.class);
        when(aDbAsyncHandler.getPageReadAll(any(AppDatabase.class))).thenReturn(aPageReadAll);
        PageListRetriever aPageListRetriever = mock(PageListRetriever.class);
        when(aSyncAsyncHandler.getPageListRetriever(any(Context.class))).thenReturn(aPageListRetriever);

        // main call to test
        WikiCacheUiOrchestrator aWikiCacheUiOrchestrator = WikiCacheUiOrchestrator.instance(mockedContext, aDbAsyncHandler, aSyncAsyncHandler);
        aWikiCacheUiOrchestrator.updatePageListFromServer();

        // verification
        verify(aSyncAsyncHandler, times(1)).getPageListRetriever(any(Context.class));
        verify(aPageListRetriever, times(1)).retrievePageList("");

    }

    @Test
    public void updatePageInCache_sendHtmlToDB() {
        final String PAGENAME = "pagename";
        final String HTMLCONTENT = "<html>content</html>";
        final String HTMLCONTENT2 = "<html>content2</html>";

        // mock initialisation
        Context mockedContext = mock(Context.class);
        when(mockedContext.getApplicationContext()).thenReturn(mockedContext);
        DbAsyncHandler aDbAsyncHandler = mock(DbAsyncHandler.class);
        SyncAsyncHandler aSyncAsyncHandler = mock(SyncAsyncHandler.class);
        PageReadAll aPageReadAll = mock(PageReadAll.class);
        when(aDbAsyncHandler.getPageReadAll(any(AppDatabase.class))).thenReturn(aPageReadAll);
        PageListRetriever aPageListRetriever = mock(PageListRetriever.class);
        when(aSyncAsyncHandler.getPageListRetriever(any(Context.class))).thenReturn(aPageListRetriever);
        PageUpdateHtml aPageUpdateHtml = mock(PageUpdateHtml.class);
        when(aDbAsyncHandler.getPageUpdateHtml(any(AppDatabase.class),eq(PAGENAME),any(String.class))).thenReturn(aPageUpdateHtml);

        // main call to test
        WikiCacheUiOrchestrator aWikiCacheUiOrchestrator = WikiCacheUiOrchestrator.instance(mockedContext, aDbAsyncHandler, aSyncAsyncHandler);
        aWikiCacheUiOrchestrator.updatePageInCache(PAGENAME, HTMLCONTENT);

        // check the results
        verify(aPageUpdateHtml, times(1)).execute();
        assert(aWikiCacheUiOrchestrator._wikiPageList._pages.size() == 1);
        assert(aWikiCacheUiOrchestrator._wikiPageList._pages.containsKey(PAGENAME));
        WikiPage newpage = aWikiCacheUiOrchestrator._wikiPageList._pages.get(PAGENAME);
        assert(newpage._html.compareTo(HTMLCONTENT) == 0);

        // update the content and check it's updated
        aWikiCacheUiOrchestrator.updatePageInCache(PAGENAME, HTMLCONTENT2);
        verify(aPageUpdateHtml, times(2)).execute();
        assert(aWikiCacheUiOrchestrator._wikiPageList._pages.size() == 1);
        assert(aWikiCacheUiOrchestrator._wikiPageList._pages.containsKey(PAGENAME));
        newpage = aWikiCacheUiOrchestrator._wikiPageList._pages.get(PAGENAME);
        assert(newpage._html.compareTo(HTMLCONTENT2) == 0);
    }
}