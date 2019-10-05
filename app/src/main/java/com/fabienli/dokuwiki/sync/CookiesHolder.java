package com.fabienli.dokuwiki.sync;

import java.util.ArrayList;
import java.util.List;

class CookiesHolder {
    private static CookiesHolder instance = null;
    final List<String> cookies = new ArrayList<>();

    static CookiesHolder Instance() {
        if(instance == null){
            instance = new CookiesHolder();
        }
        return instance;
    }
}
