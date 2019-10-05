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

public class StaticMediaManagerRemoveUT {

    /**
     * Test the delete step 1
     */
    @Test
    public void StaticPagesDisplay_move1(){
        AppDatabase appDatabase = mock(AppDatabase.class);
        // mock DB with 1 media
        MediaDao mediaDao = mock(MediaDao.class);
        when(appDatabase.mediaDao()).thenReturn(mediaDao);
        List<Media> mediaList = new ArrayList<Media>();
        Media media = new Media();
        media.id = "path:file.png";
        mediaList.add(media);
        when(mediaDao.getAll()).thenReturn(mediaList);

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

        // usecase call
        StaticMediaManagerDisplay staticPagesDisplay = new StaticMediaManagerDisplay(appDatabase, "/");
        staticPagesDisplay.setMediaManagerParams("folder=path&action=startremove&media=path%3Afile.png");
        String resultsPage = staticPagesDisplay.getMediaPageHtml();

        // checks
        //System.out.println(resultsPage);
        // the result page should reference the 2 namespaces we set above
        String pattern = ".*path:file.png.*YES, DELETE !.*cancel.*";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(resultsPage);
        assert(m.find());
    }

    /**
     * Test the delete step 2
     */
    @Test
    public void StaticPagesDisplay_move2(){
        AppDatabase appDatabase = mock(AppDatabase.class);
        // mock DB with 2 media
        MediaDao mediaDao = mock(MediaDao.class);
        when(appDatabase.mediaDao()).thenReturn(mediaDao);
        List<Media> mediaList = new ArrayList<Media>();
        Media media = new Media();
        media.id = "path:file.png";
        mediaList.add(media);
        Media media2 = new Media();
        media2.id = "path:otherfile.png";
        mediaList.add(media2);
        when(mediaDao.getAll()).thenReturn(mediaList);

        // mock sync action to check what will be added
        final boolean[] isSyncActionCalled = {false, false};
        SyncActionDao syncActionDao = mock(SyncActionDao.class);
        when(appDatabase.syncActionDao()).thenReturn(syncActionDao);
        doAnswer(new Answer<Void>(){
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                SyncAction sa = (SyncAction) args[0];
                if(sa.verb.compareTo("DEL")==0
                        && sa.priority.compareTo("1")==0
                        && sa.name.compareTo("path:file.png")==0) { // wanted
                    isSyncActionCalled[0] = true;
                }
                else if(sa.verb.compareTo("DEL")==0
                        && sa.name.compareTo("path:otherfile.png")==0) { // not wanted
                    isSyncActionCalled[1] = true;
                }
                return null;
            }
        }).when(syncActionDao).insertAll(any(SyncAction.class));

        // usecase call
        StaticMediaManagerDisplay staticPagesDisplay = new StaticMediaManagerDisplay(appDatabase, "/");
        staticPagesDisplay.setMediaManagerParams("folder=path&action=remove&media=path%3Afile.png");
        String resultsPage = staticPagesDisplay.getMediaPageHtml();

        // checks
        //System.out.println(resultsPage);
        //Check that we display the list of files in same folder:
        // - one image displayed: path:otherfile.png
        // in real app, there should not be present path:file.png anymore, but here the db is not updated and mocked
        String pattern = ".*<input type=\"hidden\" id=\"media\" name=\"media\" value=\"path:otherfile.png\"/>.*";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(resultsPage);
        assert(m.find());

        verify(appDatabase, times(2)).syncActionDao(); // 1 insert, and 1 delete to ensure uniqueness of insert
        verify(syncActionDao, times(1)).insertAll(any());
        assert(isSyncActionCalled[0]); // Delete file
        assert(!isSyncActionCalled[1]); // don't delete the other file
    }

}
