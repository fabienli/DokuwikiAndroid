package com.fabienli.dokuwiki.cache;

import com.fabienli.dokuwiki.db.Page;

public class WikiPage {
    public String _html;
    public String _text;
    public String _version;
    public String _latest_version;

    public WikiPage() {
        _html = "";
        _text = "";
        _version = "0";
        _latest_version = "0";
    }

    public WikiPage(Page p) {
        _html = p.html;
        _text = p.text;
        _version = p.rev;
        _latest_version = p.rev;
        if(_html.compareTo("TBD")==0)
        {
            _html = "";
        }
        if(_text.compareTo("TBD")==0)
        {
            _text = "";
        }
    }
}
