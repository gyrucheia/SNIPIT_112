package com.example.snipit.app.database;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import com.example.snipit.app.models.AiChatMessage;
import com.example.snipit.app.models.AiChatSession;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                    s.firebaseId = String.valueOf(System.currentTimeMillis());
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
                    
                    syncMessageToFirebase(sessionId, m);
                    
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
                    
                    syncMessageToFirebase(sessionId, m);
                    
                    if (done != null) main.post(done);
                });
    }

    private void syncMessageToFirebase(long sessionId, AiChatMessage msg) {
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;

            AiChatSession session = chatDao.getSessionById(sessionId);
            if (session == null || session.firebaseId == null) return;

            DatabaseReference chatRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(user.getUid())
                    .child("aiChat")
                    .child(session.firebaseId);

            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("role", msg.role);
            messageMap.put("body", msg.body);
            messageMap.put("timestamp", new java.util.Date(msg.createdAt).toString());

            // Add to messages array in Firebase
            chatRef.child("messages").get().addOnSuccessListener(snapshot -> {
                List<Object> messages = new ArrayList<>();
                if (snapshot.exists()) {
                    messages = (List<Object>) snapshot.getValue();
                }
                messages.add(messageMap);
                
                Map<String, Object> updates = new HashMap<>();
                updates.put("messages", messages);
                updates.put("title", session.title);
                updates.put("lastUpdated", new java.util.Date().toString());
                chatRef.updateChildren(updates);
            });

        } catch (Exception e) {
            android.util.Log.e("ChatRepo", "Firebase sync failed", e);
        }
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
                    
                    // Also clear Firebase
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        FirebaseDatabase.getInstance()
                                .getReference("users")
                                .child(user.getUid())
                                .child("aiChat")
                                .removeValue();
                    }
                    
                    if (done != null) main.post(done);
                });
    }
}
