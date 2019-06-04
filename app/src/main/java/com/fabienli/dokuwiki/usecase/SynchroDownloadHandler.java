package com.fabienli.dokuwiki.usecase;

import android.content.SharedPreferences;
import android.util.Log;

import com.fabienli.dokuwiki.db.AppDatabase;
import com.fabienli.dokuwiki.db.PageUpdateText;
import com.fabienli.dokuwiki.db.SyncAction;
import com.fabienli.dokuwiki.sync.MediaDownloader;
import com.fabienli.dokuwiki.sync.PageInfoRetriever;
import com.fabienli.dokuwiki.sync.PageTextDownUpLoader;
import com.fabienli.dokuwiki.sync.XmlRpcAdapter;
import com.fabienli.dokuwiki.tools.Logs;
import com.fabienli.dokuwiki.usecase.callback.WikiSynchroCallback;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static java.lang.Math.min;

public class SynchroDownloadHandler {
    static String TAG = "SynchroDownloadHandler";
    protected SharedPreferences _settings;
    protected XmlRpcAdapter _xmlRpcAdapter;
    protected AppDatabase _db;
    protected String _mediaLocalPath;
    protected Boolean _syncToBePlanned = false;
    protected int _syncOngoing = 0;
    protected int _level2ongoing = 0;
    protected int _level3ongoing = 0;
    WikiSynchroCallback _wikiSynchroCallback = null;

    public SynchroDownloadHandler(SharedPreferences settings, AppDatabase db, XmlRpcAdapter xmlRpcAdapter, String mediaLocalPath, WikiSynchroCallback wikiSynchroCallback){
        _xmlRpcAdapter = xmlRpcAdapter;
        _settings = settings;
        _db = db;
        _mediaLocalPath = mediaLocalPath;
        _wikiSynchroCallback = wikiSynchroCallback;
    }

    public void syncPrioZero(){
        if(_syncOngoing == 0) {
            addOneSyncOngoing();
            int i=0;
            List<SyncAction> saList = _db.syncActionDao().getAllPriority("0");
            int max = saList.size();
            for (SyncAction sa : saList) {
                if(_wikiSynchroCallback != null)
                    _wikiSynchroCallback.progressUpdate("Send pages", i, max);
                executeAction(sa);
                i++;
            }
            removeOneSyncOngoing();

        }
        else
        {
            _syncToBePlanned = true;
        }
    }

    public void syncPrio1(){
        if(_syncOngoing == 0) {
            addOneSyncOngoing();
            int i=0;
            List<SyncAction> saList = _db.syncActionDao().getAllPriority("1");
            int max = saList.size();
            for (SyncAction sa : saList) {
                if(_wikiSynchroCallback != null)
                    _wikiSynchroCallback.progressUpdate("Send media", i, max);
                executeMediaAction(sa);
                i++;
            }
            removeOneSyncOngoing();

        }
        else
        {
            _syncToBePlanned = true;
        }
    }

    public void syncPage2(){
        if(_syncOngoing == 0) {
            addOneSyncOngoing();

            final int maxpages = Integer.parseInt(_settings.getString("max_page_sync", "2"));;

            List<SyncAction> saList = _db.syncActionDao().getAllPriority("2");

            // init the sync for requested level
            if(_level2ongoing == 0)
                _level2ongoing = min(maxpages, saList.size());

            if(_level2ongoing > 0) {
                if(_wikiSynchroCallback != null)
                    _wikiSynchroCallback.progressUpdate("Download pages", (maxpages - _level2ongoing), maxpages);

                _level2ongoing--;

                try {
                    for (SyncAction sa : saList) {
                        executeAction(sa);
                        break;
                    }
                } finally {
                    removeOneSyncOngoing();
                }
            }
            else {
                removeOneSyncOngoing();
            }
        }
    }

    public void syncMedia3(){//to be re-used for level 1?
        if(_syncOngoing == 0) {
            addOneSyncOngoing();

            final int maxmedia = Integer.parseInt(_settings.getString("max_media_sync", "2"));;

            List<SyncAction> saList = _db.syncActionDao().getAllPriority("3");

            if(_level3ongoing == 0)
                _level3ongoing = min(maxmedia, saList.size());

            if(_level3ongoing > 0) {
                if (_wikiSynchroCallback != null)
                    _wikiSynchroCallback.progressUpdate("Download medias", (maxmedia - _level3ongoing), maxmedia);

                _level3ongoing--;

                try {
                    for (SyncAction sa : saList) {
                        executeMediaAction(sa);
                        break;
                    }
                } finally {
                    removeOneSyncOngoing();
                }
            }
            else {
                removeOneSyncOngoing();
            }
        }
    }

