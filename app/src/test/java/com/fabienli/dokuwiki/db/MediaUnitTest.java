package com.fabienli.dokuwiki.db;

import org.junit.Test;

public class MediaUnitTest {


    /**
     * Test default is not an image
     */
    @Test
    public void test_isImageFalse(){
        Media media = new Media();
        assert(!media.isImage());
    }

    /**
     * Test default is not an image
     */
    @Test
    public void test_isImageTrue(){
        Media media = new Media();
        media.isimg = "true";
        assert(media.isImage());
    }


}
