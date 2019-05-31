package com.fabienli.dokuwiki.tools;

import java.util.ArrayList;

public class Logs {
    static Logs _instance = null;
    public ArrayList<String> _data;

    private Logs(){
        _data = new ArrayList<>();
    }

    public static Logs getInstance() {
        if(_instance == null)
            _instance = new Logs();
        return _instance;
    }

    public void add(String data){
        _data.add(data);
    }
}
