package com.fabienli.dokuwiki;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.fabienli.dokuwiki.cache.WikiPage;
import com.fabienli.dokuwiki.db.AppDatabase;
import com.fabienli.dokuwiki.db.DbCallbackInterface;
import com.fabienli.dokuwiki.db.DbUsecaseHandler;
import com.fabienli.dokuwiki.db.SyncAction;
import com.fabienli.dokuwiki.db.SyncActionListInterface;
import com.fabienli.dokuwiki.sync.SyncUsecaseCallbackInterface;
import com.fabienli.dokuwiki.sync.SyncUsecaseHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.room.Room;

public class SynchroHandler {
    static String TAG = "SynchroHandler";
    DbUsecaseHandler _dbUsecaseHandler;
    SyncUsecaseHandler _syncUsecaseHandler;
    protected AppDatabase _db;
    protected Context _context;
    protected Boolean _syncToBePlanned = false;
    protected int _syncOngoing = 0;
    protected Boolean _nextGetIsForDisplay = false;

    SynchroHandler(Context ictx, DbUsecaseHandler iDbUsecaseHandler, SyncUsecaseHandler iSyncUsecaseHandler){
        _dbUsecaseHandler = iDbUsecaseHandler;
        _syncUsecaseHandler = iSyncUsecaseHandler;
        _context = ictx;

        _db = Room.databaseBuilder(ictx.getApplicationContext(),
                AppDatabase.class, "localcache")
                .addMigrations(AppDatabase.MIGRATION_1_2)
                .addMigrations(AppDatabase.MIGRATION_2_3)
                .addMigrations(AppDatabase.MIGRATION_3_4)
                .build();
    }

    public void syncPrioZero(){
        if(_syncOngoing == 0) {
            addOneSyncOngoing();
            _dbUsecaseHandler.callSyncActionRetrieveUsecase(_db, new SyncActionListInterface() {
                @Override
                public void handle(List<SyncAction> syncActions) {
                    for (SyncAction sa : syncActions) {
                        if (sa.priority.compareTo("0") == 0) {
                            executeAction(sa);
                        }
                    }
                    removeOneSyncOngoing();
                }

            });
        }
        else
        {
            _syncToBePlanned = true;
        }
    }

