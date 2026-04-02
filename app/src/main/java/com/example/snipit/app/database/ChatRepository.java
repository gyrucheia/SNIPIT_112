package com.example.snipit.app.database;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import com.example.snipit.app.models.AiChatMessage;
import com.example.snipit.app.models.AiChatSession;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatRepository {

    private final ChatDao chatDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public ChatRepository(Application app) {
        chatDao = SnipDatabase.getInstance(app).chatDao();
    }

    public LiveData<List<AiChatSession>> sessions() {
        return chatDao.observeSessions();
    }

    public void createSession(String title, java.util.function.Consumer<Long> onId) {
        executor.execute(
                () -> {
                    AiChatSession s = new AiChatSession();
                    s.title = title != null ? title : "Chat";
                    s.updatedAt = System.currentTimeMillis();
                    long id = chatDao.insertSession(s);
                    main.post(() -> onId.accept(id));
                });
    }

    public void saveUserMessage(long sessionId, String text, Runnable done) {
        executor.execute(
                () -> {
                    AiChatMessage m = new AiChatMessage();
                    m.sessionId = sessionId;
                    m.role = "user";
                    m.body = text;
                    m.createdAt = System.currentTimeMillis();
                    chatDao.insertMessage(m);
                    chatDao.touchSession(sessionId, System.currentTimeMillis());
                    if (done != null) main.post(done);
                });
    }

    public void saveAssistantMessage(long sessionId, String text, Runnable done) {
        executor.execute(
                () -> {
                    AiChatMessage m = new AiChatMessage();
                    m.sessionId = sessionId;
                    m.role = "assistant";
                    m.body = text;
                    m.createdAt = System.currentTimeMillis();
                    chatDao.insertMessage(m);
                    chatDao.touchSession(sessionId, System.currentTimeMillis());
                    if (done != null) main.post(done);
                });
    }

    public void loadMessages(long sessionId, java.util.function.Consumer<List<AiChatMessage>> onResult) {
        executor.execute(
                () -> {
                    List<AiChatMessage> list = chatDao.getMessagesForSession(sessionId);
                    main.post(() -> onResult.accept(list));
                });
    }

    public void clearAllHistory(Runnable done) {
        executor.execute(
                () -> {
                    chatDao.deleteAllMessages();
                    chatDao.deleteAllSessions();
                    if (done != null) main.post(done);
                });
    }
}
