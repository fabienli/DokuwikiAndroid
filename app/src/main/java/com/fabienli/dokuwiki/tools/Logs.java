package com.fabienli.dokuwiki.tools;

import android.util.Log;

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
    public void purgeToMax(){
        int l = _data.size();
        Log.d("Logs", "currently "+l+" logs items");
        if(l > 500) {
            _data = new ArrayList<>(_data.subList(l-500, l));
        }
    }
}
