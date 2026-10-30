package com.soum91.tunertv;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.StringReader;

public final class M3uParserTest {
    @Test public void parsesChannelGuideAndGroups() throws Exception {
        String file = "#EXTM3U x-tvg-url=\"https://example.test/guide.xml\"\n"
                + "#EXTINF:-1 tvg-id=\"news.example\" tvg-logo=\"https://example.test/logo.png\" group-title=\"News\",Example News\n"
                + "https://example.test/live/1.m3u8\n";
        M3uParser.Result result = M3uParser.parse(new StringReader(file));
        assertEquals("https://example.test/guide.xml", result.guideUrl);
        assertEquals(1, result.channels.size());
        assertEquals("Example News", result.channels.get(0).name);
        assertEquals("News", result.channels.get(0).group);
        assertEquals("news.example", result.channels.get(0).epgId);
    }

    @Test public void skipsCommentsAndUnsupportedSchemes() throws Exception {
        String file = "#EXTM3U\n#EXTINF:-1,Test\nfile:///private/stream.ts\n"
                + "#EXTINF:-1,Live\nhttp://example.test/stream.ts\n";
        M3uParser.Result result = M3uParser.parse(new StringReader(file));
        assertEquals(1, result.channels.size());
        assertEquals("Live", result.channels.get(0).name);
    }
}

