package com.fabienli.dokuwiki.usecase;

import com.fabienli.dokuwiki.db.AppDatabase;
import com.fabienli.dokuwiki.db.Page;
import com.fabienli.dokuwiki.db.PageDao;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PageListRetrieveUnitTest {
    /**
     * Test that an empty page list is en empty html page
     */
    @Test
    public void PageListRetrieve_emptylist(){
        // expected result
        final String HTML_CONTENT = "<ul>\n</ul>";

        // init the mocks
        AppDatabase appDatabase = mock(AppDatabase.class);
        PageDao pageDao = mock(PageDao.class);
        when(appDatabase.pageDao()).thenReturn(pageDao);
        List<Page> pageList = new ArrayList<>();
        when(pageDao.getAll()).thenReturn(pageList);

        // usecase call
        PageListRetrieve pageListRetrieve = new PageListRetrieve(appDatabase);
        String content = pageListRetrieve.getPageList();

        // check the results
        assert(content.compareTo(HTML_CONTENT) == 0);
    }

    /**
     * Test that a one-page list is en one item html page
     */
    @Test
    public void PageListRetrieve_onepagelist(){
        // expected result
        final String HTML_CONTENT = "<ul>\n<li><a href=\"http://dokuwiki/doku.php?id=page1\">page1</a> [need sync]</li>\n</ul>";

        // init the mocks
        AppDatabase appDatabase = mock(AppDatabase.class);
        PageDao pageDao = mock(PageDao.class);
        when(appDatabase.pageDao()).thenReturn(pageDao);
        Page page = new Page();
        page.pagename = "page1";
        List<Page> pageList = new ArrayList<>();
        pageList.add(page);
        when(pageDao.getAll()).thenReturn(pageList);

        // usecase call
        PageListRetrieve pageListRetrieve = new PageListRetrieve(appDatabase);
        String content = pageListRetrieve.getPageList();

        //check the results
        System.out.println(content);
        assert(content.compareTo(HTML_CONTENT) == 0);
    }
}
