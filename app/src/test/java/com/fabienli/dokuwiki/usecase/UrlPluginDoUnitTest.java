package com.fabienli.dokuwiki.usecase;

import org.junit.Test;

public class UrlPluginDoUnitTest {

    /**
     * Test the html conversion to local html
     */
    @Test
    public void UrlConverter_htmlConversion_do_undone(){
        String UrlCall = "http://dokuwiki/doku.php?id=notes:3.projectsperso:prise_de_masse:start&amp;do=plugin_do&amp;do_page=notes%3A3.projectsperso%3Aprise_de_masse%3Astart&amp;do_md5=7f705ceac979094d138ddb4ae021b16b";
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/", "default_css.css");
        assert(urlConverter.isPluginActionOnline(UrlCall));
        assert(urlConverter.redirectPluginActionOnline(UrlCall, "toto").startsWith("toto"));
    }
}
