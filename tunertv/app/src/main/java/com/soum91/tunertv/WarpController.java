package com.soum91.tunertv;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

/**
 * WARP integration boundary. The existing adaptation is another APK and cannot be
 * silently controlled or repackaged here without its working binary and an audited API.
 * A generic VPN transport is NOT proof that it belongs to Cloudflare.
 */
public final class WarpController {
    // warptvport keeps the official Cloudflare package ID when patching the APK.
    private static final String WARP_PACKAGE = "com.cloudflare.onedotonedotonedotone";
    private final Context context;

    public WarpController(Context context) { this.context = context.getApplicationContext(); }

    public boolean anyVpnConnected() {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) return false;
        for (Network network : manager.getAllNetworks()) {
            NetworkCapabilities caps = manager.getNetworkCapabilities(network);
            if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return true;
        }
        return false;
    }

    public boolean canReconnectSilently() {
        // Requires actual embedded WARP runtime from the working owner-provided APK.
        return false;
    }

    public void reconnectIfPossible(Runnable onReady, Runnable onUnavailable) {
        if (anyVpnConnected()) onReady.run();
        else onUnavailable.run();
    }

    public boolean openWarpTv() {
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(WARP_PACKAGE);
        if (launch == null) return false;
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(launch);
        return true;
    }
}

