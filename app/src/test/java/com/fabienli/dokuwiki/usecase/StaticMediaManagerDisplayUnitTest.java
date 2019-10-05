package com.fabienli.dokuwiki.usecase;

import com.fabienli.dokuwiki.db.AppDatabase;
import com.fabienli.dokuwiki.db.Media;
import com.fabienli.dokuwiki.db.MediaDao;
import com.fabienli.dokuwiki.db.Page;
import com.fabienli.dokuwiki.db.PageDao;
import com.fabienli.dokuwiki.db.SyncAction;
import com.fabienli.dokuwiki.db.SyncActionDao;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StaticMediaManagerDisplayUnitTest {
    /**
     * Test the default empty page
     */
    @Test
    public void StaticPagesDisplay_getMediaPageHtml(){
        AppDatabase appDatabase = mock(AppDatabase.class);
        MediaDao mediaDao = mock(MediaDao.class);
        when(appDatabase.mediaDao()).thenReturn(mediaDao);
        StaticMediaManagerDisplay staticPagesDisplay = new StaticMediaManagerDisplay(appDatabase, "/");

        String resultsPage = staticPagesDisplay.getMediaPageHtml();
        //System.out.println(resultsPage);
        assert(resultsPage.compareTo("<table></table><table style=\"width:100%\"></table>")==0);
    }


    /**
     * Test the default empty page
     */
    @Test
    public void StaticPagesDisplay_getMediaPageHtml_back(){
        AppDatabase appDatabase = mock(AppDatabase.class);
        MediaDao mediaDao = mock(MediaDao.class);
        when(appDatabase.mediaDao()).thenReturn(mediaDao);
        StaticMediaManagerDisplay staticPagesDisplay = new StaticMediaManagerDisplay(appDatabase, "/");
        staticPagesDisplay.setSubfolder("/path/to/folder");
        String resultsPage = staticPagesDisplay.getMediaPageHtml();

        // check that we have the back button:
        //System.out.println(resultsPage);
        String pattern = ".*<form action=\"http://dokuwiki_media_manager/\" method=\"GET\"><input type=\"hidden\" id=\"folder\" name=\"folder\" value=\"/path/to\"/><br/><input type=\"submit\" value=\"< back\"/></form>.*";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(resultsPage);
        assert(m.find());

    }

}
