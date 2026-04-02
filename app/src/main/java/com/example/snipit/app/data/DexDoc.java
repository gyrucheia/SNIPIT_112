package com.example.snipit.app.data;

/** One Dev-Dex CLI entry with expandable offline documentation. */
public final class DexDoc {

    public final String command;
    public final String summary;
    public final String documentation;

    public DexDoc(String command, String summary, String documentation) {
        this.command = command;
        this.summary = summary;
        this.documentation = documentation;
    }
}
