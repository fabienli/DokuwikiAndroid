package com.fabienli.dokuwiki.usecase;

import com.fabienli.dokuwiki.db.AppDatabase;
import com.fabienli.dokuwiki.db.MediaDao;
import com.fabienli.dokuwiki.db.Page;
import com.fabienli.dokuwiki.db.PageDao;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StaticPagesDisplayUnitTest {
    /**
     * Test the getKnownNamespaces
     */
    @Test
    public void StaticPagesDisplay_getKnownNamespaces(){
        AppDatabase appDatabase = mock(AppDatabase.class);
        MediaDao mediaDao = mock(MediaDao.class);
        when(appDatabase.mediaDao()).thenReturn(mediaDao);

        // mock DB with pages
        PageDao pageDao = mock(PageDao.class);
        when(appDatabase.pageDao()).thenReturn(pageDao);
        List<Page> pageList= new ArrayList<Page>();
        Page p1 = new Page();
        p1.pagename = "ns1:start";
        pageList.add(p1);
        Page p2 = new Page();
        p2.pagename = "multiple:ns2:start";
        pageList.add(p2);
        when(pageDao.getAll()).thenReturn(pageList);

        StaticPagesDisplay staticPagesDisplay = new StaticPagesDisplay(appDatabase, "/");

        SortedSet<String> results = staticPagesDisplay.getKnownNamespaces();
        assert(results.size()==2);
        //System.out.println(results.first());
        assert(results.contains("ns1:"));
        assert(results.contains("multiple:ns2:"));
    }


}
