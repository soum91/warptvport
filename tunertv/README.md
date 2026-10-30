# TunerTV

Native Android TV IPTV tuner aimed at Sony BRAVIA A80L (Android TV 12, 32-bit ARM) with a television-first experience. An IPTV subscription is supplied by the owner; this project does not bundle playlists, streams, accounts or keys.

## Features

- Starts directly on last watched live channel; no IPTV portal dashboard.
- CH+/CH-, numeric selection, previous channel, favourites, remote-friendly channel list and EPG.
- Xtream Codes API with movie, series and episode browsing; M3U with optional XMLTV guide.
- Media3 playback of IPTV streams supported by the Android decoder.
- Optional experimental `TvInputService` source for Sony live TV. On the A80L, visibility in the system TV app and TV-button routing are firmware dependent.
- WARP/VPN status monitoring and channel recovery when connectivity returns. The working external WARP TV app can be opened from Settings while native WARP embedding is pending.

## Source setup

1. Install the Android TV APK and open TunerTV.
2. In the first-run setup, select Xtream Codes or M3U.
3. Enter your own server URL and account credentials on the TV. These are stored in Android Keystore-encrypted local preferences, not in GitHub.
4. Use CH+/CH-, GUIDE, number buttons and menu once the playlist loads.
5. Open Settings to change streaming mode, check VPN status, access WARP TV, or publish channels as a Sony TV source.

## WARP, honestly

The public `soum91/warptvport` repository contains adaptation patches for the proprietary Cloudflare APK, **not** an embeddable WARP runtime. The owner's `WARP-TV-6.38.11-tv.4-arm32` APK was provided privately and verified; its native WARP library matches the older `tv.1` build byte-for-byte. This confirms the native engine is present, but not that it can be safely or legally repackaged into a separate public app. TunerTV currently detects whether Android has an active VPN network and pauses stream recovery when a previously active VPN drops. Once VPN connectivity returns, it retries the stream. The current code cannot connect or restart WARP silently. **In-app WARP is not yet finished.**

The existing adapted WARP APK retains the upstream package name, `com.cloudflare.onedotonedotonedotone`. The Settings shortcut opens that installed app. This shortcut is not WARP embedding or automatic connection control. Do not commit the owner's APK, Cloudflare binaries or decoded proprietary sources to this public repository.

Cloudflare's current consumer WARP is MASQUE-based, not the WireGuard substitute previously considered. No WireGuard dependency or profile import is included in this project. Android allows one active VPN at a time.

## Build

- Android Studio with JDK 17, Android SDK 36 and Gradle 8.13; Android Gradle Plugin 8.13.
- For a standalone checkout of this folder, run `gradle :app:assembleDebug` for a development APK. From the `warptvport` repository root, run `gradle -p tunertv :app:assembleDebug`. GitHub Actions uploads a debug APK artifact.
- For a repeatably signed release, configure a stable keystore outside Git with Gradle properties `TUNERTV_KEYSTORE_PATH`, `TUNERTV_STORE_PASSWORD`, `TUNERTV_KEY_ALIAS` and `TUNERTV_KEY_PASSWORD`. Never regenerate the key between releases.
- CI currently supplies test builds only; repeated debug APKs may have different signing identities. Do not expect install-over updates until release signing is configured.

## Remote testing on Sony A80L

```text
adb shell am start -n com.soum91.tunertv/.MainActivity
adb shell input keyevent 166      # CHANNEL_UP
adb shell input keyevent 167      # CHANNEL_DOWN
adb shell input keyevent 172      # GUIDE
adb shell input keyevent 170      # TV
```

The real remote may emit different codes. Check with `adb shell getevent -l` or a key-event diagnostic app. Sony Settings > Channels & Inputs > TV button shortcut may expose selectable TV sources. If it doesn't, installing a normal app cannot forcibly replace Sony's global TV button handler without separate remapping/privileged setup.

## Privacy

- No account details, VPN private keys, signing keys, M3U URLs or playlists are checked into Git.
- Do not paste credentials into GitHub issues, logs or screenshots.
- The in-app session's raw stream URLs can include credentials because the Xtream streaming protocol requires that URL form; avoid exposing logs or exporting debug traces.

## Legal / compatibility

Use only subscription sources you are authorized to access. This player does not bypass DRM. The optional third-party TV input path is best-effort on current Google TV versions and isn't a replacement for Sony's system app.

