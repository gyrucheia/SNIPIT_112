package com.example.snipit.app.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.snipit.app.models.AiChatMessage;
import com.example.snipit.app.models.AiChatSession;
import java.util.List;

@Dao
public interface ChatDao {

    @Insert
    long insertSession(AiChatSession session);

    @Insert
    long insertMessage(AiChatMessage message);

    @Query("UPDATE ai_sessions SET updatedAt = :t WHERE id = :id")
    void touchSession(long id, long t);

    @Query("SELECT * FROM ai_sessions ORDER BY updatedAt DESC")
    LiveData<List<AiChatSession>> observeSessions();

    @Query("SELECT * FROM ai_messages WHERE sessionId = :sid ORDER BY createdAt ASC")
    List<AiChatMessage> getMessagesForSession(long sid);

    @Query("DELETE FROM ai_messages")
    void deleteAllMessages();

    @Query("DELETE FROM ai_sessions")
    void deleteAllSessions();
}
