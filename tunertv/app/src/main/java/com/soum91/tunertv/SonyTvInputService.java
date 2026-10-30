package com.soum91.tunertv;

import android.database.Cursor;
import android.media.tv.TvContract;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.net.Uri;
import android.view.Surface;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

/** Optional TV Input Framework source; Sony controls whether this appears in its TV app. */
public final class SonyTvInputService extends TvInputService {
    @Override public Session onCreateSession(String inputId) { return new TunerSession(); }

    private final class TunerSession extends Session {
        private ExoPlayer player;
        private Surface surface;
        TunerSession() {
            super(SonyTvInputService.this);
            player = new ExoPlayer.Builder(SonyTvInputService.this).build();
            player.addListener(new Player.Listener() {
                @Override public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_READY) notifyVideoAvailable();
                    else if (state == Player.STATE_BUFFERING) notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_BUFFERING);
                }
                @Override public void onPlayerError(PlaybackException error) {
                    notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
                }
            });
        }

        @Override public boolean onSetSurface(Surface newSurface) {
            surface = newSurface;
            player.setVideoSurface(surface);
            return true;
        }

        @Override public boolean onTune(Uri channelUri) {
            String channelId = null;
            try (Cursor cursor = getContentResolver().query(channelUri,
                    new String[]{TvContract.Channels.COLUMN_INTERNAL_PROVIDER_ID}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) channelId = cursor.getString(0);
            } catch (Exception ignored) { }
            if (channelId == null || !channelId.matches("[0-9]+")) {
                notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
                return false;
            }
            try {
                SourceSettings source = new SecureStore(SonyTvInputService.this).source();
                if (source == null || source.type != SourceSettings.Type.XTREAM) return false;
                player.stop();
                player.setMediaItem(MediaItem.fromUri(source.stream(Channel.Kind.LIVE, channelId, "")));
                player.prepare();
                player.play();
                notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING);
                return true;
            } catch (Exception error) {
                notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
                return false;
            }
        }

        @Override public void onSetStreamVolume(float volume) { player.setVolume(volume); }
        @Override public void onSetCaptionEnabled(boolean enabled) { }
        @Override public void onRelease() {
            player.release();
            player = null;
            surface = null;
        }
    }
}

