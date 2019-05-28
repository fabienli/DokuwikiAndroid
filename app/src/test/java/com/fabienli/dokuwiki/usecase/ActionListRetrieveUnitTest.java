package com.fabienli.dokuwiki.usecase;

import com.fabienli.dokuwiki.db.AppDatabase;
import com.fabienli.dokuwiki.db.Page;
import com.fabienli.dokuwiki.db.PageDao;
import com.fabienli.dokuwiki.db.SyncAction;
import com.fabienli.dokuwiki.db.SyncActionDao;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ActionListRetrieveUnitTest {

    /**
     * Test that an empty action list is en empty html page
     */
    @Test
    public void ActionListRetrieve_emptylist(){
        // expected result
        final String HTML_CONTENT = "<table>\n<tr><td>Prio 0: </td><td>0</td></tr>\n<tr><td>Prio 1: </td><td>0</td></tr>\n<tr><td>Prio 2: </td><td>0</td></tr>\n<tr><td>Prio 3: </td><td>0</td></tr>\n</table>\n<ul>\n</ul>";

        // init the mocks
        AppDatabase appDatabase = mock(AppDatabase.class);
        SyncActionDao syncActionDao = mock(SyncActionDao.class);
        when(appDatabase.syncActionDao()).thenReturn(syncActionDao);
        List<SyncAction> syncActionsList = new ArrayList<>();
        when(syncActionDao.getAll()).thenReturn(syncActionsList);

        // usecase call
        ActionListRetrieve actionListRetrieve = new ActionListRetrieve(appDatabase);
        String content = actionListRetrieve.getSyncActionList();

        // check the results
        //System.out.println(content);
        assert(content.compareTo(HTML_CONTENT) == 0);
    }

    /**
     * Test that a one-action list is en one item html page
     */
    @Test
    public void ActionListRetrieve_oneactionlist(){
        // expected result
        final String HTML_CONTENT = "<table>\n<tr><td>Prio 0: </td><td>0</td></tr>\n<tr><td>Prio 1: </td><td>1</td></tr>\n<tr><td>Prio 2: </td><td>0</td></tr>\n<tr><td>Prio 3: </td><td>0</td></tr>\n</table>\n<ul>\n<li>1 - GET : ns:page1 (1234)</li>\n</ul>";

        // init the mocks
        AppDatabase appDatabase = mock(AppDatabase.class);
        SyncActionDao syncActionDao = mock(SyncActionDao.class);
        when(appDatabase.syncActionDao()).thenReturn(syncActionDao);
        SyncAction syncAction = new SyncAction();
        syncAction.rev = "1234";
        syncAction.name = "ns:page1";
        syncAction.verb = "GET";
        syncAction.priority = "1";
        List<SyncAction> syncActionsList = new ArrayList<>();
        syncActionsList.add(syncAction);
        when(syncActionDao.getAll()).thenReturn(syncActionsList);

        // usecase call
        ActionListRetrieve actionListRetrieve = new ActionListRetrieve(appDatabase);
        String content = actionListRetrieve.getSyncActionList();

        //check the results
        //System.out.println(content);
        assert(content.compareTo(HTML_CONTENT) == 0);
    }

    /**
     * Test that a 3-actions list is correct html page with 3 priorities
     */
    @Test
    public void ActionListRetrieve_treeactionlist(){
        // expected result
        final String HTML_CONTENT = "<table>\n<tr><td>Prio 0: </td><td>1</td></tr>\n<tr><td>Prio 1: </td><td>0</td></tr>\n<tr><td>Prio 2: </td><td>1</td></tr>\n<tr><td>Prio 3: </td><td>1</td></tr>\n</table>\n<ul>\n<li>2 - GET : ns:page2 (123598)</li>\n<li>0 - PUT : ns:ns:my_page (3849043726)</li>\n<li>3 - GET : ns:page:image.jpg (346984)</li>\n</ul>";

        // init the mocks
        AppDatabase appDatabase = mock(AppDatabase.class);
        SyncActionDao syncActionDao = mock(SyncActionDao.class);
        when(appDatabase.syncActionDao()).thenReturn(syncActionDao);
        SyncAction syncAction1 = new SyncAction();
        syncAction1.rev = "123598";
        syncAction1.name = "ns:page2";
        syncAction1.verb = "GET";
        syncAction1.priority = "2";
        SyncAction syncAction2 = new SyncAction();
        syncAction2.rev = "3849043726";
        syncAction2.name = "ns:ns:my_page";
        syncAction2.verb = "PUT";
        syncAction2.priority = "0";
        SyncAction syncAction3 = new SyncAction();
        syncAction3.rev = "346984";
        syncAction3.name = "ns:page:image.jpg";
        syncAction3.verb = "GET";
        syncAction3.priority = "3";
        List<SyncAction> syncActionsList = new ArrayList<>();
        syncActionsList.add(syncAction1);
        syncActionsList.add(syncAction2);
        syncActionsList.add(syncAction3);
        when(syncActionDao.getAll()).thenReturn(syncActionsList);

        // usecase call
        ActionListRetrieve actionListRetrieve = new ActionListRetrieve(appDatabase);
        String content = actionListRetrieve.getSyncActionList();

        //check the results
        //System.out.println(content);
        assert(content.compareTo(HTML_CONTENT) == 0);
    }
}
