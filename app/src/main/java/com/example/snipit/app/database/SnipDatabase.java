package com.example.snipit.app.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.snipit.app.models.AiChatMessage;
import com.example.snipit.app.models.AiChatSession;
import com.example.snipit.app.models.Snip;

@Database(
        entities = {Snip.class, AiChatSession.class, AiChatMessage.class},
        version = 4,
        exportSchema = false)
public abstract class SnipDatabase extends RoomDatabase {

    private static volatile SnipDatabase INSTANCE;

    public abstract SnipDao snipDao();

    public abstract ChatDao chatDao();

    public static SnipDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (SnipDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    SnipDatabase.class,
                                    "snipit_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
