package com.fabienli.dokuwiki.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Media {

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "id")
    public String id;

    @NonNull
    @ColumnInfo(name = "file")
    public String file;

    @ColumnInfo(name = "size")
    public String size;

    @ColumnInfo(name = "mtime")
    public String mtime;

    @ColumnInfo(name = "lastModified")
    public String lastModified;

    @ColumnInfo(name = "isimg")
    public String isimg;
}
