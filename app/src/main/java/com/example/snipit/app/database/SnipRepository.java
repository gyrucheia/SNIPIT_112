package com.example.snipit.app.database;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import com.example.snipit.app.models.Snip;
import com.example.snipit.app.util.VaultSyncService;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SnipRepository {

    private final SnipDao snipDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final VaultSyncService syncService;

    public SnipRepository(Application application) {
        SnipDatabase db = SnipDatabase.getInstance(application);
        snipDao = db.snipDao();
        syncService = new VaultSyncService(application);
    }

    public void sync(Runnable onDone) {
        syncService.sync(onDone);
    }

    /** Seeds demo rows once when the vault is empty (offline UI walkthrough). */
    public void seedIfEmpty() {
        executor.execute(() -> {
            if (snipDao.getTotalCount() > 0) return;
            Snip a = new Snip();
            a.title = "Firebase Auth Init";
            a.language = "Java";
            a.code = "FirebaseApp.initializeApp(this);\n" + "FirebaseAuth auth = FirebaseAuth.getInstance();";
            a.tags = "#Java,#Firebase";
            snipDao.insert(a);
            // Optionally push these to remote as well if needed
        });
    }

    public void getSnipById(int id, java.util.function.Consumer<Snip> onResult) {
        executor.execute(() -> {
            Snip s = snipDao.getSnipById(id);
            mainHandler.post(() -> onResult.accept(s));
        });
    }

    public void getLatestSnip(java.util.function.Consumer<Snip> onResult) {
        executor.execute(() -> {
            Snip s = snipDao.getLatestSnip();
            mainHandler.post(() -> onResult.accept(s));
        });
    }

    public void insert(Snip snip) {
        executor.execute(() -> {
            snipDao.insert(snip);
            syncService.sync(null); // Simple sync after insert
        });
    }

    public void insert(Snip snip, Runnable onDone) {
        executor.execute(() -> {
            snipDao.insert(snip);
            syncService.sync(() -> {
                if (onDone != null) mainHandler.post(onDone);
            });
        });
    }

    public void getSnippetCount(java.util.function.Consumer<Integer> onResult) {
        executor.execute(() -> {
            int n = snipDao.getTotalCount();
            mainHandler.post(() -> onResult.accept(n));
        });
    }

    public void update(Snip snip) {
        executor.execute(() -> {
            snipDao.update(snip);
            syncService.updateRemote(snip);
        });
    }

    public void update(Snip snip, Runnable onDone) {
        executor.execute(() -> {
            snipDao.update(snip);
            syncService.updateRemote(snip);
            if (onDone != null) mainHandler.post(onDone);
        });
    }

    public void delete(Snip snip) {
        executor.execute(() -> {
            if (snip.remoteId != null) {
                syncService.deleteRemote(snip.remoteId);
            }
            snipDao.delete(snip);
        });
    }

    public void delete(Snip snip, Runnable onDone) {
        executor.execute(() -> {
            if (snip.remoteId != null) {
                syncService.deleteRemote(snip.remoteId);
            }
            snipDao.delete(snip);
            if (onDone != null) mainHandler.post(onDone);
        });
    }

    public LiveData<List<Snip>> getAllSnips() {
        return snipDao.getAllSnips();
    }

    public LiveData<List<Snip>> getSnipsByTag(String tag) {
        return snipDao.getSnipsByTag(tag);
    }

    public LiveData<List<Snip>> searchSnips(String query) {
        return snipDao.searchSnips(query);
    }

    public void incrementUsage(int id) {
        executor.execute(() -> snipDao.incrementUsage(id));
    }

    public void getRecentSnips(int limit, java.util.function.Consumer<List<Snip>> onResult) {
        executor.execute(() -> {
            List<Snip> snips = snipDao.getRecentSnipsSync(limit);
            mainHandler.post(() -> onResult.accept(snips));
        });
    }
}
