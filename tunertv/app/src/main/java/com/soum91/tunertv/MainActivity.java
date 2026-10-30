package com.soum91.tunertv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Live television is the home screen. Menus only overlay the broadcast on demand. */
public final class MainActivity extends Activity {
    private static final int CYAN = 0xFF45DAA4;
    private static final int OFF_WHITE = 0xFFF2F7F6;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final List<Channel> live = new ArrayList<>();
    private final Set<String> favouriteIds = new HashSet<>();
    private FrameLayout root;
    private FrameLayout overlay;
    private TextView osd;
    private PlayerView playerView;
    private ExoPlayer player;
    private SecureStore store;
    private WarpController warp;
    private ConnectivityManager connectivity;
    private ConnectivityManager.NetworkCallback networkCallback;
    private SourceSettings source;
    private IpTvRepository repository;
    private Channel current;
    private Channel prior;
    private List<Channel> lastCatalog = new ArrayList<>();
    private boolean viewingVod;
    private boolean sourceIsXtream = true;
    private String panel = "";
    private String numberBuffer = "";
    private int retries = 0;
    private boolean interrupted;
    private boolean vpnRequiredForRecovery;
    private long bufferingSince;
    private Runnable numberCommit;
    private final Runnable watchdog = new Runnable() {
        @Override public void run() {
            if (player != null && current != null) {
                if (player.getPlaybackState() == Player.STATE_BUFFERING && bufferingSince != 0
                        && System.currentTimeMillis() - bufferingSince > 16000 && !interrupted) {
                    recoverStream("Stream buffering too long");
                } else if (interrupted && isNetworkAvailable() &&
                        (!vpnRequiredForRecovery || warp.anyVpnConnected())) {
                    recoverStream("Network restored");
                }
            }
            ui.postDelayed(this, 5000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        store = new SecureStore(this);
        warp = new WarpController(this);
        connectivity = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override public void onAvailable(Network network) { ui.post(() -> {
                    if (interrupted) recoverStream("Connection restored");
                }); }
            };
            connectivity.registerDefaultNetworkCallback(networkCallback);
        }
        favouriteIds.addAll(Arrays.asList(store.favorites().split("\\|")));
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        playerView = new PlayerView(this);
        playerView.setUseController(false);
        root.addView(playerView, new FrameLayout.LayoutParams(-1, -1));
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.addListener(new Player.Listener() {
            @Override public void onPlayerError(PlaybackException error) {
                recoverStream("Playback disconnected");
            }
            @Override public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    interrupted = false;
                    retries = 0;
                    bufferingSince = 0;
                    vpnRequiredForRecovery = warp.anyVpnConnected();
                } else if (state == Player.STATE_BUFFERING && bufferingSince == 0) {
                    bufferingSince = System.currentTimeMillis();
                }
            }
        });
        ui.postDelayed(watchdog, 5000);
        osd = label("TunerTV", 23, true);
        osd.setPadding(dp(18), dp(12), dp(20), dp(12));
        osd.setBackground(rounded(0xDD071B26, 14));
        FrameLayout.LayoutParams osdParams = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.START);
        osdParams.setMargins(dp(32), 0, dp(32), dp(34));
        root.addView(osd, osdParams);
        osd.setVisibility(View.GONE);
        setContentView(root);
        try {
            source = store.source();
            sourceIsXtream = source == null || source.type == SourceSettings.Type.XTREAM;
            if (source == null) showSetup();
            else loadLive();
        } catch (Exception error) {
            info("Saved account could not be unlocked. Enter it again.", 6000);
            showSetup();
        }
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    private GradientDrawable rounded(int color, int corner) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(corner));
        return bg;
    }

    private TextView label(String text, int size, boolean strong) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(OFF_WHITE);
        if (strong) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private Button button(String text, Runnable run) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(15);
        button.setTextColor(OFF_WHITE);
        button.setAllCaps(false);
        button.setBackground(rounded(0xFF1D3B44, 10));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(48));
        p.setMargins(0, dp(5), 0, dp(5));
        button.setLayoutParams(p);
        button.setOnClickListener(v -> run.run());
        return button;
    }

    private EditText input(String hint, String initial, int type) {
        EditText input = new EditText(this);
        input.setText(initial);
        input.setHint(hint);
        input.setHintTextColor(0xFF9AB1B9);
        input.setSingleLine(true);
        input.setTextColor(OFF_WHITE);
        input.setTextSize(17);
        input.setInputType(type);
        input.setPadding(dp(16), 0, dp(16), 0);
        input.setBackground(rounded(0xFF24424D, 8));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(52));
        p.setMargins(0, dp(5), 0, dp(9));
        input.setLayoutParams(p);
        return input;
    }

    private LinearLayout column() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(30), dp(25), dp(30), dp(24));
        return view;
    }

    private void clearOverlay() {
        if (overlay != null) root.removeView(overlay);
        overlay = null;
        panel = "";
    }

    private LinearLayout makeOverlay(String name, int widthDp) {
        clearOverlay();
        panel = name;
        overlay = new FrameLayout(this);
        overlay.setBackgroundColor(0x88000000);
        overlay.setFocusableInTouchMode(true);
        overlay.setOnClickListener(v -> clearOverlay());
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout body = column();
        body.setBackgroundColor(0xF5071720);
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(dp(widthDp), -1, Gravity.START);
        overlay.addView(body, p);
        overlay.requestFocus();
        return body;
    }

    private void sectionTitle(LinearLayout body, String title, String caption) {
        TextView heading = label(title, 29, true);
        heading.setTextColor(CYAN);
        body.addView(heading, new LinearLayout.LayoutParams(-1, dp(47)));
        if (!caption.isEmpty()) {
            TextView sub = label(caption, 14, false);
            sub.setTextColor(0xFF9BB8BE);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(44));
            body.addView(sub, p);
        }
    }

    private void info(String message, long durationMs) {
        osd.setText(message);
        osd.setVisibility(View.VISIBLE);
        ui.removeCallbacks(hideOsd);
        if (durationMs > 0) ui.postDelayed(hideOsd, durationMs);
    }
    private final Runnable hideOsd = () -> { if (osd != null) osd.setVisibility(View.GONE); };

    private void showSetup() {
        LinearLayout body = makeOverlay("setup", 560);
        sectionTitle(body, "TunerTV", "Add your own IPTV subscription");
        Button mode = button(sourceIsXtream ? "Provider: Xtream Codes  ▾" : "Provider: M3U playlist  ▾", () -> {
            sourceIsXtream = !sourceIsXtream;
            showSetup();
        });
        body.addView(mode);
        boolean xt = sourceIsXtream;
        EditText server = input(xt ? "Server URL · http(s)://host:port" : "M3U playlist URL", source == null ? "" : source.server,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        body.addView(server);
        EditText user = input("Xtream username", source == null ? "" : source.username, InputType.TYPE_CLASS_TEXT);
        EditText password = input("Xtream password", source == null ? "" : source.password,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        if (xt) { body.addView(user); body.addView(password); }
        EditText epg = input("Optional XMLTV URL", source == null ? "" : source.epgUrl,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        body.addView(epg);
        TextView notice = label("Credentials stay encrypted on this TV. Nothing is uploaded to GitHub.", 13, false);
        notice.setTextColor(0xFFACC0C4);
        body.addView(notice);
        Button connect = button("Connect & watch TV", () -> {
            SourceSettings next = new SourceSettings(xt ? SourceSettings.Type.XTREAM : SourceSettings.Type.M3U,
                    server.getText().toString(), user.getText().toString(), password.getText().toString(),
                    epg.getText().toString(), false);
            if (!next.valid()) { notice.setText("Check your provider URL and Xtream username/password."); return; }
            connectPending(body, next, notice);
        });
        body.addView(connect);
        connect.requestFocus();
    }

    private void connectPending(LinearLayout body, SourceSettings next, TextView notice) {
        notice.setText("Checking subscription and loading channels…");
        io.execute(() -> {
            try {
                IpTvRepository candidate = new IpTvRepository(next);
                candidate.verifyAccount();
                List<Channel> channels = candidate.live();
                if (channels.isEmpty()) throw new IllegalStateException("No live channels in the playlist");
                store.saveSource(next);
                ui.post(() -> {
                    source = next; repository = candidate;
                    live.clear(); live.addAll(channels);
                    clearOverlay();
                    restoreLastChannel();
                });
            } catch (Exception problem) {
                // No exception/URL dump: Xtream errors can embed account credentials.
                ui.post(() -> notice.setText("Couldn't load this subscription. Check URL, login and provider status."));
            }
        });
    }

    private void loadLive() {
        info("TunerTV · Loading your channels…", 0);
        io.execute(() -> {
            try {
                IpTvRepository next = new IpTvRepository(source);
                List<Channel> channels = next.live();
                ui.post(() -> {
                    repository = next;
                    live.clear(); live.addAll(channels);
                    if (channels.isEmpty()) { showSetup(); return; }
                    restoreLastChannel();
                });
            } catch (Exception error) {
                ui.post(() -> { info("Connection failed. Open Settings to retry.", 0); showSetup(); });
            }
        });
    }

    private void restoreLastChannel() {
        Channel saved = null;
        for (Channel c : live) if (c.id.equals(store.lastChannel())) { saved = c; break; }
        tune(saved != null ? saved : live.get(0));
    }

    private void tune(Channel channel) {
        if (channel == null || channel.url.isEmpty()) return;
        if (current != null && current.kind == Channel.Kind.LIVE && channel.kind == Channel.Kind.LIVE
                && !channel.id.equals(current.id)) prior = current;
        viewingVod = channel.kind != Channel.Kind.LIVE;
        current = channel;
        retries = 0;
        interrupted = false;
        bufferingSince = System.currentTimeMillis();
        vpnRequiredForRecovery = warp.anyVpnConnected();
        if (!viewingVod) store.setLastChannel(channel.id);
        clearOverlay();
        player.stop();
        MediaItem item = MediaItem.fromUri(channel.url);
        player.setMediaItem(item);
        player.prepare();
        player.play();
        int number = live.indexOf(channel) + 1;
        info((number > 0 ? "CH " + number + "  ·  " : "") + channel.name +
                (channel.group.isEmpty() ? "" : "\n" + channel.group), 3500);
    }

    private boolean isNetworkAvailable() {
        if (connectivity == null) return false;
        Network active = connectivity.getActiveNetwork();
        NetworkCapabilities caps = active == null ? null : connectivity.getNetworkCapabilities(active);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void recoverStream(String reason) {
        if (current == null || player == null || retries >= 4) {
            info("Channel unavailable · press CH+/− to continue", 5000);
            return;
        }
        interrupted = true;
        if (!isNetworkAvailable()) {
            info("Network disconnected · waiting for reconnection", 0);
            return;
        }
        if (vpnRequiredForRecovery && !warp.anyVpnConnected()) {
            // This check is real, but we cannot claim automatic WARP reconnection
            // until an in-process WARP runtime is available for TunerTV.
            info("VPN disconnected · reconnect WARP TV, then playback will resume", 0);
            return;
        }
        interrupted = false;
        int attempt = ++retries;
        String streamId = current.id;
        info(reason + " · retrying stream " + attempt + "/4", 3200);
        ui.postDelayed(() -> {
            if (player == null || current == null || !streamId.equals(current.id)) return;
            bufferingSince = System.currentTimeMillis();
            player.stop();
            player.setMediaItem(MediaItem.fromUri(current.url));
            player.prepare();
            player.play();
        }, Math.min(1000L * attempt * attempt, 8000));
    }

    private void step(int direction) {
        if (live.isEmpty()) return;
        int index = live.indexOf(current);
        if (index < 0) index = 0;
        int next = (index + direction + live.size()) % live.size();
        tune(live.get(next));
    }

    private void lastChannel() {
        if (prior != null) tune(prior);
        else if (!live.isEmpty()) tune(live.get(0));
    }

    private List<Channel> filteredChannels(boolean favoritesOnly) {
        if (!favoritesOnly) return new ArrayList<>(live);
        List<Channel> items = new ArrayList<>();
        for (Channel channel : live) if (favouriteIds.contains(channel.id)) items.add(channel);
        return items;
    }

    private void showChannels(boolean favoritesOnly) {
        LinearLayout body = makeOverlay("channels", 490);
        sectionTitle(body, favoritesOnly ? "Favourites" : "Live television",
                live.size() + " channels · CH+/− changes channels");
        body.addView(button(favoritesOnly ? "Show all channels" : "★ Show favourites",
                () -> showChannels(!favoritesOnly)));
        body.addView(button("Programme guide", this::showGuide));
        List<Channel> items = filteredChannels(favoritesOnly);
        ListView list = listView(items, channel -> tune(channel), true);
        body.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        int index = items.indexOf(current);
        if (index >= 0) list.setSelection(index);
        list.requestFocus();
    }

    private interface OnChannelSelected { void open(Channel selected); }
    private ListView listView(List<Channel> entries, OnChannelSelected listener, boolean number) {
        ListView list = new ListView(this);
        list.setBackgroundColor(Color.TRANSPARENT);
        list.setDividerHeight(dp(2));
        list.setSelector(rounded(0xFF256977, 8));
        ArrayAdapter<Channel> adapter = new ArrayAdapter<Channel>(this, android.R.layout.simple_list_item_1, entries) {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                TextView item = label("", 18, false);
                item.setMinHeight(dp(56));
                item.setPadding(dp(14), dp(7), dp(8), dp(7));
                Channel entry = getItem(position);
                if (entry != null) {
                    int channelNumber = live.indexOf(entry) + 1;
                    item.setText((number && channelNumber > 0 ? channelNumber + "   " : "")
                            + entry.name + (favouriteIds.contains(entry.id) ? "  ★" : "")
                            + "\n" + entry.group);
                }
                return item;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, index, id) -> listener.open(entries.get(index)));
        return list;
    }

    private void showGuide() {
        Channel guideChannel = current != null && current.kind == Channel.Kind.LIVE ? current
                : (!live.isEmpty() ? live.get(0) : null);
        if (guideChannel == null) { info("No channels to guide", 3000); return; }
        LinearLayout body = makeOverlay("guide", 610);
        sectionTitle(body, "Programme guide", guideChannel.name);
        TextView loading = label("Loading programme information…", 18, false);
        body.addView(loading);
        IpTvRepository guideRepo = repository;
        io.execute(() -> {
            try {
                List<Program> entries = guideRepo.guide(guideChannel);
                ui.post(() -> {
                    if (!"guide".equals(panel)) return;
                    body.removeView(loading);
                    if (entries.isEmpty()) {
                        body.addView(label("No EPG supplied for this channel.", 18, false));
                        return;
                    }
                    LinearLayout schedule = column();
                    DateFormat clock = DateFormat.getTimeInstance(DateFormat.SHORT);
                    for (Program entry : entries) {
                        TextView row = label(clock.format(entry.startMillis) + "  " + entry.title, 18, false);
                        row.setMinHeight(dp(58));
                        row.setPadding(dp(8), dp(8), dp(8), dp(8));
                        schedule.addView(row);
                    }
                    android.widget.ScrollView scroll = new android.widget.ScrollView(this);
                    scroll.addView(schedule);
                    body.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
                });
            } catch (Exception error) {
                ui.post(() -> { if ("guide".equals(panel)) loading.setText("EPG unavailable. Check guide URL or provider."); });
            }
        });
    }

    private void showMenu() {
        LinearLayout body = makeOverlay("menu", 420);
        sectionTitle(body, "TunerTV", "Your television, your channels");
        body.addView(button("Live TV", () -> {
            if (viewingVod) returnToLive();
            else showChannels(false);
        }));
        body.addView(button("Programme guide", this::showGuide));
        body.addView(button("Movies", () -> showCatalog(false)));
        body.addView(button("Series", () -> showCatalog(true)));
        body.addView(button("Favourites", () -> showChannels(true)));
        body.addView(button("VPN & Settings", this::showSettings));
        body.getChildAt(2).requestFocus();
    }

    private void showCatalog(boolean series) {
        if (source == null || source.type != SourceSettings.Type.XTREAM) {
            info("M3U contains live channels. Xtream supports Movies and Series.", 4200);
            return;
        }
        LinearLayout body = makeOverlay(series ? "series" : "movies", 600);
        sectionTitle(body, series ? "Series" : "Movies", "Browse your provider's on-demand library");
        TextView loading = label("Loading catalogue…", 17, false);
        body.addView(loading);
        io.execute(() -> {
            try {
                List<Channel> catalog = repository.vod(series);
                ui.post(() -> {
                    if (!panel.equals(series ? "series" : "movies")) return;
                    lastCatalog = catalog;
                    body.removeView(loading);
                    ListView list = listView(catalog, entry -> {
                        if (entry.kind == Channel.Kind.SERIES) showEpisodes(entry);
                        else showMovieDetail(entry);
                    }, false);
                    body.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
                    list.requestFocus();
                });
            } catch (Exception error) {
                ui.post(() -> loading.setText("Could not load the on-demand catalogue."));
            }
        });
    }

    private void showMovieDetail(Channel movie) {
        LinearLayout body = makeOverlay("movie-detail", 570);
        sectionTitle(body, movie.name, movie.group);
        body.addView(label("Select Play to start this movie.", 17, false));
        body.addView(button("▶ Play movie", () -> tune(movie)));
        body.addView(button("← Back to movies", () -> showCatalog(false)));
    }

    private void showEpisodes(Channel series) {
        LinearLayout body = makeOverlay("episodes", 580);
        sectionTitle(body, series.name, "Choose a season and episode");
        TextView loading = label("Loading episodes…", 17, false);
        body.addView(loading);
        io.execute(() -> {
            try {
                List<Channel> entries = repository.episodes(series);
                ui.post(() -> {
                    if (!"episodes".equals(panel)) return;
                    body.removeView(loading);
                    ListView list = listView(entries, this::tune, false);
                    body.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
                    list.requestFocus();
                });
            } catch (Exception error) {
                ui.post(() -> loading.setText("Episodes are unavailable for this series."));
            }
        });
    }

    private void returnToLive() {
        viewingVod = false;
        restoreLastChannel();
    }

    private void showSettings() {
        LinearLayout body = makeOverlay("settings", 580);
        sectionTitle(body, "Settings", "Tuner, subscription and private connection");
        body.addView(button("VPN status: " + (warp.anyVpnConnected() ? "Detected" : "Disconnected") + " · Check", () -> {
            info(warp.anyVpnConnected() ? "VPN tunnel detected (provider not verified)" :
                    "No active VPN tunnel detected", 3600);
            showSettings();
        }));
        body.addView(button("Open our installed WARP TV app", this::openWarpTv));
        body.addView(button("Retry interrupted channel", () -> recoverStream("Manual recovery")));
        body.addView(button("Switch source / update account", () -> {
            sourceIsXtream = source == null || source.type == SourceSettings.Type.XTREAM;
            showSetup();
        }));
        body.addView(button("Xtream live format: " + (source != null && source.useHls ? "HLS" : "MPEG-TS"), () -> {
            if (source == null || source.type != SourceSettings.Type.XTREAM) return;
            source = new SourceSettings(source.type, source.server, source.username, source.password,
                    source.epgUrl, !source.useHls);
            try { store.saveSource(source); } catch (Exception ignored) { info("Couldn't save format setting", 3000); }
            repository = new IpTvRepository(source);
            clearOverlay();
            loadLive();
        }));
        body.addView(button("Publish Xtream channels to Sony's TV tuner", () ->
                startActivity(new Intent(this, TvInputSetupActivity.class))));
        body.addView(button("Reload live channels", () -> { clearOverlay(); loadLive(); }));
        body.getChildAt(2).requestFocus();
    }

    private void openWarpTv() {
        if (!warp.openWarpTv()) info("WARP TV isn't installed", 3600);
    }

    private void toggleFavorite() {
        if (current == null || current.kind != Channel.Kind.LIVE) return;
        if (!favouriteIds.add(current.id)) favouriteIds.remove(current.id);
        store.setFavorites(String.join("|", favouriteIds));
        info((favouriteIds.contains(current.id) ? "★ Saved to favourites: " : "Removed favourite: ") + current.name, 3000);
    }

    private void number(int digit) {
        numberBuffer += digit;
        if (numberBuffer.length() > 5) numberBuffer = String.valueOf(digit);
        info("CH " + numberBuffer, 0);
        if (numberCommit != null) ui.removeCallbacks(numberCommit);
        numberCommit = () -> {
            int channel = -1;
            try { channel = Integer.parseInt(numberBuffer); } catch (NumberFormatException ignored) { }
            numberBuffer = "";
            if (channel >= 1 && channel <= live.size()) tune(live.get(channel - 1));
            else info("Channel not found", 2000);
        };
        ui.postDelayed(numberCommit, 1300);
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(event);
        int code = event.getKeyCode();
        if ("setup".equals(panel)) return super.dispatchKeyEvent(event);
        if (code == KeyEvent.KEYCODE_CHANNEL_UP) { step(1); return true; }
        if (code == KeyEvent.KEYCODE_CHANNEL_DOWN) { step(-1); return true; }
        if (code >= KeyEvent.KEYCODE_0 && code <= KeyEvent.KEYCODE_9) { number(code - KeyEvent.KEYCODE_0); return true; }
        if (code == KeyEvent.KEYCODE_GUIDE) { showGuide(); return true; }
        if (code == KeyEvent.KEYCODE_TV) { if (viewingVod) returnToLive(); else showChannels(false); return true; }
        if (code == KeyEvent.KEYCODE_LAST_CHANNEL) { lastChannel(); return true; }
        if (code == KeyEvent.KEYCODE_PROG_RED) { toggleFavorite(); return true; }
        if (code == KeyEvent.KEYCODE_MENU) { showMenu(); return true; }
        if (code == KeyEvent.KEYCODE_INFO) {
            if (current != null) info(current.name + "\n" + current.group, 4500);
            return true;
        }
        if (panel.isEmpty() && code == KeyEvent.KEYCODE_DPAD_LEFT) { showChannels(false); return true; }
        if (panel.isEmpty() && code == KeyEvent.KEYCODE_DPAD_RIGHT) { showGuide(); return true; }
        if (panel.isEmpty() && code == KeyEvent.KEYCODE_DPAD_CENTER) { showChannels(false); return true; }
        return super.dispatchKeyEvent(event);
    }

    @Override public void onBackPressed() {
        if (!panel.isEmpty()) clearOverlay();
        else if (viewingVod) returnToLive();
        else showMenu();
    }

    @Override protected void onDestroy() {
        ui.removeCallbacksAndMessages(null);
        if (connectivity != null && networkCallback != null) connectivity.unregisterNetworkCallback(networkCallback);
        io.shutdownNow();
        if (player != null) { playerView.setPlayer(null); player.release(); player = null; }
        super.onDestroy();
    }
}

