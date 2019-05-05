package com.fabienli.dokuwiki.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Page.class, Media.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {
    public abstract PageDao pageDao();
    public abstract MediaDao mediaDao();

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // create the Media table
            database.execSQL(
                    "CREATE TABLE media (id TEXT not null, file TEXT NOT NULL, size TEXT, mtime TEXT, lastModified TEXT, isimg TEXT, PRIMARY KEY(id))");
        }
    };

}

