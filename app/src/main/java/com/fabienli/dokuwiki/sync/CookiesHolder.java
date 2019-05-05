package com.fabienli.dokuwiki.sync;

import java.util.ArrayList;
import java.util.List;

public class CookiesHolder {
    static CookiesHolder instance = null;
    final List<String> cookies = new ArrayList<>();

    public static CookiesHolder Instance() {
        if(instance == null){
            instance = new CookiesHolder();
        }
        return instance;
    }
}
