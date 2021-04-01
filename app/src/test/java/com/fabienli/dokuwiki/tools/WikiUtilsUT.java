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
    public void test_autoindent_level1_newline(){
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
    //TODO: code this:
    /*@Test
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
    }*/


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

}
