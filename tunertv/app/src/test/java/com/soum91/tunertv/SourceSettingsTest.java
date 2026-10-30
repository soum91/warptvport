package com.soum91.tunertv;

import org.junit.Test;
import static org.junit.Assert.*;

public final class SourceSettingsTest {
    @Test public void buildsXtreamStreamUrlWithEncoding() {
        SourceSettings source = new SourceSettings(SourceSettings.Type.XTREAM,
                "https://host.example:8080/", "john+doe", "secr&et", "", false);
        assertEquals("https://host.example:8080/live/john%2Bdoe/secr%26et/123.ts",
                source.stream(Channel.Kind.LIVE, "123", ""));
    }

    @Test public void preventsMissingXtreamCredentials() {
        SourceSettings source = new SourceSettings(SourceSettings.Type.XTREAM,
                "https://host.example", "", "", "", false);
        assertFalse(source.valid());
    }
}

