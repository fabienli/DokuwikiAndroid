package com.fabienli.dokuwiki.db;


import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface PageDao {
    @Query("SELECT * FROM page ORDER BY pagename")
    List<Page> getAll();

    @Query("SELECT * FROM page WHERE pagename IN (:pagename) LIMIT 1")
    Page findByName(String pagename);

    @Query("SELECT * FROM page ORDER by pagename")
    List<Page> selectAll();

    @Query("SELECT * FROM page WHERE text LIKE :search or html LIKE :search")
    List<Page> search(String search);

    @Query("UPDATE page SET html=:html WHERE pagename IN (:pagename)")
    void updateHtml(String pagename, String html);

    @Query("UPDATE page SET text=:text WHERE pagename IN (:pagename)")
    void updateText(String pagename, String text);

    @Query("UPDATE page SET rev=:version WHERE pagename IN (:pagename)")
    void updateVersion(String pagename, String version);

    @Insert
    void insertAll(Page... pages);

    @Update
    void update(Page pages);

    @Delete
    void delete(Page pages);
}
