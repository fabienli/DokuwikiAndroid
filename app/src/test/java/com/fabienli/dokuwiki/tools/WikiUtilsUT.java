package com.fabienli.dokuwiki.tools;

import org.junit.Test;

public class WikiUtilsUT {
    /**
     * Test default URL type
     */
    @Test
    public void test_defaultSyncPath(){
        String newUrl = WikiUtils.convertBaseUrlToMainWikiUrl("https://www.mywiki.com/lib/exe/xmlrpc.php");
        String URL_EXPECTED = "https://www.mywiki.com/doku.php?id=";
        System.out.println(newUrl);
        assert(newUrl.compareTo(URL_EXPECTED) == 0);
    }


    /**
     * Test no http provided in URL
     */
    @Test
    public void test_noHttpSyncPath(){
        String newUrl = WikiUtils.convertBaseUrlToMainWikiUrl("www.mywiki.com/lib/exe/xmlrpc.php");
        String URL_EXPECTED = "http://www.mywiki.com/doku.php?id=";
        System.out.println(newUrl);
        assert(newUrl.compareTo(URL_EXPECTED) == 0);
    }


    /**
     * Test URL with sub-folder
     */
    @Test
    public void test_subfolderSyncPath(){
        String newUrl = WikiUtils.convertBaseUrlToMainWikiUrl("https://www.mywiki.com/dokuwiki/lib/exe/xmlrpc.php");
        String URL_EXPECTED = "https://www.mywiki.com/dokuwiki/doku.php?id=";
        System.out.println(newUrl);
        assert(newUrl.compareTo(URL_EXPECTED) == 0);
    }

    /**
     * Test special sync URL: new php script
     */
    @Test
    public void test_diffPhpSyncPath(){
        String newUrl = WikiUtils.convertBaseUrlToMainWikiUrl("https://www.mywiki.com/sync.php");
        String URL_EXPECTED = "https://www.mywiki.com/doku.php?id=";
        System.out.println(newUrl);
        assert(newUrl.compareTo(URL_EXPECTED) == 0);
    }

    /**
     * Test special sync URL: new php script and subfolder
     */
    @Test
    public void test_diffPhpSubfolderSyncPath(){
        String newUrl = WikiUtils.convertBaseUrlToMainWikiUrl("https://www.mywiki.com/dokuwiki/sync.php");
        String URL_EXPECTED = "https://www.mywiki.com/dokuwiki/doku.php?id=";
        System.out.println(newUrl);
        assert(newUrl.compareTo(URL_EXPECTED) == 0);
    }

    /**
     * Test sync URL with sync part in the middle
     */
    @Test
    public void test_verySpecialUrlSyncPath(){
        String newUrl = WikiUtils.convertBaseUrlToMainWikiUrl("https://www.mywiki.com/lib/exe/xmlrpc.php/dokuwiki/lib/exe/xmlrpc.php");
        String URL_EXPECTED = "https://www.mywiki.com/lib/exe/xmlrpc.php/dokuwiki/doku.php?id=";
        System.out.println(newUrl);
        assert(newUrl.compareTo(URL_EXPECTED) == 0);
    }

}
