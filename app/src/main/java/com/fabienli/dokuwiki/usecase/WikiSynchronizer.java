package com.fabienli.dokuwiki.usecase;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.fabienli.dokuwiki.db.AppDatabase;
import com.fabienli.dokuwiki.db.Media;
import com.fabienli.dokuwiki.db.Page;
import com.fabienli.dokuwiki.db.SyncAction;
import com.fabienli.dokuwiki.sync.FileListRetriever;
import com.fabienli.dokuwiki.sync.WikiTitleRetriever;
import com.fabienli.dokuwiki.sync.XmlRpcAdapter;
import com.fabienli.dokuwiki.tools.Logs;
import com.fabienli.dokuwiki.usecase.callback.WikiSynchroCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

public class WikiSynchronizer extends AsyncTask<String, Integer, String> {
    String TAG = "WikiSynchronizer";
    protected AppDatabase _db;
    protected XmlRpcAdapter _xmlRpcAdapter;
    protected String _mediaLocalPath = "";
    protected SharedPreferences _settings;
    WikiSynchroCallback _wikiSynchroCallback = null;

    public WikiSynchronizer(SharedPreferences settings, AppDatabase db, XmlRpcAdapter xmlRpcAdapter, String mediaLocalPath) {
        _db = db;
        _xmlRpcAdapter = xmlRpcAdapter;
        _mediaLocalPath = mediaLocalPath;
        _settings = settings;
    }

    public void retrieveDataFromServer() {

        // 1. pages
        publishProgress(1, 10);
        retrievePagesDataFromServer();

        // 2. medias
        publishProgress(2, 10);
        retrieveMediasDataFromServer();

        // 3. download synchro
        publishProgress(3, 10);
        runDownloadSynchro();

        // 4. wiki title
        publishProgress(8, 10);
        retrieveTitleFromServer();

        // 5. dynamic content'' page
        publishProgress(9, 10);
        runDownloadSynchroDynamicPges();
    }

    public void retrievePagesDataFromServer() {
        // 1. get up-to-date list of pages
        Logs.getInstance().add("Retrieve the list of pages from server");
        FileListRetriever fileListRetriever = new FileListRetriever(_xmlRpcAdapter);
        ArrayList<String> pagesList = fileListRetriever.retrievePagesList("");
        if(pagesList == null){
            Logs.getInstance().add("error received from server");
            return;
        }

        // 2. save in memory the list of current pages from db
        HashSet<String> oldPagesToRemove = new HashSet<>();
        for (Page page : _db.pageDao().getAll()) {
            oldPagesToRemove.add(page.pagename);
        }

        // 3. remove all pending sync actions
        _db.syncActionDao().deleteLevel(SyncAction.LEVEL_GET_FILES);
        _db.syncActionDao().deleteLevel(SyncAction.LEVEL_GET_DYNAMICS);

        // 4. update our db with the list of pages and their version
        for (String item : pagesList) {
            // 4.1 compute the received line to extract data
            // TODO: use PageInfoRetriever here
            String pageinfo = item.replace("{", "").replace("}", "").replace(", ", ",");
            String[] parts = pageinfo.split(",");
            String pageName = "";
            String pageRevision = "";
            for (String a : parts) {
                if (a.startsWith("id=")) {
                    pageName = a.substring(3);
                } else if (a.startsWith("rev=")) { //deprecated
                    pageRevision = a.substring(4);
                } else if (a.startsWith("revision=")) { //new API
                    pageRevision = a.substring(9);
                }
            }
            Log.d(TAG,"Pages found: "+pageName+" / "+pageRevision);
            // 4.2 this page is not to be removed from db
            oldPagesToRemove.remove(pageName);

            // 4.3 make sure the page exists in db, with up-to-date data
            Page page = _db.pageDao().findByName(pageName);
            if (page == null) {
                page = new Page();
                page.pagename = pageName;
                page.rev = pageRevision;
                _db.pageDao().insertAll(page);
            } else if (page.rev.compareTo(pageRevision) != 0) {
                _db.pageDao().updateVersion(pageName, pageRevision);
                // html data is not up to date, so text data is deprecated: remove it to avoid conflicts
                _db.pageDao().updateText(pageName, "");
            }

            // 4.4 check if synchro is need
            SyncAction syncAction = null;
            if (page.rev.compareTo(pageRevision) != 0 || page.isHtmlEmpty()) {
                syncAction = new SyncAction();
                syncAction.priority = SyncAction.LEVEL_GET_FILES;
                syncAction.verb = "GET";
                syncAction.name = pageName;
                syncAction.rev = pageRevision;
                _db.syncActionDao().insertAll(syncAction);
            }

            // 4.5 force sync of page, if no-cache option, or if using a dynamic plugin (indexmenu, catlist)
            else if(page != null && page.text != null) {
                boolean forceSync = false;
                // check nocache option
                if (page.text.length() == 0)
                    forceSync = true;
                else if(page.text.contains("~~NOCACHE~~"))
                    forceSync = true;
                else if(page.text.contains("<catlist"))
                    forceSync = true;
                else if(page.text.contains("{{indexmenu"))
                    forceSync = true;

                if (forceSync) {
                    Log.d("WikiSynchronizer", "page with dynamic content: " + pageName);
                    syncAction = new SyncAction();
                    syncAction.priority = SyncAction.LEVEL_GET_DYNAMICS;
                    syncAction.verb = "GET";
                    syncAction.name = pageName;
                    syncAction.rev = pageRevision;
                    _db.syncActionDao().insertAll(syncAction);
                }
            }
        }

        // 5. remove old pages not in server anymore
        for (String oldPageName : oldPagesToRemove) {
            Page existingItem = _db.pageDao().findByName(oldPageName);
            if (existingItem != null)
                _db.pageDao().delete(existingItem);
        }

        // 6. no dynamic update needed if no new page to sync
        if(_db.syncActionDao().getAllPriority(SyncAction.LEVEL_GET_FILES).size() == 0) {
            _db.syncActionDao().deleteLevel(SyncAction.LEVEL_GET_DYNAMICS);
        }

    }

