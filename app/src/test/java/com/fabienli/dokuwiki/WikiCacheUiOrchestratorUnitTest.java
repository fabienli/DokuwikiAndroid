package com.fabienli.dokuwiki;

import android.content.Context;

import com.fabienli.dokuwiki.cache.WikiPage;
import com.fabienli.dokuwiki.db.AppDatabase;
import com.fabienli.dokuwiki.db.DbUsecaseHandler;
import com.fabienli.dokuwiki.sync.SyncUsecaseHandler;

import org.junit.Test;

import static org.mockito.Mockito.*;

/**
 * Testing the WikiCacheUiOrchestrator class
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class WikiCacheUiOrchestratorUnitTest {
    @Test
    public void WikiManager_instance() {
        // mock initialisation
        Context mockedContext = mock(Context.class);
        when(mockedContext.getApplicationContext()).thenReturn(mockedContext);
        DbUsecaseHandler aDbUsecaseHandler = mock(DbUsecaseHandler.class);
        SyncUsecaseHandler aSyncUsecaseHandler = mock(SyncUsecaseHandler.class);

        // main call to test
        WikiCacheUiOrchestrator aWikiCacheUiOrchestrator = WikiCacheUiOrchestrator.instance(mockedContext, aDbUsecaseHandler, aSyncUsecaseHandler);

        // verification
        verify(aDbUsecaseHandler, times(1)).setWikiManagerCallback(aWikiCacheUiOrchestrator);
        verify(aSyncUsecaseHandler, times(1)).setWikiManagerCallback(aWikiCacheUiOrchestrator);

    }

    @Test
    public void WikiManager_updatePageListFromServer() {
        // mock initialisation
        Context mockedContext = mock(Context.class);
        when(mockedContext.getApplicationContext()).thenReturn(mockedContext);
        DbUsecaseHandler aDbUsecaseHandler = mock(DbUsecaseHandler.class);
        SyncUsecaseHandler aSyncUsecaseHandler = mock(SyncUsecaseHandler.class);

        // main call to test
        WikiCacheUiOrchestrator aWikiCacheUiOrchestrator = WikiCacheUiOrchestrator.instance(mockedContext, aDbUsecaseHandler, aSyncUsecaseHandler);
        aWikiCacheUiOrchestrator.updatePageListFromServer();

        // verification
        verify(aSyncUsecaseHandler, times(1)).callPageListRetrieveUsecase(eq(""), any(Context.class));

    }

    @Test
    public void updatePageInCache_sendHtmlToDB() {
        final String PAGENAME = "pagename";
        final String HTMLCONTENT = "<html>content</html>";
        final String HTMLCONTENT2 = "<html>content2</html>";

        // mock initialisation
        Context mockedContext = mock(Context.class);
        when(mockedContext.getApplicationContext()).thenReturn(mockedContext);
        DbUsecaseHandler aDbUsecaseHandler = mock(DbUsecaseHandler.class);
        SyncUsecaseHandler aSyncUsecaseHandler = mock(SyncUsecaseHandler.class);

        // main call to test
        WikiCacheUiOrchestrator aWikiCacheUiOrchestrator = WikiCacheUiOrchestrator.instance(mockedContext, aDbUsecaseHandler, aSyncUsecaseHandler);
        aWikiCacheUiOrchestrator.updatePageInCache(PAGENAME, HTMLCONTENT);

        // check the results
        verify(aDbUsecaseHandler, times(1)).callPageUpdateHtmlUsecase(any(AppDatabase.class),eq(PAGENAME),any(String.class));
        assert(aWikiCacheUiOrchestrator._wikiPageList._pages.size() == 1);
        assert(aWikiCacheUiOrchestrator._wikiPageList._pages.containsKey(PAGENAME));
        WikiPage newpage = aWikiCacheUiOrchestrator._wikiPageList._pages.get(PAGENAME);
        assert(newpage._html.compareTo(HTMLCONTENT) == 0);

        // update the content and check it's updated
        aWikiCacheUiOrchestrator.updatePageInCache(PAGENAME, HTMLCONTENT2);
        verify(aDbUsecaseHandler, times(2)).callPageUpdateHtmlUsecase(any(AppDatabase.class),eq(PAGENAME),any(String.class));
        assert(aWikiCacheUiOrchestrator._wikiPageList._pages.size() == 1);
        assert(aWikiCacheUiOrchestrator._wikiPageList._pages.containsKey(PAGENAME));
        newpage = aWikiCacheUiOrchestrator._wikiPageList._pages.get(PAGENAME);
        assert(newpage._html.compareTo(HTMLCONTENT2) == 0);
    }
}