package com.fabienli.dokuwiki.cache;

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class WikiPageList {
    public ArrayList<String> _pagelist;
    public SortedMap<String, String> _pageversions;
    public SortedMap<String, WikiPage> _pages;

    public WikiPageList(){
        _pagelist = new ArrayList<String>();
        _pageversions = new TreeMap<String, String>();
        _pages = new TreeMap<String, WikiPage>();
    }

}
