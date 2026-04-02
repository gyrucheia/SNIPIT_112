package com.example.snipit.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.snipit.app.R;
import com.example.snipit.app.models.AiChatSession;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AiSessionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 1;
    private static final int TYPE_SESSION = 2;

    public interface Listener {
        void onSessionClicked(AiChatSession session);
    }

    private final List<Object> rows = new ArrayList<>();
    private final Listener listener;
    private final SimpleDateFormat fmt =
            new SimpleDateFormat("MMM d · h:mm a", Locale.getDefault());

    public AiSessionAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<AiChatSession> sessions) {
        rows.clear();
        if (sessions != null && !sessions.isEmpty()) {
            String lastBucket = null;
            for (AiChatSession s : sessions) {
                String bucket = bucketLabel(s.updatedAt);
                if (!bucket.equals(lastBucket)) {
                    rows.add(bucket);
                    lastBucket = bucket;
                }
                rows.add(s);
            }
        }
        notifyDataSetChanged();
    }

    /** Matches HTML mock: Today / Yesterday / This week / Earlier. */
    private String bucketLabel(long updatedAt) {
        Calendar u = Calendar.getInstance();
        u.setTimeInMillis(updatedAt);
        Calendar today = Calendar.getInstance();
        if (sameDay(u, today)) {
            return "Today";
        }
        Calendar y = Calendar.getInstance();
        y.add(Calendar.DAY_OF_YEAR, -1);
        if (sameDay(u, y)) {
            return "Yesterday";
        }
        Calendar weekStart = Calendar.getInstance();
        weekStart.set(Calendar.DAY_OF_WEEK, weekStart.getFirstDayOfWeek());
        stripTime(weekStart);
        Calendar uDay = (Calendar) u.clone();
        stripTime(uDay);
        if (!uDay.before(weekStart)) {
            return "This week";
        }
        return "Earlier";
    }

    private static void stripTime(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }

    private static boolean sameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position) instanceof String ? TYPE_HEADER : TYPE_SESSION;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View v =
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_ai_session_header, parent, false);
            return new HeaderVH(v);
        }
        View v =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_ai_session, parent, false);
        return new SessionVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).title.setText((String) rows.get(position));
        } else {
            AiChatSession s = (AiChatSession) rows.get(position);
            SessionVH h = (SessionVH) holder;
            h.title.setText(s.title != null ? s.title : "Chat");
            h.time.setText(fmt.format(new Date(s.updatedAt)));
            h.itemView.setOnClickListener(v -> listener.onSessionClicked(s));
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class HeaderVH extends RecyclerView.ViewHolder {
        final TextView title;

        HeaderVH(View v) {
            super(v);
            title = (TextView) v;
        }
    }

    static class SessionVH extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView time;

        SessionVH(View v) {
            super(v);
            title = v.findViewById(R.id.hist_title);
            time = v.findViewById(R.id.hist_time);
        }
    }
}
