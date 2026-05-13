package com.example.snipit.app.models;

public class HandbookEntry {
    private String category;
    private String command;
    private String summary;
    private String documentation;

    public HandbookEntry(String category, String command, String summary, String documentation) {
        this.category = category;
        this.command = command;
        this.summary = summary;
        this.documentation = documentation;
    }

    public String getCategory() { return category; }
    public String getCommand() { return command; }
    public String getSummary() { return summary; }
    public String getDocumentation() { return documentation; }
}