    public void retrieveMediasDataFromServer() {
        // 1. get up-to-date list of media
        Logs.getInstance().add("Retrieve the list of medias from server");
        FileListRetriever fileListRetriever = new FileListRetriever(_xmlRpcAdapter);
        ArrayList<String> mediasList = fileListRetriever.retrieveMediasList("");
        if(mediasList == null){
            Logs.getInstance().add("error received from server");
            return;
        }

        // 2. save in memory the list of current media from db
        HashSet<String> oldMediasToRemove = new HashSet<>();
        for (Media media : _db.mediaDao().getAll()) {
            oldMediasToRemove.add(media.id);
        }

        // 3. remove all pending sync actions
        _db.syncActionDao().deleteLevel(SyncAction.LEVEL_GET_MEDIAS);


        // 4. update our db with the list of medias and their version
        for (String item : mediasList) {
            Boolean isDownloadNeeded = false;

            // 4.1 compute the received line to extract data
            // TODO: use MediaInfoRetriever here
            String pageinfo = item.replace("{","").replace("}","").replace(", ",",");
            String[] parts = pageinfo.split(",");
            String mediaFile = "";//deprecated
            String mediaId = "";
            String mediaSize = "";
            String mediaMTime = "";
            String mediaLastModified = "";
            String mediaIsImg = "";
            for (String a : parts) {
                if(a.startsWith("id=")){
                    mediaId = a.substring(3);
                }
                else if(a.startsWith("file=")){ //deprecated
                    mediaFile = a.substring(5);
                }
                else if(a.startsWith("size=")){
                    mediaSize = a.substring(5);
                }
                else if(a.startsWith("isimg=")){ //deprecated
                    mediaIsImg = a.substring(6);
                }
                else if(a.startsWith("isimage=")){
                    mediaIsImg = a.substring(8);
                }
                else if(a.startsWith("mtime=")){ //deprecated
                    mediaMTime = a.substring(6);
                }
                else if(a.startsWith("lastModified=")){ //deprecated
                    mediaLastModified = a.substring(13);
                }
                else if(a.startsWith("revision=")){
                    mediaLastModified = a.substring(9);
                }
            }

            // 4.2 this media is not to be removed from db
            oldMediasToRemove.remove(mediaId);

            // 4.3 make sure the media exists in db, with up-to-date data
            Media media = _db.mediaDao().findByName(mediaId);
            if (media == null) {
                media = new Media();
                media.file = mediaFile;
                media.id = mediaId;
                media.size = mediaSize;
                media.isimg = mediaIsImg;
                media.mtime = mediaMTime;
                media.lastModified = mediaLastModified;
                _db.mediaDao().insertAll(media);
            } else if (media.mtime == null || media.mtime.compareTo(mediaMTime) != 0) {
                //TODO: change values or to be done at file download time? -> _db.mediaDao().updateAll(media);
                isDownloadNeeded = true;
            }
            else {
                // check if local file is there
                String imageFilePath = mediaId.replaceAll(":","/");
                File originalfile = new File(_mediaLocalPath, imageFilePath);
                isDownloadNeeded = ( !originalfile.exists() );
            }

            // 4.4 check if synchro is need
            if (isDownloadNeeded) {
                SyncAction syncAction = new SyncAction();
                syncAction.priority = SyncAction.LEVEL_GET_MEDIAS;
                syncAction.verb = "GET";
                syncAction.name = mediaId;
                syncAction.rev = mediaMTime;
                _db.syncActionDao().insertAll(syncAction);
            }

        }

        // 5. remove old medias not in server anymore
        for (String oldFileName : oldMediasToRemove) {
            Media existingItem = _db.mediaDao().findByName(oldFileName);
            if (existingItem != null)
                _db.mediaDao().delete(existingItem);
        }
    }

