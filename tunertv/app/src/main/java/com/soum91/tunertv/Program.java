package com.soum91.tunertv;

public final class Program {
    public final String title;
    public final long startMillis;
    public final long endMillis;
    public final String description;

    public Program(String title, long startMillis, long endMillis, String description) {
        this.title = title == null || title.isEmpty() ? "Programme unavailable" : title;
        this.startMillis = startMillis;
        this.endMillis = endMillis;
        this.description = description == null ? "" : description;
    }
}

