package com.soum91.tunertv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Streaming M3U parser; preserves signed channel URLs only in memory. */
public final class M3uParser {
    private static final Pattern ATTRIBUTE = Pattern.compile("([A-Za-z0-9_-]+)=\"([^\"]*)\"");
    private M3uParser() { }

    public static final class Result {
        public final List<Channel> channels;
        public final String guideUrl;
        Result(List<Channel> channels, String guideUrl) { this.channels = channels; this.guideUrl = guideUrl; }
    }

    public static Result parse(Reader reader) throws IOException {
        List<Channel> result = new ArrayList<>();
        BufferedReader lines = new BufferedReader(reader);
        String line, name = "", group = "Other", icon = "", tvgId = "", guide = "";
        int index = 0;
        while ((line = lines.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("#EXTM3U")) {
                Matcher attrs = ATTRIBUTE.matcher(line);
                while (attrs.find()) {
                    if (attrs.group(1).equals("x-tvg-url") || attrs.group(1).equals("url-tvg")) guide = attrs.group(2);
                }
            } else if (line.startsWith("#EXTINF")) {
                int comma = line.lastIndexOf(',');
                name = comma >= 0 ? line.substring(comma + 1).trim() : "Unknown channel";
                group = "Other"; icon = ""; tvgId = "";
                Matcher attrs = ATTRIBUTE.matcher(comma >= 0 ? line.substring(0, comma) : line);
                while (attrs.find()) {
                    switch (attrs.group(1)) {
                        case "group-title": group = attrs.group(2); break;
                        case "tvg-id": tvgId = attrs.group(2); break;
                        case "tvg-logo": icon = attrs.group(2); break;
                        case "tvg-name": if (name.isEmpty()) name = attrs.group(2); break;
                        default: break;
                    }
                }
            } else if (!line.isEmpty() && !line.startsWith("#") && (line.startsWith("https://") || line.startsWith("http://"))) {
                result.add(new Channel(Channel.Kind.LIVE, "m3u-" + index++, name, group, line, tvgId, icon, ""));
                name = ""; group = "Other"; icon = ""; tvgId = "";
            }
        }
        return new Result(result, guide);
    }
}

