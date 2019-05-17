package com.fabienli.dokuwiki;

import android.content.Context;
import android.util.Log;

import com.fabienli.dokuwiki.db.AppDatabase;
import com.fabienli.dokuwiki.db.DbCallbackInterface;
import com.fabienli.dokuwiki.db.DbUsecaseHandler;
import com.fabienli.dokuwiki.db.SyncAction;
import com.fabienli.dokuwiki.db.SyncActionListInterface;
import com.fabienli.dokuwiki.sync.SyncUsecaseCallbackInterface;
import com.fabienli.dokuwiki.sync.SyncUsecaseHandler;

import java.util.ArrayList;
import java.util.List;

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

    private void executeAction(final SyncAction sa) {
        if(sa.verb.compareTo("PUT")==0){
            WikiCacheUiOrchestrator.instance()._logs.add("Put page "+sa.name+" to server");
            addOneSyncOngoing();
            _syncUsecaseHandler.callPageTextUploadUsecase(sa.name, sa.data, _context, new SyncUsecaseCallbackInterface() {
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
            });
        }
        else if(sa.verb.compareTo("GET")==0){
            WikiCacheUiOrchestrator.instance()._logs.add("Get page "+sa.name+" from server");
            addOneSyncOngoing();
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
