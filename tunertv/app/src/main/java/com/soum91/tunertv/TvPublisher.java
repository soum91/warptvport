package com.soum91.tunertv;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.Uri;

import java.util.List;

/** Best-effort bridge into Sony's OEM tuner; third-party TIF behaviour varies by firmware. */
public final class TvPublisher {
    private TvPublisher() { }

    public static String inputId(Context context) {
        TvInputManager manager = (TvInputManager) context.getSystemService(Context.TV_INPUT_SERVICE);
        if (manager == null) return null;
        ComponentName component = new ComponentName(context, SonyTvInputService.class);
        for (TvInputInfo info : manager.getTvInputList()) {
            ServiceInfo service = info.getServiceInfo();
            if (service != null && component.getClassName().equals(service.name)
                    && component.getPackageName().equals(service.packageName)) return info.getId();
        }
        return null;
    }

    public static int publish(Context context, List<Channel> channels) {
        String id = inputId(context);
        if (id == null) throw new IllegalStateException("Sony did not register TunerTV as a TV source");
        Uri ownChannels = TvContract.buildChannelsUriForInput(id);
        // Delete only channels owned by this input. Never touch the physical tuner or other inputs.
        context.getContentResolver().delete(ownChannels, null, null);
        int count = 0;
        for (Channel channel : channels) {
            if (channel.kind != Channel.Kind.LIVE || channel.id.startsWith("m3u-")) continue;
            ContentValues values = new ContentValues();
            values.put(TvContract.Channels.COLUMN_INPUT_ID, id);
            values.put(TvContract.Channels.COLUMN_TYPE, TvContract.Channels.TYPE_OTHER);
            values.put(TvContract.Channels.COLUMN_DISPLAY_NUMBER, String.valueOf(++count));
            values.put(TvContract.Channels.COLUMN_DISPLAY_NAME, channel.name);
            values.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_ID, channel.id);
            context.getContentResolver().insert(TvContract.Channels.CONTENT_URI, values);
            if (count >= 2000) break; // Protect OEM TV provider from enormous IPTV catalogues.
        }
        return count;
    }
}

