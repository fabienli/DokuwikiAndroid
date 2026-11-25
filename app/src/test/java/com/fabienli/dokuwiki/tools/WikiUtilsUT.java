package com.fabienli.dokuwiki.tools;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WikiUtilsUT {
    /**
     * Page titles, taken from the first heading of the page html
     */
    @Test
    public void test_pageTitle_firstHeading(){
        String html = "\n<h1 class=\"sectionedit1\" id=\"vpn_setup\">VPN setup</h1>\n<div class=\"level1\">\n<p>text</p>\n</div>\n"
                + "<h2 class=\"sectionedit2\" id=\"client\">Client</h2>\n";
        assertEquals("VPN setup", WikiUtils.getPageTitle(html));
    }

    @Test
    public void test_pageTitle_firstHeadingIsNotH1(){
        assertEquals("Client", WikiUtils.getPageTitle("<p>intro</p><h3 id=\"client\">Client</h3><h1>Later</h1>"));
    }

    @Test
    public void test_pageTitle_innerTagsAndEntities(){
        assertEquals("Tom & Jerry's <page> é",
                WikiUtils.getPageTitle("<H2 id=\"x\"><a name=\"x\">Tom &amp; Jerry&#039;s</a> &lt;page&gt;\n&#xE9;</H2>"));
    }

    @Test
    public void test_pageTitle_namedEntities(){
        assertEquals("Licensing \u2014 Copyright \u00A9 2026",
                WikiUtils.getPageTitle("<h1>Licensing &mdash; Copyright &copy; 2026</h1>"));
        assertEquals("&unknown; stays", WikiUtils.getPageTitle("<h1>&unknown; stays</h1>"));
    }

    @Test
    public void test_pageTitle_escapedEntityIsDecodedOnce(){
        assertEquals("&lt; and &#38;", WikiUtils.getPageTitle("<h1>&amp;lt; and &amp;#38;</h1>"));
    }

    @Test
    public void test_pageTitle_invalidCodePoint(){
        assertEquals("Release \uFFFD \uFFFD", WikiUtils.getPageTitle("<h1>Release &#x110000; &#99999999999;</h1>"));
    }

    /**
     * Titles built from the page id, for pages without heading
     */
    @Test
    public void test_pageTitleFromId_pageInNamespace(){
        assertEquals("home", WikiUtils.getPageTitleFromId("infra:network:home"));
        assertEquals("vpn setup", WikiUtils.getPageTitleFromId("infra:network:vpn_setup"));
    }

    @Test
    public void test_pageTitleFromId_namespaceStartPage(){
        assertEquals("network", WikiUtils.getPageTitleFromId("infra:network:start"));
    }

    @Test
    public void test_pageTitleFromId_rootPages(){
        assertEquals("wiki", WikiUtils.getPageTitleFromId("wiki"));
        assertEquals("start", WikiUtils.getPageTitleFromId("start"));
    }

    @Test
    public void test_pageTitleFromId_leadingAndTrailingColons(){
        assertEquals("vpn", WikiUtils.getPageTitleFromId(":infra:vpn"));
        assertEquals("infra", WikiUtils.getPageTitleFromId("infra:"));
    }

    @Test
    public void test_pageTitleFromId_emptyOrNull(){
        assertEquals("", WikiUtils.getPageTitleFromId(""));
        assertEquals(":", WikiUtils.getPageTitleFromId(":"));
        assertEquals("", WikiUtils.getPageTitleFromId(null));
    }

    @Test
    public void test_pageTitle_noHeading(){
        assertNull(WikiUtils.getPageTitle("<p>no heading here</p>"));
        assertNull(WikiUtils.getPageTitle("<h1> </h1>"));
        assertNull(WikiUtils.getPageTitle(""));
        assertNull(WikiUtils.getPageTitle(null));
    }

    /**
     * Test default URL type
     */
    @Test
    public void test_defaultSyncPath(){
        String newUrl = WikiUtils.convertBaseUrlToMainWikiUrl("https://www.mywiki.com/lib/exe/xmlrpc.php");
        String URL_EXPECTED = "https://www.mywiki.com/doku.php?id=";
        //System.out.println(newUrl);
        assert(newUrl.compareTo(URL_EXPECTED) == 0);
    }


    /**
     * Test no http provided in URL
     */
    @Test
    public void test_noHttpSyncPath(){
        String newUrl = WikiUtils.convertBaseUrlToMainWikiUrl("www.mywiki.com/lib/exe/xmlrpc.php");
        String URL_EXPECTED = "http://www.mywiki.com/doku.php?id=";
        //System.out.println(newUrl);
        assert(newUrl.compareTo(URL_EXPECTED) == 0);
    }


    /**
     * Test URL with sub-folder
     */
    @Test
    public void test_subfolderSyncPath(){
        String newUrl = WikiUtils.convertBaseUrlToMainWikiUrl("https://www.mywiki.com/dokuwiki/lib/exe/xmlrpc.php");
        String URL_EXPECTED = "https://www.mywiki.com/dokuwiki/doku.php?id=";
        //System.out.println(newUrl);
        assert(newUrl.compareTo(URL_EXPECTED) == 0);
    }

    /**
     * Test special sync URL: new php script
     */
    @Test
    public void test_diffPhpSyncPath(){
        String newUrl = WikiUtils.convertBaseUrlToMainWikiUrl("https://www.mywiki.com/sync.php");
        String URL_EXPECTED = "https://www.mywiki.com/doku.php?id=";
        //System.out.println(newUrl);
        assert(newUrl.compareTo(URL_EXPECTED) == 0);
    }

    /**
     * Test special sync URL: new php script and subfolder
     */
    @Test
    public void test_diffPhpSubfolderSyncPath(){
        String newUrl = WikiUtils.convertBaseUrlToMainWikiUrl("https://www.mywiki.com/dokuwiki/sync.php");
        String URL_EXPECTED = "https://www.mywiki.com/dokuwiki/doku.php?id=";
        //System.out.println(newUrl);
        assert(newUrl.compareTo(URL_EXPECTED) == 0);
    }

    /**
     * Test sync URL with sync part in the middle
     */
    @Test
    public void test_verySpecialUrlSyncPath(){
        String newUrl = WikiUtils.convertBaseUrlToMainWikiUrl("https://www.mywiki.com/lib/exe/xmlrpc.php/dokuwiki/lib/exe/xmlrpc.php");
        String URL_EXPECTED = "https://www.mywiki.com/lib/exe/xmlrpc.php/dokuwiki/doku.php?id=";
        //System.out.println(newUrl);
        assert(newUrl.compareTo(URL_EXPECTED) == 0);
    }

    /**
     * Test autoIndent new list level 1 with *
     */
    @Test
    public void test_autoindent_unordered_level1_newline(){
        CharSequence updatedText = "  * line 1\n";
        int position = 10;
        WikiUtils.EditedText newText = WikiUtils.autoIndentLists(updatedText, position, 1);
        String NEW_TEXT_EXPECTED = "  * line 1\n  * ";
        int NEW_POSITION = 15;
        assert(newText != null);
        //System.out.println(newText.text.replace("\n","\\n"));
        assert(newText.text.compareTo(NEW_TEXT_EXPECTED) == 0);
        assert (newText.cursorPosition == NEW_POSITION);
    }


    /**
     * Test autoIndent new list level 1 with -
     */
    @Test
    public void test_autoindent_orderer_level1_newline(){
        CharSequence updatedText = "  - line 1\n";
        int position = 10;
        WikiUtils.EditedText newText = WikiUtils.autoIndentLists(updatedText, position, 1);
        String NEW_TEXT_EXPECTED = "  - line 1\n  - ";
        int NEW_POSITION = 15;
        assert(newText != null);
        //System.out.println(newText.text.replace("\n","\\n"));
        assert(newText.text.compareTo(NEW_TEXT_EXPECTED) == 0);
        assert (newText.cursorPosition == NEW_POSITION);
    }

    /**
     * Test autoIndent new list level 1 with * and text after
     */
    @Test
    public void test_autoindent_level1_newline_unused_text_after(){
        CharSequence updatedText = "  * line 1\n\nthis text is not used";
        int position = 10;
        WikiUtils.EditedText newText = WikiUtils.autoIndentLists(updatedText, position, 1);
        String NEW_TEXT_EXPECTED = "  * line 1\n  * \nthis text is not used";
        int NEW_POSITION = 15;
        assert(newText != null);
        //System.out.println(newText.text.replace("\n","\\n"));
        assert(newText.text.compareTo(NEW_TEXT_EXPECTED) == 0);
        assert (newText.cursorPosition == NEW_POSITION);
    }
    /**
     * Test autoIndent new list level 1 with * and text spilt in list
     */
    @Test
    public void test_autoindent_level1_newline_text_in_list(){
        CharSequence updatedText = "  * line 1\nthis text is in the list";
        int position = 10;
        WikiUtils.EditedText newText = WikiUtils.autoIndentLists(updatedText, position, 1);
        String NEW_TEXT_EXPECTED = "  * line 1\n  * this text is in the list";
        int NEW_POSITION = 15;
        assert(newText != null);
        //System.out.println(newText.text.replace("\n","\\n"));
        assert(newText.text.compareTo(NEW_TEXT_EXPECTED) == 0);
        assert (newText.cursorPosition == NEW_POSITION);
    }


    /**
     * Test autoIndent level 2 on new line
     */
    @Test
    public void test_autoindent_level2_new_line(){
        CharSequence updatedText = "  * line 1\n  *  ";
        int position = 15;
        WikiUtils.EditedText newText = WikiUtils.autoIndentLists(updatedText, position, 1);
        String NEW_TEXT_EXPECTED = "  * line 1\n    * ";
        int NEW_POSITION = 17;
        assert(newText != null);
        //System.out.println(newText.text.replace("\n","\\n"));
        //System.out.println(newText.cursorPosition);
        assert(newText.text.compareTo(NEW_TEXT_EXPECTED) == 0);
        assert (newText.cursorPosition == NEW_POSITION);
    }

    /**
     * Test autoIndent level 2 single line
     */
    @Test
    public void test_autoindent_level2_single_line(){
        CharSequence updatedText = "  *  ";
        int position = 4;
        WikiUtils.EditedText newText = WikiUtils.autoIndentLists(updatedText, position, 1);
        String NEW_TEXT_EXPECTED = "    * ";
        int NEW_POSITION = 6;
        assert(newText != null);
        //System.out.println(newText.text.replace("\n","\\n"));
        //System.out.println(newText.cursorPosition);
        assert(newText.text.compareTo(NEW_TEXT_EXPECTED) == 0);
        assert (newText.cursorPosition == NEW_POSITION);
    }


    /**
     * Test autoIndent level 3 single line
     */
    @Test
    public void test_autoindent_level3_single_line(){
        CharSequence updatedText = "    *  ";
        int position = 6;
        WikiUtils.EditedText newText = WikiUtils.autoIndentLists(updatedText, position, 1);
        String NEW_TEXT_EXPECTED = "      * ";
        int NEW_POSITION = 8;
        assert(newText != null);
        //System.out.println(newText.text.replace("\n","\\n"));
        //System.out.println(newText.cursorPosition);
        assert(newText.text.compareTo(NEW_TEXT_EXPECTED) == 0);
        assert (newText.cursorPosition == NEW_POSITION);
    }


    /**
     * Test autoIndent level 2 on new line with unused text after
     */
    @Test
    public void test_autoindent_level2_new_line_unused_text(){
        CharSequence updatedText = "  * line 1\n  *  \nuseless other line";
        int position = 15;
        WikiUtils.EditedText newText = WikiUtils.autoIndentLists(updatedText, position, 1);
        String NEW_TEXT_EXPECTED = "  * line 1\n    * \nuseless other line";
        int NEW_POSITION = 17;
        assert(newText != null);
        //System.out.println(newText.text.replace("\n","\\n"));
        //System.out.println(newText.cursorPosition);
        assert(newText.text.compareTo(NEW_TEXT_EXPECTED) == 0);
        assert (newText.cursorPosition == NEW_POSITION);
    }


    /**
     * Test autoIndent new list level 2 with *
     */
    @Test
    public void test_autoindent_level2_newline(){
        CharSequence updatedText = "    * line 1\n";
        int position = 12;
        WikiUtils.EditedText newText = WikiUtils.autoIndentLists(updatedText, position, 1);
        String NEW_TEXT_EXPECTED = "    * line 1\n    * ";
        int NEW_POSITION = 19;
        assert(newText != null);
        //System.out.println(newText.text.replace("\n","\\n"));
        assert(newText.text.compareTo(NEW_TEXT_EXPECTED) == 0);
        assert (newText.cursorPosition == NEW_POSITION);
    }

    /**
     * Test autoIndent reduce level 2 *
     */
    @Test
    public void test_autoindent_level2_reduce_to1(){
        CharSequence updatedText = "    * line 1\n    *";
        int position = 18;
        WikiUtils.EditedText newText = WikiUtils.autoIndentLists(updatedText, position, 0);
        String NEW_TEXT_EXPECTED = "    * line 1\n  * ";
        int NEW_POSITION = 17;
        assert(newText != null);
        //System.out.println(newText.text.replace("\n","\\n"));
        assert(newText.text.compareTo(NEW_TEXT_EXPECTED) == 0);
        assert (newText.cursorPosition == NEW_POSITION);
    }
    /**
     * Test autoIndent reduce level 1 *
     */
    @Test
    public void test_autoindent_level1_reduce_to_nothing(){
        CharSequence updatedText = "    * line 1\n  *";
        int position = 16;
        WikiUtils.EditedText newText = WikiUtils.autoIndentLists(updatedText, position, 0);
        String NEW_TEXT_EXPECTED = "    * line 1\n";
        int NEW_POSITION = 13;
        assert(newText != null);
        //System.out.println(newText.text.replace("\n","\\n"));
        assert(newText.text.compareTo(NEW_TEXT_EXPECTED) == 0);
        assert (newText.cursorPosition == NEW_POSITION);
    }

    /**
     * Test invalid indent no
     */
    @Test
    public void test_invalid_indent(){
        CharSequence updatedText = " *";
        int position = 2;
        WikiUtils.EditedText newText = WikiUtils.autoIndentLists(updatedText, position, 0);
        String NEW_TEXT_EXPECTED = "* ";
        int NEW_POSITION = 2;
        assert(newText == null);
    }


    /**
     * Test autoIndent remove list level 1 as entered empty
     */
    @Test
    public void test_autoindent_level1_remove_line(){
        CharSequence updatedText = "  * line 1\n  * \n";
        int position = 15;
        WikiUtils.EditedText newText = WikiUtils.autoIndentLists(updatedText, position, 1);
        String NEW_TEXT_EXPECTED = "  * line 1\n\n";
        int NEW_POSITION = 12;
        assert(newText != null);
        //System.out.println(newText.text.replace("\n","\\n"));
        //System.out.println(newText.cursorPosition);
        assert(newText.text.compareTo(NEW_TEXT_EXPECTED) == 0);
        assert (newText.cursorPosition == NEW_POSITION);
    }

    /**
     * Test autoIndent remove list level 2 as entered empty
     */
    @Test
    public void test_autoindent_level2_remove_line(){
        CharSequence updatedText = "  * line 1\n    * \n";
        int position = 17;
        WikiUtils.EditedText newText = WikiUtils.autoIndentLists(updatedText, position, 1);
        String NEW_TEXT_EXPECTED = "  * line 1\n\n";
        int NEW_POSITION = 12;
        assert(newText != null);
        //System.out.println(newText.text.replace("\n","\\n"));
        //System.out.println(newText.cursorPosition);
        assert(newText.text.compareTo(NEW_TEXT_EXPECTED) == 0);
        assert (newText.cursorPosition == NEW_POSITION);
    }


    /**
     * Test autoIndent remove list level 1 as entered empty with text after
     */
    @Test
    public void test_autoindent_level1_remove_line_unused_text_after(){
        CharSequence updatedText = "  * line 1\n  * \n\nuseless text after";
        int position = 15;
        WikiUtils.EditedText newText = WikiUtils.autoIndentLists(updatedText, position, 1);
        String NEW_TEXT_EXPECTED = "  * line 1\n\n\nuseless text after";
        int NEW_POSITION = 12;
        assert(newText != null);
        //System.out.println(newText.text.replace("\n","\\n"));
        //System.out.println(newText.cursorPosition);
        assert(newText.text.compareTo(NEW_TEXT_EXPECTED) == 0);
        assert (newText.cursorPosition == NEW_POSITION);
    }


    @Test
    public void isUnencryptedUrl_detectsHttp() {
        assertTrue(WikiUtils.isUnencryptedUrl("http://wiki.local/lib/exe/xmlrpc.php"));
        assertTrue(WikiUtils.isUnencryptedUrl(" HTTP://wiki.local/lib/exe/xmlrpc.php"));
        assertFalse(WikiUtils.isUnencryptedUrl("https://wiki.local/lib/exe/xmlrpc.php"));
        assertFalse(WikiUtils.isUnencryptedUrl(""));
        assertFalse(WikiUtils.isUnencryptedUrl(null));
    }
}
