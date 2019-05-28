package com.fabienli.dokuwiki.usecase;

import com.fabienli.dokuwiki.db.AppDatabase;
import com.fabienli.dokuwiki.db.Page;
import com.fabienli.dokuwiki.db.PageDao;
import com.fabienli.dokuwiki.db.SyncAction;
import com.fabienli.dokuwiki.db.SyncActionDao;
import com.fabienli.dokuwiki.sync.XmlRpcAdapter;
import com.fabienli.dokuwiki.usecase.callback.PageHtmlRetrieveCallback;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;

public class PageHtmlRetrieveUnitTest {
    /**
     * Test that when a page is in DB, we simply reply with the cached content
     */
    @Test
    public void PageHtmlRetrieve_retrievePageFromDB() {
        // test the basic call is getting into the callback
        final String HTML_CONTENT = "test content";
        AppDatabase appDatabase = mock(AppDatabase.class);
        PageDao pageDao = mock(PageDao.class);
        when(appDatabase.pageDao()).thenReturn(pageDao);
        Page page = new Page();
        page.html = HTML_CONTENT;
        when(pageDao.findByName(any(String.class))).thenReturn(page);

        SyncActionDao syncActionDao = mock(SyncActionDao.class);
        when(appDatabase.syncActionDao()).thenReturn(syncActionDao);
        List<SyncAction> saList = new ArrayList<>();
        when(syncActionDao.getAll()).thenReturn(saList);

        XmlRpcAdapter xmlRpcAdapter = mock(XmlRpcAdapter.class);

        // test the usecase:
        PageHtmlRetrieve aPageHtmlRetrieve = new PageHtmlRetrieve(appDatabase, xmlRpcAdapter);
        String content = aPageHtmlRetrieve.retrievePage("start");
        // check that returned page content is the correct one
        assert(content.compareTo(HTML_CONTENT) == 0);
        // ensure we have retrieved from db
        verify(appDatabase, times(1)).pageDao();
        verify(pageDao, times(1)).findByName(eq("start"));
        // ensure we didn't try to call the server
        // TODO
    }

    /**
     * test that when a page is not in DB (empty html) we try to retrieve it from server
     */
    @Test
    public void PageHtmlRetrieve_retrievePageFromServer_updateHtml() {
        // test the basic call is getting into the callback
        final String HTML_CONTENT = "test content";
        AppDatabase appDatabase = mock(AppDatabase.class);
        PageDao pageDao = mock(PageDao.class);
        when(appDatabase.pageDao()).thenReturn(pageDao);
        Page page = new Page();
        page.html = "";
        when(pageDao.findByName(any(String.class))).thenReturn(page);

        SyncActionDao syncActionDao = mock(SyncActionDao.class);
        when(appDatabase.syncActionDao()).thenReturn(syncActionDao);
        List<SyncAction> saList = new ArrayList<>();
        when(syncActionDao.getAll()).thenReturn(saList);

        XmlRpcAdapter xmlRpcAdapter = mock(XmlRpcAdapter.class);
        ArrayList<String> results = new ArrayList<String>();
        results.add(HTML_CONTENT);
        when(xmlRpcAdapter.callMethod(eq("wiki.getPageHTML"), any(String.class))).thenReturn(results);
        ArrayList<String> resultsInfo = new ArrayList<String>();
        resultsInfo.add("{id=1,version=1234567890}");
        when(xmlRpcAdapter.callMethod(eq("wiki.getPageInfo"), any(String.class))).thenReturn(resultsInfo);

        // test the usecase:
        PageHtmlRetrieve aPageHtmlRetrieve = new PageHtmlRetrieve(appDatabase, xmlRpcAdapter);
        String content = aPageHtmlRetrieve.retrievePage("start");
        // check that returned page content is the correct one
        assert(content.compareTo(HTML_CONTENT) == 0);
        // ensure we called the server
        verify(xmlRpcAdapter, times(1)).callMethod(eq("wiki.getPageHTML"), any(String.class));
        verify(xmlRpcAdapter, times(1)).callMethod(eq("wiki.getPageInfo"), any(String.class));
        // ensure we updated the content in our DB cache
        verify(pageDao, times(1)).updateHtml(any(String.class), any(String.class));
        verify(pageDao, times(1)).updateVersion(any(String.class), eq("1234567890"));
    }


