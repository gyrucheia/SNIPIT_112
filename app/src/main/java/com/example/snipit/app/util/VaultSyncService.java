package com.example.snipit.app.util;

import android.content.Context;
import android.util.Log;
import com.example.snipit.app.database.SnipDao;
import com.example.snipit.app.database.SnipDatabase;
import com.example.snipit.app.models.Snip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VaultSyncService {

    private static final String TAG = "VaultSyncService";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private SnipDao dao;

    public VaultSyncService(Context context) {
        try {
            dao = SnipDatabase.getInstance(context).snipDao();
        } catch (Exception e) {
            Log.e(TAG, "DAO init failed", e);
        }
    }

    public void sync(Runnable onDone) {
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null || dao == null) {
                if (onDone != null) onDone.run();
                return;
            }

            executor.execute(() -> {
                try {
                    pullFromFirebase(user.getUid(), () -> {
                        pushNewLocals(user.getUid(), onDone);
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Sync executor failed", e);
                    if (onDone != null) onDone.run();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Sync start failed", e);
            if (onDone != null) onDone.run();
        }
    }

    private void pullFromFirebase(String uid, Runnable next) {
        try {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(uid).child("snippets");
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    executor.execute(() -> {
                        try {
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                String rid = ds.getKey();
                                Object val = ds.getValue();
                                if (!(val instanceof Map)) continue;
                                
                                Map<String, Object> data = (Map<String, Object>) val;
                                Snip existing = dao.getSnipByRemoteId(rid);
                                if (existing == null) {
                                    Snip s = new Snip();
                                    s.remoteId = rid;
                                    s.title = (String) data.get("title");
                                    s.code = (String) data.get("code");
                                    s.language = (String) data.get("language");
                                    s.tags = (String) data.get("tags");
                                    dao.insert(s);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Pull processing failed", e);
                        }
                        if (next != null) next.run();
                    });
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    if (next != null) next.run();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Pull start failed", e);
            if (next != null) next.run();
        }
    }

    private void pushNewLocals(String uid, Runnable onDone) {
        try {
            List<Snip> locals = dao.getSnipsWithoutRemoteId();
            if (locals == null || locals.isEmpty()) {
                if (onDone != null) onDone.run();
                return;
            }

            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(uid).child("snippets");
            for (Snip s : locals) {
                DatabaseReference newRef = ref.push();
                String rid = newRef.getKey();
                
                Map<String, Object> data = new HashMap<>();
                data.put("title", s.title != null ? s.title : "");
                data.put("code", s.code != null ? s.code : "");
                data.put("language", s.language != null ? s.language : "");
                data.put("tags", s.tags != null ? s.tags : "");
                data.put("updated", new java.util.Date().toString());

                newRef.setValue(data);
                s.remoteId = rid;
                dao.update(s);
            }
        } catch (Exception e) {
            Log.e(TAG, "Push failed", e);
        }
        if (onDone != null) onDone.run();
    }

    public void deleteRemote(String rid) {
        try {
            if (rid == null) return;
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;
            FirebaseDatabase.getInstance().getReference("users")
                    .child(user.getUid())
                    .child("snippets")
                    .child(rid)
                    .removeValue();
        } catch (Exception e) {
            Log.e(TAG, "Delete remote failed", e);
        }
    }

    public void updateRemote(Snip s) {
        try {
            if (s == null || s.remoteId == null) return;
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;
            
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users")
                    .child(user.getUid())
                    .child("snippets")
                    .child(s.remoteId);
                    
            Map<String, Object> data = new HashMap<>();
            data.put("title", s.title);
            data.put("code", s.code);
            data.put("language", s.language);
            data.put("tags", s.tags);
            data.put("updated", new java.util.Date().toString());
            ref.updateChildren(data);
        } catch (Exception e) {
            Log.e(TAG, "Update remote failed", e);
        }
    }
}
