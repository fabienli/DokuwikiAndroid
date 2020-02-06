package com.fabienli.dokuwiki.sync;

import android.util.Log;

import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class XmlRpcThrottler {
    String TAG = "XmlRpcThrottler";
    static XmlRpcThrottler _instance = null;
    LinkedList<Date> _callLog;
    int _limit = 1000;

    public static XmlRpcThrottler instance() {
        if(_instance == null){
            _instance = resetInstance();
        }
        return _instance;
    }

    public static XmlRpcThrottler resetInstance() {
        _instance = new XmlRpcThrottler();
        _instance._callLog = new LinkedList<>();
        return _instance;
    }

    public void addCallNow() {
        Date now = new Date();
        this._callLog.add(now);
        Log.d(TAG, "throttling for: "+now+" ("+this._callLog.size()+"/"+this._limit+")");
    }

    public int getNbCallLastMinute() {
        int nbCall = this._callLog.size();
        if(nbCall >= _limit){ // compute the actual nb in range only if could be higher than limit
            purgeOlderThanMinute();
            nbCall = this._callLog.size();
        }
        return nbCall;
    }

    public void purgeOlderThanMinute(){
        long diffInMillies = 60001;
        Date now = new Date();
        while(diffInMillies > 60000) {
            if(_callLog.size() == 0) {
                diffInMillies = 0;
            }
            else {
                Date last = _callLog.get(0);
                diffInMillies = now.getTime() - last.getTime();
            }
            if (diffInMillies > 60000) {
                _callLog.remove(0);
            }
        }
    }

    public void setLimit(int i) {
        if(i>0) _limit = i;
    }

    public boolean isNextCallInLimit() {
        return (getNbCallLastMinute() < _limit);
    }

    public long getTimeToWait() {
        if(_callLog.size()>0) {
            Date now = new Date();
            Date last = _callLog.get(0);
            long diffInMillies = now.getTime() - last.getTime();
            return (60000 - diffInMillies);
        }
        else {
            return 0;
        }
    }

    public void waitIfNotInLimit() throws InterruptedException {
        while(!isNextCallInLimit()) {
            Log.d(TAG, "waiting ..."+getTimeToWait());
            TimeUnit.SECONDS.sleep(1+getTimeToWait()/1000);
        }
    }
}
