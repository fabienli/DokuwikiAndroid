package com.fabienli.dokuwiki.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Page.class, Media.class, SyncAction.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    public abstract PageDao pageDao();
    public abstract MediaDao mediaDao();
    public abstract SyncActionDao syncActionDao();

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // create the Media table
            database.execSQL(
                    "CREATE TABLE media (id TEXT not null, file TEXT NOT NULL, size TEXT, mtime TEXT, lastModified TEXT, isimg TEXT, PRIMARY KEY(id))");
        }
    };


    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // create the Media table
            database.execSQL(
                    "CREATE TABLE syncAction (verb TEXT not null, name TEXT NOT NULL, priority TEXT NOT NULL, data TEXT, PRIMARY KEY(priority, verb, name))");
        }
    };

}

