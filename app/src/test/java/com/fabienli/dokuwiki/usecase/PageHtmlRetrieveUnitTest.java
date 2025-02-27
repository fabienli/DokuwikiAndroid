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
        when(xmlRpcAdapter.useOldApi()).thenReturn(false);

        ArrayList<String> results = new ArrayList<String>();
        results.add(HTML_CONTENT);
        when(xmlRpcAdapter.callMethod(eq("core.getPageHTML"), any(String.class))).thenReturn(results);
        ArrayList<String> resultsInfo = new ArrayList<String>();
        resultsInfo.add("{id=1,revision=1234567890}");
        when(xmlRpcAdapter.callMethod(eq("core.getPageInfo"), any(String.class))).thenReturn(resultsInfo);

        // test the usecase:
        PageHtmlRetrieve aPageHtmlRetrieve = new PageHtmlRetrieve(appDatabase, xmlRpcAdapter);
        String content = aPageHtmlRetrieve.retrievePage("start");
        // check that returned page content is the correct one
        assert(content.compareTo(HTML_CONTENT) == 0);
        // ensure we called the server
        verify(xmlRpcAdapter, times(1)).callMethod(eq("core.getPageHTML"), any(String.class));
        verify(xmlRpcAdapter, times(1)).callMethod(eq("core.getPageInfo"), any(String.class));
        // ensure we updated the content in our DB cache
        verify(pageDao, times(1)).updateHtml(any(String.class), any(String.class));
        verify(pageDao, times(1)).updateVersion(any(String.class), eq("1234567890"));
    }


    /**
     * test that when a page is not in DB (empty html) we try to retrieve it from server
     */
    @Test
    public void PageHtmlRetrieve_retrievePageFromServer_updateHtml_Deprecated() {
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
        when(xmlRpcAdapter.useOldApi()).thenReturn(true);

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
        syncAction.priority = SyncAction.LEVEL_GET_FILES;
        saList.add(syncAction);
        when(syncActionDao.getAll()).thenReturn(saList);

        XmlRpcAdapter xmlRpcAdapter = mock(XmlRpcAdapter.class);
        ArrayList<String> results = new ArrayList<String>();
        results.add(HTML_CONTENT);
        when(xmlRpcAdapter.callMethod(eq("core.getPageHTML"), any(String.class))).thenReturn(results);
        when(xmlRpcAdapter.useOldApi()).thenReturn(false);

        // test the usecase:
        PageHtmlRetrieve aPageHtmlRetrieve = new PageHtmlRetrieve(appDatabase, xmlRpcAdapter);
        String content = aPageHtmlRetrieve.retrievePage("start");
        // check that returned page content is the correct one
        assert(content.compareTo(HTML_CONTENT) == 0);
        // ensure we called the server
        verify(xmlRpcAdapter, times(1)).callMethod(eq("core.getPageHTML"), any(String.class));
        // ensure we didn't retrieve the useless info version
        verify(xmlRpcAdapter, times(0)).callMethod(eq("core.getPageInfo"), any(String.class));
        // ensure we updated the content in our DB cache
        verify(pageDao, times(1)).updateHtml(any(String.class), any(String.class));
        verify(pageDao, times(1)).updateVersion(any(String.class), eq("9123456780"));
    }

    /**
     * test that when a page is in DB with a level 5 DYNAMIC we default to local version
     */
    @Test
    public void PageHtmlRetrieve_retrievePageLocal_updateSyncAction() {
        // test the basic call is getting into the callback
        final String HTML_CONTENT = "test content";
        final String HTML_CONTENT_LOCAL = "test content local";
        AppDatabase appDatabase = mock(AppDatabase.class);
        PageDao pageDao = mock(PageDao.class);
        when(appDatabase.pageDao()).thenReturn(pageDao);
        Page page = new Page();
        page.html = HTML_CONTENT_LOCAL;
        when(pageDao.findByName(any(String.class))).thenReturn(page);

        SyncActionDao syncActionDao = mock(SyncActionDao.class);
        when(appDatabase.syncActionDao()).thenReturn(syncActionDao);
        List<SyncAction> saList = new ArrayList<>();
        SyncAction syncAction = new SyncAction();
        syncAction.verb = "GET";
        syncAction.name = "start";
        syncAction.rev = "9123456780";
        syncAction.priority = SyncAction.LEVEL_GET_DYNAMICS;
        saList.add(syncAction);
        when(syncActionDao.getAll()).thenReturn(saList);

        XmlRpcAdapter xmlRpcAdapter = mock(XmlRpcAdapter.class);
        ArrayList<String> results = new ArrayList<String>();
        results.add(HTML_CONTENT);
        when(xmlRpcAdapter.callMethod(eq("core.getPageHTML"), any(String.class))).thenReturn(results);
        when(xmlRpcAdapter.useOldApi()).thenReturn(false);

        // test the usecase:
        PageHtmlRetrieve aPageHtmlRetrieve = new PageHtmlRetrieve(appDatabase, xmlRpcAdapter);
        String content = aPageHtmlRetrieve.retrievePage("start");
        // check that returned page content is the correct one
        assert(content.compareTo(HTML_CONTENT_LOCAL) == 0);
        // ensure we didn't call the server
        verify(xmlRpcAdapter, times(0)).callMethod(eq("core.getPageHTML"), any(String.class));
        // ensure we didn't retrieve the useless info version
        verify(xmlRpcAdapter, times(0)).callMethod(eq("core.getPageInfo"), any(String.class));
        // ensure we didn't touch the content in our DB cache
        verify(pageDao, times(0)).updateHtml(any(String.class), any(String.class));
    }


    /**
     * test that when a page is in DB with a level 5 DYNAMIC we default to local version
     */
    @Test
    public void PageHtmlRetrieve_retrievePageLocal_updateSyncActionDeprecated() {
        // test the basic call is getting into the callback
        final String HTML_CONTENT = "test content";
        final String HTML_CONTENT_LOCAL = "test content local";
        AppDatabase appDatabase = mock(AppDatabase.class);
        PageDao pageDao = mock(PageDao.class);
        when(appDatabase.pageDao()).thenReturn(pageDao);
        Page page = new Page();
        page.html = HTML_CONTENT_LOCAL;
        when(pageDao.findByName(any(String.class))).thenReturn(page);

        SyncActionDao syncActionDao = mock(SyncActionDao.class);
        when(appDatabase.syncActionDao()).thenReturn(syncActionDao);
        List<SyncAction> saList = new ArrayList<>();
        SyncAction syncAction = new SyncAction();
        syncAction.verb = "GET";
        syncAction.name = "start";
        syncAction.rev = "9123456780";
        syncAction.priority = SyncAction.LEVEL_GET_DYNAMICS;
        saList.add(syncAction);
        when(syncActionDao.getAll()).thenReturn(saList);

        XmlRpcAdapter xmlRpcAdapter = mock(XmlRpcAdapter.class);
        ArrayList<String> results = new ArrayList<String>();
        results.add(HTML_CONTENT);
        when(xmlRpcAdapter.callMethod(eq("wiki.getPageHTML"), any(String.class))).thenReturn(results);
        when(xmlRpcAdapter.useOldApi()).thenReturn(true);

        // test the usecase:
        PageHtmlRetrieve aPageHtmlRetrieve = new PageHtmlRetrieve(appDatabase, xmlRpcAdapter);
        String content = aPageHtmlRetrieve.retrievePage("start");
        // check that returned page content is the correct one
        assert(content.compareTo(HTML_CONTENT_LOCAL) == 0);
        // ensure we didn't call the server
        verify(xmlRpcAdapter, times(0)).callMethod(eq("wiki.getPageHTML"), any(String.class));
        // ensure we didn't retrieve the useless info version
        verify(xmlRpcAdapter, times(0)).callMethod(eq("wiki.getPageInfo"), any(String.class));
        // ensure we didn't touch the content in our DB cache
        verify(pageDao, times(0)).updateHtml(any(String.class), any(String.class));
    }

    /**
     * test that when a page is not in DB (not found) we try to retrieve it from server
     */
    @Test
    public void PageHtmlRetrieve_retrievePageFromServer_newInLocal() {
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
        when(xmlRpcAdapter.useOldApi()).thenReturn(false);

        // test the usecase:
        PageHtmlRetrieve aPageHtmlRetrieve = new PageHtmlRetrieve(appDatabase, xmlRpcAdapter);
        String content = aPageHtmlRetrieve.retrievePage("start");
        // check that returned page content is the correct one
        assert(content.compareTo(HTML_CONTENT) == 0);
        // ensure we called the server
        verify(xmlRpcAdapter, times(1)).callMethod(eq("core.getPageHTML"), any(String.class));
        verify(xmlRpcAdapter, times(1)).callMethod(eq("core.getPageInfo"), any(String.class));
        // ensure we updated the content in our DB cache
        verify(pageDao, times(1)).insertAll(any(Page.class));
    }


    /**
     * test that when a page is not in DB (not found) we try to retrieve it from server
     */
    @Test
    public void PageHtmlRetrieve_retrievePageFromServer_newInLocalDeprecated() {
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
        when(xmlRpcAdapter.useOldApi()).thenReturn(true);

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


    /**
     * test that when a page is not in DB (not found), not in server, so we propose to create it
     */
    @Test
    public void PageHtmlRetrieve_retrievePageFromServer_newPage() {
        // test the basic call is getting into the callback
        final String PAGENAME = "start";
        AppDatabase appDatabase = mock(AppDatabase.class);
        PageDao pageDao = mock(PageDao.class);
        when(appDatabase.pageDao()).thenReturn(pageDao);
        when(pageDao.findByName(any(String.class))).thenReturn(null);

        XmlRpcAdapter xmlRpcAdapter = mock(XmlRpcAdapter.class);
        ArrayList<String> results = new ArrayList<String>();
        when(xmlRpcAdapter.callMethod(any(String.class), any(String.class))).thenReturn(results);
        when(xmlRpcAdapter.useOldApi()).thenReturn(false);

        // test the usecase:
        PageHtmlRetrieve aPageHtmlRetrieve = new PageHtmlRetrieve(appDatabase, xmlRpcAdapter);
        String content = aPageHtmlRetrieve.retrievePage(PAGENAME);
        // check that returned page content is the correct one
        //System.out.println(content);
        assert(content.length() > 0);
        assert(content.indexOf(PAGENAME) != -1);
        assert(content.indexOf("create") != -1);
        // ensure we called the server
        verify(xmlRpcAdapter, times(1)).callMethod(eq("core.getPageHTML"), any(String.class));
        // ensure we didn't try to update anything in our DB cache
        verify(pageDao, times(0)).insertAll(any(Page.class));
    }


    /**
     * test that when a page is not in DB (not found), not in server, so we propose to create it
     */
    @Test
    public void PageHtmlRetrieve_retrievePageFromServer_newPageDeprecated() {
        // test the basic call is getting into the callback
        final String PAGENAME = "start";
        AppDatabase appDatabase = mock(AppDatabase.class);
        PageDao pageDao = mock(PageDao.class);
        when(appDatabase.pageDao()).thenReturn(pageDao);
        when(pageDao.findByName(any(String.class))).thenReturn(null);

        XmlRpcAdapter xmlRpcAdapter = mock(XmlRpcAdapter.class);
        ArrayList<String> results = new ArrayList<String>();
        when(xmlRpcAdapter.callMethod(any(String.class), any(String.class))).thenReturn(results);
        when(xmlRpcAdapter.useOldApi()).thenReturn(true);

        // test the usecase:
        PageHtmlRetrieve aPageHtmlRetrieve = new PageHtmlRetrieve(appDatabase, xmlRpcAdapter);
        String content = aPageHtmlRetrieve.retrievePage(PAGENAME);
        // check that returned page content is the correct one
        //System.out.println(content);
        assert(content.length() > 0);
        assert(content.indexOf(PAGENAME) != -1);
        assert(content.indexOf("create") != -1);
        // ensure we called the server
        verify(xmlRpcAdapter, times(1)).callMethod(eq("wiki.getPageHTML"), any(String.class));
        // ensure we didn't try to update anything in our DB cache
        verify(pageDao, times(0)).insertAll(any(Page.class));
    }



}
