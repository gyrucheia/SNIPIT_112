package com.example.snipit.app.models;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "ai_messages",
        foreignKeys =
                @ForeignKey(
                        entity = AiChatSession.class,
                        parentColumns = "id",
                        childColumns = "sessionId",
                        onDelete = ForeignKey.CASCADE),
        indices = {@Index("sessionId")})
public class AiChatMessage {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long sessionId;
    /** "user" or "assistant" */
    public String role;

    public String body;
    public long createdAt;
}
