# Akito Station for Android

Native Kotlin / Jetpack Compose edition of Akito Station. Android 8.0 (API 26) or newer; ARM64 packaging. Version 1.0.2, build 3.

The Public edition provides a unified, local game library and explicit external emulator handoff. No emulator, game, BIOS, console firmware or encryption key is bundled. The dark navy/violet, red/cyan palette, Station logo, collection grid and library navigation follow the macOS Public reference. Android uses document-provider access instead of desktop filesystem paths.

## Implemented

- Adaptive cover grid, search by title/system, All games / Favorites / Recently launched, system filtering and title/recent/system sorting.
- Consent-gated Search Cover Art, platform-aware Libretro matching, native results selection and immediate card refresh; local cover import remains available.
- SQLite metadata persistence, stable document identities, favorites, launch timestamps, editable title/system and bounded cover-image import.
- Multiple folder libraries via Android Storage Access Framework; recursive scanning, cancellation and bounded traversal; sibling artwork discovery. Failed/offline scans preserve existing metadata and never delete originals.
- Automatic discovery for 13 built-in adapters: DuckStation (PS1), PPSSPP/Gold, Dolphin, melonDS, upstream Mupen64Plus-AE alpha and seven EX-family apps. A sole installed compatible runtime launches automatically; multiple apps use a chooser with a saved console preference.
- Simple Add Emulator: console → installed app → Save. Labels/icons and duplicate prevention; technical action/package/activity/MIME fields are under Advanced / Custom Emulator.
- Read-only URI grants, missing-file/runtime errors, controller D-pad/left-stick focus navigation, A/select and B/back, Compose lifecycle state.
- Persistent compact-card preference and runtime settings, privacy/storage explanations, Android-only GitHub update selection.

## Build

Install JDK 17, Android SDK platform 36 and build-tools 36.0.0. Use `./gradlew clean testDebugUnitTest assembleRelease lintRelease` with `JAVA_HOME`, `ANDROID_HOME`, `ANDROID_USER_HOME` and `GRADLE_USER_HOME` set to your development locations. The local isolated setup uses `Scripts/build.sh`. No macOS project build is needed.

`assembleRelease` is **unsigned** without owner signing environment variables. See `Documentation/PRODUCTION_SIGNING.md` for the preferred fingerprint-checked offline signing script. Existing optional Gradle production signing accepts `AKITO_SIGNING_STORE`, `AKITO_SIGNING_STORE_PASSWORD`, `AKITO_SIGNING_ALIAS`, `AKITO_SIGNING_KEY_PASSWORD`. The owner controls the key; do not commit or ship it. Debug builds use Android's development key and are not production releases. Preserve one owner certificate for all future production updates.

`Scripts/package-update.py` audits/stages the 1.0.2 source and unsigned candidate. `Scripts/package-candidate.py` prepares legacy local review assets and audits source/APK contents. It does not publish. SDK, Java, emulator images, caches, development keys, local logs and device data in `.tools/` are never included in the source archive or APK.

## Runtime scope

See `Documentation/RUNTIME_SUPPORT.md`. Platform identification is broader than playable runtime support. A successful Android activity handoff is recorded as a launch; it does not prove game boot or minutes played. Gameplay is outside Akito in the selected emulator. Original games and external emulator saves stay with their owners.

## Distribution and updates

Android uses its own update tag `android-v1.0.2` and APK `Akito-Station-Android-v1.0.2.apk`. Keep the existing macOS release/assets intact. No publishing automation or upload code is provided.

The owner-selected official repository defaults to `leoxcho/akito-station-android`; the live repository is https://github.com/leoxcho/akito-station-android. The checker accepts stable Android tags with matching APK version/name, uploaded state, repository-specific HTTPS download path and a GitHub SHA-256 asset digest. It opens release review in a browser; it does not automatically download/install an APK. Drafts, prereleases, macOS packages, older versions, mismatched names and foreign URLs are excluded.

## Honest limitations

No embedded emulation, Play billing, Android PRO account/entitlement integration, bulk online metadata scraping, cloud sync, emulator-specific save management, ROM archive extraction or multi-disc dependency grouping. Ambiguous disc formats require a recognized folder or a user system correction. Systems without an Android-compatible configured runtime remain library-only. External emulator intent compatibility and removable storage need owner hardware acceptance. See the candidate reports for actual verification results.

The final public APK is owner production-signed; the permanent private key is never distributed. Physical-device acceptance for 1.0.0 was confirmed by the owner; this update was validated on ARM64 emulators. The temporary development signing key used for local review was removed after validation. A subsequent clean debug build generates a new development certificate. Review APKs are for temporary local testing; only the owner certificate defines the production update identity.

## Android 1.0.1 update

Open a game, choose **Search Cover Art**, review the online disclosure, and allow search. Search the prefilled title and system, select a cover in the native adaptive grid, then choose **Apply cover**. All systems broadens the catalog search. Local **Change cover** remains available. Settings → Online cover art reviews or disables permission. Online search uses Libretro Thumbnails official catalogs and searches titles locally. Downloaded covers are stored privately; originals remain unchanged.

Version 1.0.1 / code 2 retains app.akitostation.android and database schema 1. Install over production 1.0.0 only after same-certificate upgrade verification; do not uninstall to update. See the 1.0.1 release verification report and published release audit for validation evidence.


## Android 1.0.2 update

DuckStation / PlayStation detection, automatic runtime routing, a multiple-emulator chooser, readable Consoles status and a simple Add Emulator picker. Technical registration is under Advanced / Custom Emulator. Existing manual known-runtime registrations migrate without duplicates; library schema, artwork, favorites, consent, preferences and storage grants are preserved. Install over production v1.0.1 without uninstalling.

Validated with 139 JVM tests, phone/tablet 7/7 each, production signing, same-certificate upgrade and source/APK security/alignment checks. Physical hardware gameplay is being tested separately by the owner. See [runtime support](Documentation/RUNTIME_SUPPORT.md), [release notes](Documentation/Release/RELEASE-NOTES-v1.0.2.md) and [verification report](Documentation/Release/RELEASE-REPORT-v1.0.2.md).