    /**
     * test that when a page is not in DB (empty html) we try to retrieve it from server
     */
    @Test
    public void PageHtmlRetrieve_retrievePageFromServer_updateSyncAction() {
        // test the basic call is getting into the callback
        final String HTML_CONTENT = "test content";
        AppDatabase appDatabase = mock(AppDatabase.class);
        PageDao pageDao = mock(PageDao.class);
        when(appDatabase.pageDao()).thenReturn(pageDao);
        Page page = new Page();
        page.html = "";
        when(pageDao.findByName(any(String.class))).thenReturn(page);

        SyncActionDao syncActionDao = mock(SyncActionDao.class);
        when(appDatabase.syncActionDao()).thenReturn(syncActionDao);
        List<SyncAction> saList = new ArrayList<>();
        SyncAction syncAction = new SyncAction();
        syncAction.verb = "GET";
        syncAction.name = "start";
        syncAction.rev = "9123456780";
        saList.add(syncAction);
        when(syncActionDao.getAll()).thenReturn(saList);

        XmlRpcAdapter xmlRpcAdapter = mock(XmlRpcAdapter.class);
        ArrayList<String> results = new ArrayList<String>();
        results.add(HTML_CONTENT);
        when(xmlRpcAdapter.callMethod(eq("wiki.getPageHTML"), any(String.class))).thenReturn(results);

        // test the usecase:
        PageHtmlRetrieve aPageHtmlRetrieve = new PageHtmlRetrieve(appDatabase, xmlRpcAdapter);
        String content = aPageHtmlRetrieve.retrievePage("start");
        // check that returned page content is the correct one
        assert(content.compareTo(HTML_CONTENT) == 0);
        // ensure we called the server
        verify(xmlRpcAdapter, times(1)).callMethod(eq("wiki.getPageHTML"), any(String.class));
        // ensure we didn't retrieve the useless info version
        verify(xmlRpcAdapter, times(0)).callMethod(eq("wiki.getPageInfo"), any(String.class));
        // ensure we updated the content in our DB cache
        verify(pageDao, times(1)).updateHtml(any(String.class), any(String.class));
        verify(pageDao, times(1)).updateVersion(any(String.class), eq("9123456780"));
    }


    /**
     * test that when a page is not in DB (not found) we try to retrieve it from server
     */
    @Test
    public void PageHtmlRetrieve_retrievePageFromServer_new() {
        // test the basic call is getting into the callback
        final String HTML_CONTENT = "test content";
        AppDatabase appDatabase = mock(AppDatabase.class);
        PageDao pageDao = mock(PageDao.class);
        when(appDatabase.pageDao()).thenReturn(pageDao);
        when(pageDao.findByName(any(String.class))).thenReturn(null);

        XmlRpcAdapter xmlRpcAdapter = mock(XmlRpcAdapter.class);
        ArrayList<String> results = new ArrayList<String>();
        results.add(HTML_CONTENT);
        when(xmlRpcAdapter.callMethod(any(String.class), any(String.class))).thenReturn(results);

        // test the usecase:
        PageHtmlRetrieve aPageHtmlRetrieve = new PageHtmlRetrieve(appDatabase, xmlRpcAdapter);
        String content = aPageHtmlRetrieve.retrievePage("start");
        // check that returned page content is the correct one
        assert(content.compareTo(HTML_CONTENT) == 0);
        // ensure we called the server
        verify(xmlRpcAdapter, times(1)).callMethod(eq("wiki.getPageHTML"), any(String.class));
        verify(xmlRpcAdapter, times(1)).callMethod(eq("wiki.getPageInfo"), any(String.class));
        // ensure we updated the content in our DB cache
        verify(pageDao, times(1)).insertAll(any(Page.class));
    }

}