    public void syncPrioN(final int level){
        if(_syncOngoing == 0) {
            addOneSyncOngoing();
            final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(_context);
            final Integer urldelay = Integer.parseInt(settings.getString("list_delay_sync", "0"));
            _dbUsecaseHandler.callSyncActionRetrieveUsecase(_db, new SyncActionListInterface() {
                @Override
                public void handle(List<SyncAction> syncActions) {
                    try {
                        for (SyncAction sa : syncActions) {
                            if (sa.priority.compareTo(""+level) == 0) {
                                executeAction(sa);
                            }
                                TimeUnit.SECONDS.sleep(urldelay);
                        }

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    finally {
                        removeOneSyncOngoing();
                    }
                }

            });
        }
        else
        {
            _syncToBePlanned = true;
        }
    }

    private void executeAction(final SyncAction sa) {
        if(sa.verb.compareTo("PUT")==0){
            WikiCacheUiOrchestrator.instance()._logs.add("Put page "+sa.name+" to server");
            addOneSyncOngoing();
            _syncUsecaseHandler.callPageGetInfoUsecase(sa.name, _context, new SyncUsecaseCallbackInterface() {
                @Override
                public void processResultsList(ArrayList<String> iXmlrpcResults) {
                    // compare the current version, to the one we had in local
                    String server_rev_tmp = "";
                    for(String r : iXmlrpcResults) {
                        Log.d("DEBUG", "version:"+r);
                        String pageinfo = r.replace("{","").replace("}","").replace(", ",",");
                        String[] parts = pageinfo.split(",");
                        for (String a : parts) {
                            if (a.startsWith("version=")) {
                                server_rev_tmp = a.substring(8);
                            }
                        }
                    }
                    final String server_rev = server_rev_tmp;

                    final SyncUsecaseCallbackInterface callbackUploadText = new SyncUsecaseCallbackInterface() {
                        @Override
                        public void processResultsList(ArrayList<String> iXmlrpcResults) {
                            _dbUsecaseHandler.callSyncActionDeleteUsecase(_db, sa, new DbCallbackInterface() {
                                @Override
                                public void onceDone() {
                                    removeOneSyncOngoing(); // remove the synchro from PUT page
                                }
                            });
                            //  try to display the new html version in _webview
                            WikiCacheUiOrchestrator.instance()._logs.add("page "+sa.name+" should be updated, get it from server");
                            Log.d(TAG, "page "+sa.name+" should be updated, get it from server");
                            _nextGetIsForDisplay = true;
                            SyncAction syncAction = new SyncAction();
                            syncAction.priority = "0";
                            syncAction.verb = "GET";
                            syncAction.name = sa.name;
                            syncAction.rev = "0";
                            syncAction.data = "";
                            _dbUsecaseHandler.callSyncActionInsertUsecase(_db, syncAction, new DbCallbackInterface() {
                                @Override
                                public void onceDone() {
                                    syncPrioZero();
                                }
                            });
                        }
                        @Override
                        public void processResultsBinary(byte[] iXmlrpcResults) {}
                    };
                    if(sa.rev.compareTo(server_rev) != 0) {
                        Log.d("DEBUG", "need a conflict handling: " + sa.rev + " - " + server_rev);
                        _syncUsecaseHandler.callPageTextDownloadUsecase(sa.name, _context, false, new SyncUsecaseCallbackInterface(){
                            @Override
                            public void processResultsList(ArrayList<String> iXmlrpcResults) {
                                if(iXmlrpcResults.size()==1) {
                                    WikiCacheUiOrchestrator.instance()._logs.add("page " + sa.name + " retrieved text from server to merge conflict");
                                    String newContent = sa.data;
                                    newContent += "\n\n----\n\nconflict between versions "+sa.rev+" and "+server_rev+"\n\n----\n\n";
                                    newContent += iXmlrpcResults.get(0);
                                    _syncUsecaseHandler.callPageTextUploadUsecase(sa.name, newContent, _context, callbackUploadText);
                                    WikiCacheUiOrchestrator.instance().savePageTextInCache(sa.name, newContent);
                                }
                            }
                            @Override
                            public void processResultsBinary(byte[] iXmlrpcResults) {}
                        });
                    }
                    else // no conflict: upload the new content
                        _syncUsecaseHandler.callPageTextUploadUsecase(sa.name, sa.data, _context, callbackUploadText);
                }
                @Override
                public void processResultsBinary(byte[] iXmlrpcResults) {}
            });

        }
        else if(sa.verb.compareTo("GET")==0){
            WikiCacheUiOrchestrator.instance()._logs.add("Get page "+sa.name+" from server");
            addOneSyncOngoing();

            _syncUsecaseHandler.callPageGetInfoUsecase(sa.name, _context, new SyncUsecaseCallbackInterface() {
                @Override
                public void processResultsList(ArrayList<String> iXmlrpcResults) {
                    // compare the current version, to the one we had in local
                    String server_rev_tmp = "";
                    for (String r : iXmlrpcResults) {
                        String pageinfo = r.replace("{", "").replace("}", "").replace(", ", ",");
                        String[] parts = pageinfo.split(",");
                        for (String a : parts) {
                            if (a.startsWith("version=")) {
                                server_rev_tmp = a.substring(8);
                            }
                        }
                    }
                    WikiPage aPageItem = WikiCacheUiOrchestrator.instance()._wikiPageList._pages.remove(sa.name);
                    aPageItem._latest_version = server_rev_tmp;
                    WikiCacheUiOrchestrator.instance()._wikiPageList._pages.put(sa.name, aPageItem);
                    _syncUsecaseHandler.callPageHtmlDownloadUsecase(sa.name, _context, _nextGetIsForDisplay, new SyncUsecaseCallbackInterface() {
                        @Override
                        public void processResultsList(ArrayList<String> iXmlrpcResults) {
                            if(_nextGetIsForDisplay){
                                _nextGetIsForDisplay = false;
                            }
                            _dbUsecaseHandler.callSyncActionDeleteUsecase(_db, sa, new DbCallbackInterface() {
                                @Override
                                public void onceDone() {
                                    removeOneSyncOngoing(); // remove the synchro from PUT page
                                }
                            });
                        }
                        @Override
                        public void processResultsBinary(byte[] iXmlrpcResults) {}
                    });
                }
                @Override
                public void processResultsBinary(byte[] iXmlrpcResults) {}
            });
        }
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
    }
}
