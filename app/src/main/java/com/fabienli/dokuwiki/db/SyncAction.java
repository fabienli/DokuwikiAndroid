package com.fabienli.dokuwiki.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(primaryKeys = {"priority", "verb", "name"})
public class SyncAction {

    @NonNull
    @ColumnInfo(name = "priority")
    public String priority;

    @NonNull
    @ColumnInfo(name = "verb")
    public String verb;

    @NonNull
    @ColumnInfo(name = "name")
    public String name;

    @NonNull
    @ColumnInfo(name = "rev")
    public String rev;


    @ColumnInfo(name = "data")
    public String data;

    public String toText() {
        return priority+ " - "+ verb + " : " + name + " (" + rev + ")";
    }
}
