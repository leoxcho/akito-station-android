# Akito Station for Android

Native Kotlin / Jetpack Compose edition of Akito Station. Android 8.0 (API 26) or newer; ARM64 packaging. Version 1.0.0, build 1.

The Public edition provides a unified, local game library and explicit external emulator handoff. No emulator, game, BIOS, console firmware or encryption key is bundled. The dark navy/violet, red/cyan palette, Station logo, collection grid and library navigation follow the macOS Public reference. Android uses document-provider access instead of desktop filesystem paths.

## Implemented

- Adaptive cover grid, search by title/system, All games / Favorites / Recently launched, system filtering and title/recent/system sorting.
- SQLite metadata persistence, stable document identities, favorites, launch timestamps, editable title/system and bounded cover-image import.
- Multiple folder libraries via Android Storage Access Framework; recursive scanning, cancellation and bounded traversal; sibling artwork discovery. Failed/offline scans preserve existing metadata and never delete originals.
- Explicit per-system emulator selection, 12 built-in external app adapters (PPSSPP/Gold, Dolphin, melonDS, Mupen64Plus-AE alpha and seven EX-family apps), custom package/activity/MIME registrations for compatible ACTION_VIEW apps; no fallback selection.
- Read-only URI grants, missing-file/runtime errors, controller D-pad/left-stick focus navigation, A/select and B/back, Compose lifecycle state.
- Persistent compact-card preference and runtime settings, privacy/storage explanations, Android-only GitHub update selection.

## Build

Install JDK 17, Android SDK platform 36 and build-tools 36.0.0. Use `./gradlew clean testDebugUnitTest assembleRelease lintRelease` with `JAVA_HOME`, `ANDROID_HOME`, `ANDROID_USER_HOME` and `GRADLE_USER_HOME` set to your development locations. The local isolated setup uses `Scripts/build.sh`. No macOS project build is needed.

`assembleRelease` is **unsigned** without owner signing environment variables. See `Documentation/PRODUCTION_SIGNING.md` for the preferred fingerprint-checked offline signing script. Existing optional Gradle production signing accepts `AKITO_SIGNING_STORE`, `AKITO_SIGNING_STORE_PASSWORD`, `AKITO_SIGNING_ALIAS`, `AKITO_SIGNING_KEY_PASSWORD`. The owner controls the key; do not commit or ship it. Debug builds use Android's development key and are not production releases. Preserve one owner certificate for all future production updates.

`Scripts/package-candidate.py` prepares local review assets and audits source/APK contents. It does not publish. SDK, Java, emulator images, caches, development keys, local logs and device data in `.tools/` are never included in the source archive or APK.

## Runtime scope

See `Documentation/RUNTIME_SUPPORT.md`. Platform identification is broader than playable runtime support. A successful Android activity handoff is recorded as a launch; it does not prove game boot or minutes played. Gameplay is outside Akito in the selected emulator. Original games and external emulator saves stay with their owners.

## Distribution and updates

Android uses its own tag `android-v1.0.0` and APK `Akito-Station-Android-v1.0.0.apk`. Keep the existing macOS release/assets intact. No publishing automation or upload code is provided.

The owner-selected official repository defaults to `leoxcho/akito-station-android`. The checker accepts stable Android tags with matching APK version/name, uploaded state, repository-specific HTTPS download path and a GitHub SHA-256 asset digest. It opens release review in a browser; it does not automatically download/install an APK. Drafts, prereleases, macOS packages, older versions, mismatched names and foreign URLs are excluded.

## Honest limitations

No embedded emulation, Play billing, Android PRO account/entitlement integration, online metadata/cover scraping, cloud sync, emulator-specific save management, ROM archive extraction or multi-disc dependency grouping. Ambiguous disc formats require a recognized folder or a user system correction. Systems without an Android-compatible configured runtime remain library-only. Owner confirmed real Android hardware acceptance. Individual runtime gameplay compatibility remains unverified unless separately recorded. See the candidate reports for actual verification results.

The final public APK is owner production-signed; the permanent private key is never distributed. Physical-device acceptance was confirmed by the owner. The temporary development signing key used for local review was removed after validation. A subsequent clean debug build generates a new development certificate. Review APKs are for temporary local testing; only the owner certificate defines the production update identity.

## Download and license

Download the production-signed [Android 1.0.0 APK](https://github.com/leoxcho/akito-station-android/releases/download/android-v1.0.0/Akito-Station-Android-v1.0.0.apk). See [installation](Documentation/Release/INSTALLATION.md), [runtime support](Documentation/RUNTIME_SUPPORT.md), [privacy](Documentation/PRIVACY.md), and [limitations](Documentation/Release/KNOWN-LIMITATIONS.md). Akito includes no games, ROMs, BIOS/firmware or console encryption keys.

Original code uses [Apache-2.0](LICENSE), with [explicit scope](LICENSE_SCOPE.json). Branding and third-party material retain their existing rights; see [notices](THIRD_PARTY_NOTICES.md).
