package com.example.snipit.app.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.snipit.app.models.Snip;
import java.util.List;

@Dao
public interface SnipDao {

    @Insert
    long insert(Snip snip);

    @Update
    void update(Snip snip);

    @Delete
    void delete(Snip snip);

    @Query("SELECT * FROM snippets ORDER BY isPinned DESC, dateCreated DESC")
    LiveData<List<Snip>> getAllSnips();

    @Query("SELECT * FROM snippets WHERE tags LIKE '%' || :tag || '%' ORDER BY dateCreated DESC")
    LiveData<List<Snip>> getSnipsByTag(String tag);

    @Query("SELECT * FROM snippets WHERE title LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%' ORDER BY dateCreated DESC")
    LiveData<List<Snip>> searchSnips(String query);

    @Query("SELECT * FROM snippets WHERE id = :id LIMIT 1")
    Snip getSnipById(int id);

    @Query("SELECT * FROM snippets ORDER BY dateCreated DESC LIMIT 1")
    Snip getLatestSnip();

    @Query("UPDATE snippets SET usageCount = usageCount + 1 WHERE id = :id")
    void incrementUsage(int id);

    @Query("SELECT COUNT(*) FROM snippets")
    int getTotalCount();

    @Query("SELECT COUNT(DISTINCT language) FROM snippets")
    int getLanguageCount();
}
