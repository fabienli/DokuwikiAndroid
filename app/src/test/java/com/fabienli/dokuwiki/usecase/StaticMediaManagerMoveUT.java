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

public class StaticMediaManagerMoveUT {

    /**
     * Test the move step 1
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
        staticPagesDisplay.setMediaManagerParams("folder=wiki&action=startmove&media=path%3Afile.png");
        String resultsPage = staticPagesDisplay.getMediaPageHtml();

        // checks
        //System.out.println(resultsPage);
        // the result page should reference the 2 namespaces we set above
        String pattern = ".*<ul><li onclick=\"chooseNS\\('multiple:ns2:'\\)\">multiple:ns2:</li><li onclick=\"chooseNS\\('ns1:'\\)\">ns1:</li></ul>.*";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(resultsPage);
        assert(m.find());
    }

    /**
     * Test the move step 2
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
        media2.id = "longer:newpath:otherfile.png";
        mediaList.add(media2);
        when(mediaDao.getAll()).thenReturn(mediaList);

        // mock sync action to check what will be added
        final boolean[] isSyncActionCalled = {false, false, false, false};
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
                        && sa.name.compareTo("longer:newpath:file.png")==0) { // not wanted
                    isSyncActionCalled[1] = true;
                }
                else if(sa.verb.compareTo("PUT")==0
                        && sa.priority.compareTo("1")==0
                        && sa.name.compareTo("path:file.png")==0) { // wanted
                    isSyncActionCalled[2] = true;
                }
                else if(sa.verb.compareTo("PUT")==0
                        && sa.name.compareTo("longer:newpath:file.png")==0) { // not wanted
                    isSyncActionCalled[3] = true;
                }
                return null;
            }
        }).when(syncActionDao).insertAll(any(SyncAction.class));

        // usecase call
        StaticMediaManagerDisplay staticPagesDisplay = new StaticMediaManagerDisplay(appDatabase, "/");
        staticPagesDisplay.setMediaManagerParams("folder=wiki&action=move&media=path%3Afile.png&destmove=longer%3Anewpath%3A");
        String resultsPage = staticPagesDisplay.getMediaPageHtml();

        // checks
        //System.out.println(resultsPage);
        //Check that we display the list of files in new folder:
        // - back button folder should be "longer"
        // - one image displayed: longer:newpath:otherfile.png
        // in real app, there should be present longer:newpath:file.png as well, but here the db is not updated and mocked
        String pattern = ".*<input type=\"hidden\" id=\"folder\" name=\"folder\" value=\"longer\"/>.*<input type=\"hidden\" id=\"media\" name=\"media\" value=\"longer:newpath:otherfile.png\"/>.*";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(resultsPage);
        assert(m.find());
        verify(appDatabase, times(4)).syncActionDao(); // 2 insert, and 2 delete to ensure uniqueness of insert
        verify(syncActionDao, times(2)).insertAll(any());
        assert(isSyncActionCalled[0]); // Delete old file
        assert(!isSyncActionCalled[1]);
        assert(isSyncActionCalled[2]); // Put new file
        assert(!isSyncActionCalled[3]);
    }

}
