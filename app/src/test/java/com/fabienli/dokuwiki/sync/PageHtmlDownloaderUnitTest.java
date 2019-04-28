package com.fabienli.dokuwiki.sync;

import android.content.Context;

import com.fabienli.dokuwiki.WikiCacheUiOrchestrator;

import org.junit.Test;

import java.util.ArrayList;

import static org.mockito.Mockito.*;

public class PageHtmlDownloaderUnitTest {

    @Test
    public void onPostExecute_saveCache() {
        final String PAGENAME = "my:page";
        final String PAGECONTENT1 = "this is the page content";
        final String PAGECONTENT2 = "this is a second page content";

        // mock initialisation
        Context mockedContext = mock(Context.class);
        when(mockedContext.getApplicationContext()).thenReturn(mockedContext);
        WikiCacheUiOrchestrator aWikiCacheUiOrchestrator = mock(WikiCacheUiOrchestrator.class);
        SyncUsecaseHandler aSyncUsecaseHandler = mock(SyncUsecaseHandler.class);

        // class to be tested
        PageHtmlDownloader aPageHtmlDownloader = new PageHtmlDownloader(mockedContext, aWikiCacheUiOrchestrator, false);
        aPageHtmlDownloader._pagename = PAGENAME;
        ArrayList<String> results = new ArrayList<>();
        results.add(PAGECONTENT1);
        aPageHtmlDownloader.processXmlRpcResult(results);

        // check that one line result was processed
        verify(aWikiCacheUiOrchestrator, times(1)).updatePageInCache(PAGENAME, PAGECONTENT1);

        // test again with 2 lines
        reset(aWikiCacheUiOrchestrator);
        results.add(PAGECONTENT2);
        aPageHtmlDownloader.processXmlRpcResult(results);

        // check that the cache was not updated
        verify(aWikiCacheUiOrchestrator, never()).updatePageInCache(any(String.class), any(String.class));
    }
}
