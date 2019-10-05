package com.fabienli.dokuwiki.usecase;

import com.fabienli.dokuwiki.WikiCacheUiOrchestrator;
import com.fabienli.dokuwiki.db.AppDatabase;
import com.fabienli.dokuwiki.db.Media;
import com.fabienli.dokuwiki.db.MediaDao;
import com.fabienli.dokuwiki.sync.XmlRpcAdapter;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StaticMediaManagerDetailsUT {
    /**
     * Test the default empty page
     */
    @Test
    public void StaticPagesDisplay_getMediaPageHtml(){
        AppDatabase appDatabase = mock(AppDatabase.class);
        // mock DB with 1 media
        MediaDao mediaDao = mock(MediaDao.class);
        when(appDatabase.mediaDao()).thenReturn(mediaDao);
        List<Media> mediaList = new ArrayList<Media>();
        Media media = new Media();
        media.id = "wiki:file.png";
        mediaList.add(media);
        when(mediaDao.getAll()).thenReturn(mediaList);
        // mock for sync attemps
        WikiCacheUiOrchestrator wikiCacheUiOrchestrator = mock(WikiCacheUiOrchestrator.class);
        //when(wikiCacheUiOrchestrator.instance()).thenReturn(wikiCacheUiOrchestrator);

        // usecase call
        StaticMediaManagerDisplay staticPagesDisplay = new StaticMediaManagerDisplay(appDatabase, "/");
        staticPagesDisplay.mediaSynchronizer = wikiCacheUiOrchestrator;
        staticPagesDisplay.setMediaManagerParams("folder=wiki&action=details&media=wiki%3Afile.png");
        String resultsPage = staticPagesDisplay.getMediaPageHtml();

        //System.out.println(resultsPage);
        String pattern = ".*media id:.*wiki:file.png.*";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(resultsPage);
        assert(m.find());

        // ensure we have the correct move action
        pattern = ".*name=\"action\" value=\"startmove\".*";
        r = Pattern.compile(pattern);
        m = r.matcher(resultsPage);
        assert(m.find());

        // ensure we have the correct remove action
        pattern = ".*name=\"action\" value=\"startremove\".*";
        r = Pattern.compile(pattern);
        m = r.matcher(resultsPage);
        assert(m.find());
    }
}