    public void runDownloadSynchro() {
        SynchroDownloadHandler synchroDownloadHandler = new SynchroDownloadHandler(_settings, _db, _xmlRpcAdapter, _mediaLocalPath, _wikiSynchroCallback);
        Logs.getInstance().add("Retry the urgent items to be synced");
        synchroDownloadHandler.syncPrioZero();
        synchroDownloadHandler.syncPrio1();

        Logs.getInstance().add("Download items level 2");
        String typesync = _settings.getString("list_type_sync", "a");
        if(typesync.compareTo("b")==0) {
            synchroDownloadHandler.syncPage2();
        }

        Logs.getInstance().add("Download items level 3");
        if(typesync.compareTo("b")==0) {
            synchroDownloadHandler.syncMedia3();
        }
    }
    public void runDownloadSynchroDynamicPges() {
        SynchroDownloadHandler synchroDownloadHandler = new SynchroDownloadHandler(_settings, _db, _xmlRpcAdapter, _mediaLocalPath, _wikiSynchroCallback);

        Logs.getInstance().add("Download items level 5");
        String typesync = _settings.getString("list_type_sync", "a");
        if(typesync.compareTo("b")==0) {
            synchroDownloadHandler.syncPage5();
        }
    }

    public void retrieveTitleFromServer() {
        WikiTitleRetriever wikiTitleRetriever = new WikiTitleRetriever(_xmlRpcAdapter);
        String title = wikiTitleRetriever.retrieveTitle();
        _settings.edit().putString("wikiTitle", title).commit();
    }

    public void retrieveDataFromServerAsync(WikiSynchroCallback wikiSynchroCallback) {
        _wikiSynchroCallback = wikiSynchroCallback;
        executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    protected String doInBackground(String... params) {
        retrieveDataFromServer();
        return "ok";
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        String footer = "";
        if (values.length>=1)
            if(values[0]==1)
                footer = "Get pages list";
            else if(values[0]==2)
                footer = "Get medias list";
            else if(values[0]==3)
                footer = "Download updates";
            else if(values[0]==8)
                footer = "Sync meta data";
            else if(values[0]==9)
                footer = "Dynamic pages update";
        if(_wikiSynchroCallback!=null)
            _wikiSynchroCallback.progressUpdate("", footer, values);

    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if(_wikiSynchroCallback!=null)
            _wikiSynchroCallback.onceDone();
    }

}
