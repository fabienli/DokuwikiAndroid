package com.fabienli.dokuwiki.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MediaDao {
    @Query("SELECT * FROM media")
    List<Media> getAll();

    @Query("SELECT * FROM media WHERE id IN (:id) LIMIT 1")
    Media findByName(String id);

    @Insert
    void insertAll(Media... medias);

    @Update
    void updateAll(Media... medias);

    @Delete
    void delete(Media media);
}
