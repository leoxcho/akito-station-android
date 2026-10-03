# Android 1.0.1 release verification

Package app.akitostation.android, versionCode 2, versionName 1.0.1. Native Kotlin/Compose cover search uses Libretro Thumbnails official platform catalogs, local normalized whole-token matching, an adaptive results grid and explicit Apply cover. Consent defaults off, is disclosed before online access and can be revoked in Settings. Local import remains available. No TheGamesDB fallback or Google search. Coverage depends on provider catalogs; all supported runtime platforms and other recognized systems have mappings.

Clean release/debug build passed. JVM suite passed (final count recorded in PUBLIC-RELEASE-AUDIT.json), including consent, normalization, parsing, caching, bounded download, failures/offline behavior, artwork persistence and schema-1 compatibility. Phone 1080×1920/420 dpi and tablet 1600×1920/240 dpi each passed 5/5 Compose tests, including consent cancellation, D-pad key dispatch, live provider search/select/apply and immediate card refresh. Existing local importer/storage/runtime tests passed. No physical-device gameplay testing was performed for this update. Controller validation covers simulated navigation and existing input mapping tests; individual physical controllers remain device-specific.

Production v1.0.0 was downloaded anonymously from GitHub and installed on an isolated ARM64 API 36 AVD. v1.0.1 installed directly over it using the same permanent certificate: Success. The schema-1 database, synthetic titles/system overrides/favorites/recent/library entries, cover bytes and preference bytes remained unchanged across install and startup. A real SAF folder grant established by v1.0.0 remained persisted, and the upgraded app scanned a new synthetic header file through it. Unavailable fixture roots reported errors while preserving metadata. Existing non-test AVD data was preserved.

APK: Akito-Station-Android-v1.0.1.apk
SHA-256: 8cb52ca9ba20a028208d72be9041e4ad3eaf0430435aa930f9572bbbc8bc1ad1
Production certificate SHA-256: babd07ffe949cdce48998f1b935778c3dba5285d69b2b48d20e67f5b6187b9b2
Single production signer, v2/v3 APK signatures, non-debuggable, exact tested unsigned payload match, ARM64-only packaging and 16 KB ELF/ZIP alignment passed. Lint: zero errors, 22 warnings (dependency updates, API style, ChromeOS ABI, Compose modifier conventions and synchronous consent preference commit).

Allowlisted source and APK security/content audit passed with no prohibited game/firmware/key/secret/private-signing content. The keystore was neither copied nor published. Passwords were entered through the existing secure local Terminal workflow. macOS sources were read only and no macOS write/upload command was issued.

Publication and anonymous post-publication verification are pending until recorded in POST-PUBLICATION-VERIFICATION.json. This document does not assert that GitHub publication has completed.
