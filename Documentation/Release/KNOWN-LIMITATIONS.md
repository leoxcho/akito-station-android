# Known limitations

Online cover search uses Libretro catalogs; catalog availability/coverage varies. The macOS TheGamesDB fallback is not implemented on Android. All systems includes covers from other editions and can take longer. Results are limited to 120. Network failures offer retry/local import; no automatic bulk scraping. Consent revocation cannot recall already dispatched requests.

The v1.0.1 upgrade and phone/tablet UI were tested on ARM64 API 36 emulators, with JVM Android behavior tested on APIs 26 and 35. No new physical-device, individual-runtime gameplay or individual-controller compatibility matrix was independently tested. Existing external-runtime routing remains unchanged. Multi-file discs, saves and BIOS setup remain runtime-specific. PRO, cloud backup, embedded rendering and desktop save/profile management remain future work.