    private void executeAction(final SyncAction sa) {
        addOneSyncOngoing();
        if(sa.verb.compareTo("PUT")==0){
            PageTextDownUpLoader pageTextDownUpLoader = new PageTextDownUpLoader(_xmlRpcAdapter);

            // 1. get the current server's version
            Logs.getInstance().add("Put page "+sa.name+" to server");
            PageInfoRetriever pageInfoRetriever = new PageInfoRetriever(_xmlRpcAdapter);
            String server_rev = pageInfoRetriever.retrievePageVersion(sa.name);

            // 2. ensure we are based on same version, or it's a conflict
            String textContent = sa.data;
            if(sa.rev.compareTo(server_rev) != 0) {
                Log.d("DEBUG", "need a conflict handling: " + sa.rev + " - " + server_rev);
                Logs.getInstance().add("conflict page " + sa.name + ": retrieve text from server to merge conflict");

                textContent += "\n\n----\n\nconflict between versions "+sa.rev+" and "+server_rev+"\n\n----\n\n";
                textContent += pageTextDownUpLoader.retrievePageText(sa.name);

                PageUpdateText pageUpdateText = new PageUpdateText(_db, sa.name, textContent);
                pageUpdateText.doSync();
                Logs.getInstance().add("page "+sa.name+" stored in local db" );
            }

            // 3. push the content to server
            pageTextDownUpLoader.sendPageText(sa.name, textContent);

            // 4. update the local HTML version
            Logs.getInstance().add("page "+sa.name+" should be updated, get its HTML from server");
            _syncToBePlanned = true;
            SyncAction syncAction = new SyncAction();
            syncAction.priority = "0";
            syncAction.verb = "GET";
            syncAction.name = sa.name;
            syncAction.rev = "0";
            syncAction.data = "";
            _db.syncActionDao().insertAll(syncAction);

            // 5. the end
            _db.syncActionDao().deleteAll(sa);
        }
        else if(sa.verb.compareTo("GET")==0){
            Logs.getInstance().add("Get page "+sa.name+" from server");

            // 1. force the download of current's server version
            PageHtmlRetrieveForceDownload pageHtmlRetrieveForceDownload = new PageHtmlRetrieveForceDownload(_db, _xmlRpcAdapter);
            pageHtmlRetrieveForceDownload.retrievePage(sa.name);

            // 2. the end
            _db.syncActionDao().deleteAll(sa);
        }
        removeOneSyncOngoing(); // remove the synchro from GET/PUT page
    }

    private void executeMediaAction(final SyncAction sa) {
        addOneSyncOngoing();
        if(sa.verb.compareTo("GET")==0){
            Logs.getInstance().add("Get media "+sa.name+" from server");
            String mediaFilePath = sa.name.replaceAll(":","/");

            // 1. force the download of current's server version
            MediaRetrieve mediaRetrieve = new MediaRetrieve(_db, _xmlRpcAdapter, _mediaLocalPath);
            mediaRetrieve.getMedia(sa.name, mediaFilePath, 0, 0, true);

            // 2. make sure to delete all other sizes of this image
            File f = new File(_mediaLocalPath);
            final Pattern p = Pattern.compile(mediaFilePath+"_[1-9]*_[1-9]*");
            File[] flists = f.listFiles(file -> p.matcher(file.getName()).matches());
            for(File fileToDelete : flists) {
                fileToDelete.delete();
                Log.d(TAG, "removing file: "+fileToDelete.getName());
            }

            // 3. the end
            _db.syncActionDao().deleteAll(sa);
        }
        else if(sa.verb.compareTo("PUT")==0){
            Log.d(TAG, "PUT media: "+sa.toText());
            if(sa.data != null) {

                // 1. call the upload of this file
                MediaDownloader mediaDownloader = new MediaDownloader(_xmlRpcAdapter);
                mediaDownloader.uploadMedia(sa.data);

                // 2. the end
                //TODO: one fully implemented: _db.syncActionDao().deleteAll(sa);

            }
        }
        removeOneSyncOngoing(); // remove the synchro from GET media
    }

    public void addOneSyncOngoing() {
        _syncOngoing++;
    }

    public void removeOneSyncOngoing() {
        _syncOngoing--;
        if(_syncOngoing == 0 && _syncToBePlanned) { //someone requested the sync while we were running
            _syncToBePlanned = false;
            syncPrioZero();
        }
        else if(_syncOngoing == 0 && _level2ongoing>0) { //some more items to be synced
            final Integer urldelay = Integer.parseInt(_settings.getString("list_delay_sync", "0"));
            try {
                TimeUnit.SECONDS.sleep(urldelay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(_syncOngoing == 0 && _level2ongoing>0) { //check again if some more items to be synced
                syncPage2();
            }
        }
        else if(_syncOngoing == 0 && _level3ongoing>0) { //some more items to be synced
            final Integer urldelay = Integer.parseInt(_settings.getString("list_delay_sync", "0"));
            try {
                TimeUnit.SECONDS.sleep(urldelay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(_syncOngoing == 0 && _level3ongoing>0) { //check again if some more items to be synced
                syncMedia3();
            }
        }

    }
}
