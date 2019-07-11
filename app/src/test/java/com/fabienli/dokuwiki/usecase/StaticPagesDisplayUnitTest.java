package com.fabienli.dokuwiki.usecase;

import com.fabienli.dokuwiki.db.AppDatabase;
import com.fabienli.dokuwiki.db.MediaDao;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StaticPagesDisplayUnitTest {
    /**
     * Test the default empty page
     */
    @Test
    public void StaticPagesDisplay_getMediaPageHtml(){
        AppDatabase appDatabase = mock(AppDatabase.class);
        MediaDao mediaDao = mock(MediaDao.class);
        when(appDatabase.mediaDao()).thenReturn(mediaDao);
        StaticPagesDisplay staticPagesDisplay = new StaticPagesDisplay(appDatabase, "/");

        String resultsPage = staticPagesDisplay.getMediaPageHtml();
        //System.out.println(resultsPage);
        assert(resultsPage.compareTo("<table></table><table></table>")==0);
    }


    /**
     * Test the default empty page
     */
    @Test
    public void StaticPagesDisplay_getMediaPageHtml_back(){
        AppDatabase appDatabase = mock(AppDatabase.class);
        MediaDao mediaDao = mock(MediaDao.class);
        when(appDatabase.mediaDao()).thenReturn(mediaDao);
        StaticPagesDisplay staticPagesDisplay = new StaticPagesDisplay(appDatabase, "/");
        staticPagesDisplay.setSubfolder("/path/to/folder");
        String resultsPage = staticPagesDisplay.getMediaPageHtml();
        //System.out.println(resultsPage);
        assert(resultsPage.compareTo("<form action=\"http://dokuwiki_media_manager/\" method=\"GET\"><input type=\"hidden\" id=\"folder\" name=\"folder\" value=\"/path/to\"/><br/><input type=\"submit\" value=\"back\"/></form><table></table><table></table>")==0);
    }

}
