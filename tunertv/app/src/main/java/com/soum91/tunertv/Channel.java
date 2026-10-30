package com.soum91.tunertv;

/** One television channel, movie, series or episode. The URL remains in memory only. */
public final class Channel {
    public enum Kind { LIVE, MOVIE, SERIES, EPISODE }

    public final Kind kind;
    public final String id;
    public final String name;
    public final String group;
    public final String url;
    public final String epgId;
    public final String artwork;
    public final String extension;

    public Channel(Kind kind, String id, String name, String group, String url,
                   String epgId, String artwork, String extension) {
        this.kind = kind;
        this.id = id == null ? "" : id;
        this.name = name == null ? "Untitled" : name;
        this.group = group == null ? "Other" : group;
        this.url = url == null ? "" : url;
        this.epgId = epgId == null ? "" : epgId;
        this.artwork = artwork == null ? "" : artwork;
        this.extension = extension == null ? "" : extension;
    }

    public String display() { return name; }
}

