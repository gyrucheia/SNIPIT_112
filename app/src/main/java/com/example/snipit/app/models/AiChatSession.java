package com.example.snipit.app.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "ai_sessions")
public class AiChatSession {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String title;
    public long updatedAt;
    public String firebaseId;
}
