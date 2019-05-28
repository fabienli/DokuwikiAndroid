package com.fabienli.dokuwiki.db;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface SyncActionDao {

    @Query("SELECT * FROM syncaction ORDER BY priority")
    List<SyncAction> getAll();


    @Query("SELECT * FROM syncaction WHERE priority = :prio")
    List<SyncAction> getAllPriority(String prio);

    @Query("SELECT * FROM syncaction WHERE priority = :prio AND verb = :verb AND name= :name LIMIT 1")
    SyncAction findUnique(String prio, String verb, String name);

    @Insert
    void insertAll(SyncAction... syncActions);

    @Delete
    void deleteAll(SyncAction... syncActions);

    @Query("DELETE FROM syncaction WHERE priority = :prio")
    void deleteLevel(String prio);
}
