package com.fabienli.dokuwiki.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Page.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract PageDao pageDao();
}

