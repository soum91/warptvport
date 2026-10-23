# WARP TV adaptation for Sony Android TV

Private, owner-maintained adaptation files for the Sony A80L (Android TV 12, ARM32). This project modifies a user-supplied Cloudflare 1.1.1.1 package to add a TV launcher entry, landscape dashboard, remote-friendly focus and activation, and a registration recovery path.

This is an unofficial personal adaptation. Cloudflare owns the original 1.1.1.1 app and its native code. This repository does not contain Cloudflare's APK, decoded app sources, or signing credentials. A matching ARM32 app bundle and local build tools are required. Do not redistribute a rebuilt APK unless you have permission from the relevant rights holders.

## Source files

- `prepare_tv.py`: applies the TV resource, manifest, D-pad, and registration changes to a freshly decoded package.
- `RemoteSupport.smali`: runtime remote-key adapter installed by the preparation script.
- `tv_banner.png.b64`: original TV banner asset in Base64 form. The script materializes `tv_banner.png` locally when needed.

## Build outline

1. On your own machine, obtain the official package and compatible build tools. Keep them local; do not commit APK/APKM files or signing keys.
2. Extract the universal APKM and retain the `armeabi-v7a` split. Omit `arm64-v8a`, `x86`, and `x86_64` splits. Keep base, language, and density splits.
3. Merge the retained splits with APKEditor, then decode the merged APK with Apktool.
4. Place the decoded package in a sibling directory named `decoded` beside `prepare_tv.py`.
5. Run `python3 prepare_tv.py` from this directory.
6. Rebuild with Apktool and sign the APK using your own key. Install it on the TV and approve Android's VPN connection prompt when it appears.

The package version and IDs this patch expects are specific to the 6.38.11 build. A different upstream build may require changes to the Smali paths and resource identifiers.

## Connection behavior

The original CometSwitch listener only accepts changes preceded by a touchscreen event. RemoteSupport uses performClick(), so earlier TV builds could change the switch appearance without invoking the connection callback.

Version `6.38.11-tv.4` marks click dispatch in CometSwitch, consumes that marker before invoking the existing connection callback, and clears it on normal and exceptional returns. Touch activation still works. Programmatic state synchronization remains silent, including updates made within the callback. The speculative `tv.3` result-callback workaround has been removed; stock VPN result handling already starts the service on Android 12.

When there is no Cloudflare device registration, the first activation opens Cloudflare's terms/registration activity. After registration returns to the dashboard, activate the switch again to reach Android's VPN profile flow.

The input callback can be checked locally. End-to-end WARP registration, tunnel establishment and traffic still require the Sony A80L; successful APK packaging is not a connectivity test.
