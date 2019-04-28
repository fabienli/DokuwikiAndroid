package com.fabienli.dokuwiki.db;


import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Page {

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "pagename")
    public String pagename;

    @ColumnInfo(name = "html")
    public String html;

    @ColumnInfo(name = "text")
    public String text;

    @ColumnInfo(name = "rev")
    public String rev;
}
