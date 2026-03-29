package com.example.snipit.app.database;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import com.example.snipit.app.models.Snip;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SnipRepository {

    private final SnipDao snipDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public SnipRepository(Application application) {
        SnipDatabase db = SnipDatabase.getInstance(application);
        snipDao = db.snipDao();
    }

    /** Seeds demo rows once when the vault is empty (offline UI walkthrough). */
    public void seedIfEmpty() {
        executor.execute(() -> {
            if (snipDao.getTotalCount() > 0) return;
            Snip a = new Snip();
            a.title = "Firebase Auth Init";
            a.language = "Java";
            a.code =
                    "FirebaseApp.initializeApp(this);\n"
                            + "FirebaseAuth auth = FirebaseAuth.getInstance();";
            a.tags = "#Java,#Firebase";
            snipDao.insert(a);

            Snip b = new Snip();
            b.title = "RecyclerView adapter";
            b.language = "Kotlin";
            b.code =
                    "class SnipAdapter(private val items: List<Snip>) :\n"
                            + "    RecyclerView.Adapter<SnipAdapter.VH>() { }";
            b.tags = "#Kotlin,#Android";
            snipDao.insert(b);

            Snip c = new Snip();
            c.title = "Git: undo last commit";
            c.language = "CLI";
            c.code = "git reset --soft HEAD~1";
            c.tags = "#Git,#CLI";
            snipDao.insert(c);
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
        executor.execute(() -> snipDao.insert(snip));
    }

    public void update(Snip snip) {
        executor.execute(() -> snipDao.update(snip));
    }

    public void delete(Snip snip) {
        executor.execute(() -> snipDao.delete(snip));
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
}
