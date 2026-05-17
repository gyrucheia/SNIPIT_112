package com.example.snipit.app.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "snippets")
public class Snip {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String code;
    public String language;
    public String tags;
    public long dateCreated;
    public int usageCount;
    public boolean isPinned;
    public String remoteId;
    public long lastModified;
    public boolean isDirty;

    public Snip() {
        this.dateCreated = System.currentTimeMillis();
        this.lastModified = System.currentTimeMillis();
        this.usageCount = 0;
        this.isPinned = false;
        this.isDirty = true;
    }

    public String[] getTagArray() {
        if (tags == null || tags.isEmpty()) return new String[0];
        return tags.split(",");
    }

    public String getFormattedDate() {
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(dateCreated));
    }
}
