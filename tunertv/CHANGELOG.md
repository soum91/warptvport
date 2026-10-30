# Changelog

## 0.1.1 - WARP TV tv.4 inspection and repository handoff

- Inspected the privately supplied `WARP-TV-6.38.11-tv.4-arm32` APK and verified ZIP integrity.
- Confirmed WARP's ARM32 native libraries match the supplied `tv.1` build; TV modifications differ in classes and manifest.
- Fixed the Settings launcher to target WARP's actual `com.cloudflare.onedotonedotonedotone` package ID.
- Documented the source-only public release and limits of automatic WARP recovery. No third-party APK or IPTV secrets are committed.
- Advanced Android version code to 2 for future update detection.
- Added a repository-root GitHub Actions workflow to build a TunerTV test APK from `warptvport/tunertv`.


## 0.1.0 - Initial native Android TV implementation

- Full-screen live TV entry and last-channel restoration.
- Xtream Codes and M3U source setup without embedded credentials.
- Live channel list, favourites, direct channel numbering, CH+/CH- and guide controls.
- Xtream movie/series catalogue and episode navigation.
- XMLTV and Xtream short-EPG loaders.
- Android TV input service experiment for Sony's native TV experience.
- WARP runtime integration boundary and VPN/network status detection; stream recovery framework.
- Shortcut into the working existing WARP TV app while an embeddable runtime is evaluated.
- Android TV CI debug artifact; release signing remains owner-managed.

### Known limitations

- Sony TV button routing is firmware dependent. TIF channel integration must be tested on the A80L.
- Actual in-process Cloudflare WARP embedding is blocked until the working owner-provided WARP TV APK can be audited; **not implemented yet**.
- VPN reconnection cannot yet be initiated silently. A detected dropped VPN is surfaced to the user.
- DRM-protected or proprietary IPTV formats are not supported.
- Stable in-place upgrades need a persistent signing keystore (never committed to the repository).

