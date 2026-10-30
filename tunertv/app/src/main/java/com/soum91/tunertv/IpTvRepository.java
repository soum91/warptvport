package com.soum91.tunertv;

import android.util.Base64;
import android.util.Xml;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

/** External-source access. Call off the UI thread. No logs or disk cache of credentials/URLs. */
public final class IpTvRepository {
    private final SourceSettings source;
    private String playlistGuideUrl = "";

    public IpTvRepository(SourceSettings source) { this.source = source; }

    private HttpURLConnection open(String address) throws IOException {
        if (!(address.startsWith("https://") || address.startsWith("http://")))
            throw new IOException("Unsupported source address");
        HttpURLConnection c = (HttpURLConnection) new URL(address).openConnection();
        c.setConnectTimeout(12000);
        c.setReadTimeout(30000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "TunerTV/0.1 AndroidTV");
        c.setRequestProperty("Accept-Encoding", "gzip");
        return c;
    }

    private InputStream stream(HttpURLConnection connection) throws IOException {
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) throw new IOException("Server returned HTTP " + status);
        InputStream input = connection.getInputStream();
        if ("gzip".equalsIgnoreCase(connection.getContentEncoding()) ||
                connection.getURL().getPath().toLowerCase(Locale.ROOT).endsWith(".gz"))
            return new GZIPInputStream(input);
        return input;
    }

    private String read(String address, int limit) throws IOException {
        HttpURLConnection c = open(address);
        try (InputStream input = stream(c); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[16384];
            int n;
            while ((n = input.read(buffer)) != -1) {
                if (out.size() + n > limit) throw new IOException("Playlist too large");
                out.write(buffer, 0, n);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        } finally { c.disconnect(); }
    }

    public void verifyAccount() throws Exception {
        if (!source.valid()) throw new IOException("Enter a valid http(s) source and credentials");
        if (source.type != SourceSettings.Type.XTREAM) return;
        JSONObject response = new JSONObject(read(source.base() + "/player_api.php?username="
                + SourceSettings.encoded(source.username) + "&password="
                + SourceSettings.encoded(source.password), 2_000_000));
        JSONObject user = response.optJSONObject("user_info");
        if (user == null || user.optInt("auth", 0) != 1)
            throw new IOException("The provider did not authorize these credentials");
    }

    public List<Channel> live() throws Exception {
        if (source.type == SourceSettings.Type.M3U) {
            String data = read(source.server, 40_000_000);
            M3uParser.Result result = M3uParser.parse(new java.io.StringReader(data));
            playlistGuideUrl = result.guideUrl;
            return result.channels;
        }
        Map<String, String> groups = categories("get_live_categories");
        JSONArray data = array(source.api("get_live_streams"));
        List<Channel> result = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            JSONObject obj = data.optJSONObject(i);
            if (obj == null) continue;
            String id = obj.optString("stream_id");
            if (id.isEmpty()) continue;
            result.add(new Channel(Channel.Kind.LIVE, id, obj.optString("name"),
                    groups.getOrDefault(obj.optString("category_id"), "Other"),
                    source.stream(Channel.Kind.LIVE, id, ""), obj.optString("epg_channel_id"),
                    obj.optString("stream_icon"), ""));
        }
        return result;
    }

    private JSONArray array(String url) throws Exception {
        String json = read(url, 40_000_000).trim();
        return new JSONArray(json);
    }

    private Map<String, String> categories(String action) throws Exception {
        Map<String, String> groups = new HashMap<>();
        JSONArray data = array(source.api(action));
        for (int i = 0; i < data.length(); i++) {
            JSONObject item = data.optJSONObject(i);
            if (item != null) groups.put(item.optString("category_id"), item.optString("category_name"));
        }
        return groups;
    }

    public List<Channel> vod(boolean series) throws Exception {
        if (source.type == SourceSettings.Type.M3U) return new ArrayList<>();
        Map<String, String> groups = categories(series ? "get_series_categories" : "get_vod_categories");
        JSONArray data = array(source.api(series ? "get_series" : "get_vod_streams"));
        List<Channel> result = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            JSONObject item = data.optJSONObject(i);
            if (item == null) continue;
            String id = item.optString(series ? "series_id" : "stream_id");
            if (id.isEmpty()) continue;
            String ext = item.optString("container_extension", "mp4");
            Channel.Kind kind = series ? Channel.Kind.SERIES : Channel.Kind.MOVIE;
            result.add(new Channel(kind, id, item.optString("name"),
                    groups.getOrDefault(item.optString("category_id"), "Other"),
                    series ? "" : source.stream(kind, id, ext), "",
                    item.optString(series ? "cover" : "stream_icon"), ext));
        }
        return result;
    }

    public List<Channel> episodes(Channel series) throws Exception {
        JSONObject response = new JSONObject(read(source.api("get_series_info") + "&series_id="
                + SourceSettings.encoded(series.id), 12_000_000));
        JSONObject seasons = response.optJSONObject("episodes");
        List<Channel> result = new ArrayList<>();
        if (seasons == null) return result;
        List<String> keys = new ArrayList<>();
        java.util.Iterator<String> iterator = seasons.keys();
        while (iterator.hasNext()) keys.add(iterator.next());
        keys.sort(Comparator.comparingInt(IpTvRepository::safeNumber));
        for (String season : keys) {
            JSONArray list = seasons.optJSONArray(season);
            if (list == null) continue;
            for (int i = 0; i < list.length(); i++) {
                JSONObject episode = list.optJSONObject(i);
                if (episode == null) continue;
                String id = episode.optString("id");
                if (id.isEmpty()) continue;
                String ext = episode.optString("container_extension", "mp4");
                result.add(new Channel(Channel.Kind.EPISODE, id,
                        "S" + season + " · E" + episode.optString("episode_num", String.valueOf(i + 1))
                                + "  " + episode.optString("title", "Episode"),
                        series.name, source.stream(Channel.Kind.EPISODE, id, ext), "", series.artwork, ext));
            }
        }
        return result;
    }

    private static int safeNumber(String input) {
        try { return Integer.parseInt(input); } catch (Exception ignored) { return Integer.MAX_VALUE; }
    }

    public List<Program> guide(Channel channel) throws Exception {
        if (source.type == SourceSettings.Type.XTREAM) {
            String json = read(source.api("get_short_epg") + "&stream_id="
                    + SourceSettings.encoded(channel.id) + "&limit=16", 2_000_000);
            JSONObject response = new JSONObject(json);
            JSONArray entries = response.optJSONArray("epg_listings");
            List<Program> result = new ArrayList<>();
            if (entries == null) return result;
            for (int i = 0; i < entries.length(); i++) {
                JSONObject item = entries.optJSONObject(i);
                if (item == null) continue;
                long start = item.optLong("start_timestamp", 0) * 1000L;
                long end = item.optLong("stop_timestamp", 0) * 1000L;
                if (end < System.currentTimeMillis() - 60 * 60 * 1000) continue;
                result.add(new Program(maybeBase64(item.optString("title")), start, end,
                        maybeBase64(item.optString("description"))));
            }
            return result;
        }
        String guideUrl = !source.epgUrl.isEmpty() ? source.epgUrl : playlistGuideUrl;
        if (guideUrl.isEmpty() || channel.epgId.isEmpty()) return new ArrayList<>();
        return xmltv(guideUrl, channel.epgId);
    }

    private static String maybeBase64(String text) {
        if (text == null || text.isEmpty()) return "";
        try {
            String decoded = new String(Base64.decode(text, Base64.DEFAULT), StandardCharsets.UTF_8);
            if (decoded.indexOf('\uFFFD') == -1 && decoded.chars().noneMatch(c -> c < 9)) return decoded;
        } catch (Exception ignored) { }
        return text;
    }

    private List<Program> xmltv(String url, String epgId) throws Exception {
        HttpURLConnection connection = open(url);
        List<Program> result = new ArrayList<>();
        try (InputStream input = stream(connection)) {
            XmlPullParser xml = Xml.newPullParser();
            xml.setInput(new InputStreamReader(input, StandardCharsets.UTF_8));
            int type;
            boolean matching = false;
            String title = "", description = "";
            long start = 0, stop = 0;
            while ((type = xml.next()) != XmlPullParser.END_DOCUMENT) {
                if (type == XmlPullParser.START_TAG && "programme".equals(xml.getName())) {
                    matching = epgId.equals(xml.getAttributeValue(null, "channel"));
                    if (matching) {
                        start = parseXmlDate(xml.getAttributeValue(null, "start"));
                        stop = parseXmlDate(xml.getAttributeValue(null, "stop"));
                        title = ""; description = "";
                    }
                } else if (matching && type == XmlPullParser.START_TAG) {
                    if ("title".equals(xml.getName())) title = xml.nextText();
                    else if ("desc".equals(xml.getName())) description = xml.nextText();
                } else if (matching && type == XmlPullParser.END_TAG && "programme".equals(xml.getName())) {
                    if (stop > System.currentTimeMillis() - 60 * 60 * 1000 && result.size() < 32)
                        result.add(new Program(title, start, stop, description));
                    matching = false;
                }
            }
        } finally { connection.disconnect(); }
        result.sort(Comparator.comparingLong(p -> p.startMillis));
        return result;
    }

    private static long parseXmlDate(String raw) {
        if (raw == null) return 0;
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            if (raw.length() == 14) raw += " +0000";
            return format.parse(raw.trim()).getTime();
        } catch (Exception ignored) { return 0; }
    }
}

