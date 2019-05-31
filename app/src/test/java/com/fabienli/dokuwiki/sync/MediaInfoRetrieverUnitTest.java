package com.fabienli.dokuwiki.sync;

import com.fabienli.dokuwiki.db.Media;

import org.junit.Test;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MediaInfoRetrieverUnitTest {
    /**
     * Test with null value returned
     */
    @Test
    public void retrieveMediaInfo_nullResult(){
        XmlRpcAdapter xmlRpcAdapter = mock(XmlRpcAdapter.class);
        MediaInfoRetriever mediaInfoRetriever = new MediaInfoRetriever(xmlRpcAdapter);
        when(xmlRpcAdapter.callMethod(any(String.class),any(String.class))).thenReturn(null);

        // call usecase
        Media media = mediaInfoRetriever.retrieveMediaInfo("whatever");

        // check results
        assert(media.id.compareTo("whatever") == 0);
        assert(media.size == null);
        assert(media.lastModified == null);
    }
    /**
     * Test with empty array value returned
     */
    @Test
    public void retrieveMediaInfo_noResult(){
        XmlRpcAdapter xmlRpcAdapter = mock(XmlRpcAdapter.class);
        MediaInfoRetriever mediaInfoRetriever = new MediaInfoRetriever(xmlRpcAdapter);
        ArrayList<String> resultArray = new ArrayList<String>();
        when(xmlRpcAdapter.callMethod(any(String.class),any(String.class))).thenReturn(resultArray);

        // call usecase
        Media media = mediaInfoRetriever.retrieveMediaInfo("whatever");

        // check results
        assert(media.id.compareTo("whatever") == 0);
        assert(media.size == null);
        assert(media.lastModified == null);
    }
    /**
     * Test with simple value returned
     */
    @Test
    public void retrieveMediaInfo_oneResult(){
        XmlRpcAdapter xmlRpcAdapter = mock(XmlRpcAdapter.class);
        MediaInfoRetriever mediaInfoRetriever = new MediaInfoRetriever(xmlRpcAdapter);
        ArrayList<String> resultArray = new ArrayList<String>();
        resultArray.add("{size=123, lastModified=20190513 12:00:00}");
        when(xmlRpcAdapter.callMethod(any(String.class),any(String.class))).thenReturn(resultArray);

        // call usecase
        Media media = mediaInfoRetriever.retrieveMediaInfo("whatever");

        // check results
        assert(media.id.compareTo("whatever") == 0);
        assert(media.size.compareTo("123") == 0);
        assert(media.lastModified.compareTo("20190513 12:00:00") == 0);
    }
}
