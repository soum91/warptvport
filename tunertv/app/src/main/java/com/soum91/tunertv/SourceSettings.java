package com.soum91.tunertv;

import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;

public final class SourceSettings {
    public enum Type { XTREAM, M3U }
    public final Type type;
    public final String server;
    public final String username;
    public final String password;
    public final String epgUrl;
    public final boolean useHls;

    public SourceSettings(Type type, String server, String username, String password,
                          String epgUrl, boolean useHls) {
        this.type = type;
        this.server = server == null ? "" : server.trim();
        this.username = username == null ? "" : username;
        this.password = password == null ? "" : password;
        this.epgUrl = epgUrl == null ? "" : epgUrl.trim();
        this.useHls = useHls;
    }

    public String base() { return server.replaceAll("/+$", ""); }
    public boolean valid() {
        if (!(server.startsWith("https://") || server.startsWith("http://"))) return false;
        return type == Type.M3U || (!username.isEmpty() && !password.isEmpty());
    }

    public static String encoded(String input) {
        try {
            return URLEncoder.encode(input, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException error) {
            throw new IllegalStateException("UTF-8 is unavailable", error);
        }
    }

    public String api(String action) {
        return base() + "/player_api.php?username=" + encoded(username)
                + "&password=" + encoded(password) + "&action=" + encoded(action);
    }

    public String stream(Channel.Kind kind, String id, String extension) {
        String category = kind == Channel.Kind.MOVIE ? "movie"
                : (kind == Channel.Kind.EPISODE ? "series" : "live");
        String ext = kind == Channel.Kind.LIVE ? (useHls ? "m3u8" : "ts") : extension;
        if (ext == null || !ext.matches("[a-zA-Z0-9]{1,8}")) ext = "mp4";
        return base() + "/" + category + "/" + encoded(username) + "/"
                + encoded(password) + "/" + encoded(id) + "." + ext;
    }

    public String xmltv() {
        return epgUrl.isEmpty()
                ? base() + "/xmltv.php?username=" + encoded(username) + "&password=" + encoded(password)
                : epgUrl;
    }
}

