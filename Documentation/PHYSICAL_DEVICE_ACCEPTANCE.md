# Owner/device acceptance gates

Before any public release:

1. Supply the long-lived owner signing certificate and configure the official GitHub repository. Sign the release build; verify certificate fingerprint, checksum, APK metadata, ZIP alignment and 16 KB native page alignment. Never include keys/passwords in release assets.
2. Install owner-signed APK on at least one ARM64 phone/handheld with Android 8+ and one current Android 16 device. Verify cold/warm startup, rotate, process death, back stack, gesture insets, grid scrolling and accessibility font scale/TalkBack.
3. Select local and removable SD/USB libraries through Android's picker. Scan synthetic/homebrew or legally owned files; revoke folder permissions, disconnect/reconnect storage and delete/move a game externally. Errors should be readable and metadata retained.
4. Import adjacent covers and changed covers; verify favorite/title/system/card settings persist after restart. Reject malformed and oversized images. Unregister a library and confirm original file hashes are unchanged.
5. Install official PPSSPP, explicitly select it for PSP and launch a legally owned/homebrew single-file game. Confirm content URI permission, game boot, controller play, emulator-owned save/load, exit and return. Repeat PPSSPP Gold only if owned.
6. Validate each custom emulator separately. A registered system is not gameplay support. Check exported activity, MIME type and multi-file dependencies before claiming compatibility.
7. Pair physical Bluetooth and USB controllers. Verify D-pad and analog focus, A/select, B/back, reconnect and external emulator mappings. Keyboard-only and touch-only flows must work.
8. Use fixtures to confirm the official release checker ignores macOS tags/assets, prereleases, older versions, unsigned metadata and unrelated URLs. Test no network, rate limit and no browser. Test a signed same-certificate upgrade after a subsequent Android release exists.
9. Review branding rights, dependency notices, privacy text and support policy. No Android PRO purchases should be advertised until backend verification, payment and restore are implemented and tested.

Emulator validation is not physical-hardware acceptance. No public release/upload/push is authorized by test success.

## Final sign-off matrix (owner fills every row)

Record device/model, Android version, runtime version/package, content format, result and notes. No rows have physical-device sign-off yet.

- [ ] Clean install of final owner-signed APK; cold start and permission-denial recovery.
- [ ] Same-production-certificate upgrade with increased versionCode preserves library, artwork, favorites and settings. LOCAL-REVIEW to production changes certificate and requires a separate clean install.
- [ ] Internal storage library import; duplicates, malformed files, rescans, metadata preservation and original hashes.
- [ ] SD card/USB document provider where available; detach/reconnect, revoked access, unavailable game and multiple-file discs.
- [ ] Artwork import/edit/remove; oversized/malformed image rejection. Integrated online search is optional and absent; use legally sourced local images.
- [ ] Favorites, title/system search, All/Favorites/Recently launched filters and sorting.
- [ ] Settings and per-system runtime persistence after app restart and process death.
- [ ] Bluetooth and USB controller navigation, focus visibility, A/select, B/back and reconnect.
- [ ] PPSSPP PSP game boot/play/save/return; Gold separately if owned.
- [ ] Dolphin GameCube and Wii launch/boot/controller/save/return.
- [ ] melonDS DS scanned-ROM launch, save placement and repeat launch.
- [ ] Mupen64Plus-AE alpha N64 launch/boot/save/return; do not substitute another fork without separate testing.
- [ ] NES.emu NES, Snes9x EX+ SNES, GBC.emu GB and GBC, GBA.emu GBA, MD.emu Genesis, PCE.emu PC Engine and Saturn.emu Saturn: each installed/missing, cold/warm launch, gameplay/controller/save/return.
- [ ] Missing and disabled runtime behavior; unsupported system rejected; unavailable original game preserved as metadata.
- [ ] App restart/state recovery after external emulator exit, runtime cancellation, rotation and OS process termination.
- [ ] Manual Android update check: unpublished channel/error, offline, later legitimate Android update, mixed macOS assets rejected; browser handoff.
- [ ] Small ARM64 phone portrait/landscape, gesture navigation and enlarged fonts.
- [ ] ARM64 tablet portrait/landscape with grid, game dialog and settings scrolling.
- [ ] Production signature fingerprint/checksums, privacy/notices/branding and publication review approved.

No ROM/BIOS/firmware/key downloads are part of this checklist. Gameplay is unverified until the owner records actual results.
