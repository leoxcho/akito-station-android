# Android runtime adapters — 1.0.2 candidate

Akito is a frontend. No emulator binaries, ROMs, BIOS, firmware, keys or paid apps are bundled. Install compatible apps independently from their official publisher and complete their first-run setup. Consoles automatically lists built-in adapters. A single installed compatible app is used automatically; choose and persist a preference when several are available. Package visibility declarations detect each built-in app; missing apps produce an installation message. Launches pass the selected document URI and read permission, never shell commands or raw filesystem paths. A successful handoff updates recent history; it does not establish game boot or play time.

| App | Android package | Systems | Exported activity |
|---|---|---|---|
| DuckStation | com.github.stenzek.duckstation | PlayStation / PS1 | com.github.stenzek.duckstation.EmulationActivity |
| PPSSPP | org.ppsspp.ppsspp | PSP | org.ppsspp.ppsspp.PpssppActivity |
| PPSSPP Gold | org.ppsspp.ppssppgold | PSP | org.ppsspp.ppsspp.PpssppActivity |
| Dolphin | org.dolphinemu.dolphinemu | GameCube, Wii | org.dolphinemu.dolphinemu.ui.main.MainActivity |
| melonDS | me.magnum.melonds | Nintendo DS | me.magnum.melonds.ui.emulator.EmulatorActivity |
| Mupen64Plus-AE alpha | org.mupen64plusae.v3.alpha | Nintendo 64 | paulscode.android.mupen64plusae.SplashActivity |
| NES.emu | com.explusalpha.NesEmu | NES | com.imagine.BaseActivity |
| Snes9x EX+ | com.explusalpha.Snes9xPlus | SNES | com.imagine.BaseActivity |
| GBC.emu | com.explusalpha.GbcEmu | GB, GBC | com.imagine.BaseActivity |
| GBA.emu | com.explusalpha.GbaEmu | GBA | com.imagine.BaseActivity |
| MD.emu | com.explusalpha.MdEmu | Mega Drive/Genesis | com.imagine.BaseActivity |
| PCE.emu | com.PceEmu | PC Engine | com.imagine.BaseActivity |
| Saturn.emu | com.explusalpha.SaturnEmu | Saturn | com.imagine.BaseActivity |

DuckStation uses explicit EmulationActivity, ACTION_VIEW and the `bootPath` string extra containing the content URI, with `resumeState=false`. Data and ClipData carry a read-only grant. All others use ACTION_VIEW except melonDS, which uses `me.magnum.melonds.LAUNCH_ROM`. All supply data plus ClipData read grants. No new adapter has gameplay verification. Automated tests establish intent construction, supported-system restrictions and missing-package detection only. Codex did not independently exercise a third-party runtime with a real game. The owner confirmed general real-device app acceptance; no per-runtime gameplay matrix was supplied.

Primary interface evidence:

- [PPSSPP frontend documentation](https://www.ppsspp.org/docs/reference/front-end-integration/).
- [Dolphin StartupHandler](https://github.com/dolphin-emu/dolphin/blob/a0c3ab78d391ff4703fed5a1d583e947dc8e7750/Source/Android/app/src/main/java/org/dolphinemu/dolphinemu/utils/StartupHandler.kt): reads ClipData or intent data and launches emulation.
- [melonDS integration instructions](https://github.com/rafaelvcaetano/melonDS-android/blob/c42995caff0144f37ad9791dc9bc96d82e8a6f65/README.md): read-granted ROM URI; scan the game in melonDS first. Saves and folder access remain runtime-owned.
- [Mupen64Plus-AE source](https://github.com/mupen64plus-ae/mupen64plus-ae/tree/33bf7021549d7873e976884370fbd1a8e7333bc7): SplashActivity passes document data to GalleryActivity. This package is the upstream alpha app, not an assertion of compatibility with FZ or other forks.
- [EX-family source](https://github.com/Rakashazi/emu-ex-plus-alpha/tree/1c12fac5ce49badaadff2e2f210dcc30b89f4943): shared exported BaseActivity consumes content-URI data; metadata defines packages, including PCE's Android override.

Version changes in third-party apps may break these interfaces. Multi-file CUE discs, archives, BIOS-dependent systems and saves may require granting the emulator its own library folder access; a URI grant for one game does not grant sibling files. Test legal single-file/homebrew content first, then representative multi-file content. Install required BIOS yourself where lawful. Akito never supplies or copies it.

PS2/PS3/PS4/Vita/3DS/Dreamcast/Wii U/Xbox/Xbox 360/Switch are library categories, not built-in Android gameplay claims. Compatible custom external apps can be registered with package, exported activity and MIME type. Unsupported systems, embedded rendering, emulator-owned save managers and additional adapters are post-1.0 work. SkyEmu and RetroArch have no built-in adapter.

Online cover search is independent of runtime support. All listed runtime systems and recognized library systems can search Libretro catalogs where available. See COVER_SEARCH.md.


## v1.0.2 discovery and interface audit

All 12 existing adapters retain their verified package/activity/action mappings. The October 3 audit refreshed upstream Dolphin manifest/StartupHandler, melonDS manifest, Mupen manifest/SplashActivity and all seven EX-family metadata entries plus BaseActivity. PPSSPP/Gold match the [publisher frontend contract](https://www.ppsspp.org/docs/reference/front-end-integration/). No stale interface was found in this set. Mupen detection applies to the upstream alpha package, not FZ. The audit establishes source contracts, not third-party gameplay acceptance.

DuckStation's [official README](https://github.com/stenzek/duckstation#android) names `com.github.stenzek.duckstation`. The publisher removed Android source; the [last Android manifest](https://github.com/stenzek/duckstation/blob/81da9be2d1040665ebfaf2db6d7fdb710a48a383/android/app/src/main/AndroidManifest.xml) exports EmulationActivity, and its [launch handler](https://github.com/stenzek/duckstation/blob/81da9be2d1040665ebfaf2db6d7fdb710a48a383/android/app/src/main/java/com/github/stenzek/duckstation/EmulationActivity.java) consumes bootPath. The previous official APK URL returns 404. Source evidence is the last published Android contract. The owner will test physical hardware separately; current installed-binary behavior was not exercised here. Akito checks the actual installed package and enabled/exported activity rather than assuming installation.

PS1 scan formats: CUE, CHD, M3U, BIN, IMG, ISO, PBP, ECM, MDS, MDF, CCD and EXE. These formats can overlap other systems; use a PS1/PSX/PlayStation folder or choose a metadata override. Ambiguous files remain Unidentified. BIN/IMG/EXE are scanned only inside a PS1 folder; known BIOS/key names and firmware folders are excluded. Use CUE rather than individual tracks where applicable. Multi-file discs/playlists require the emulator's own folder access. Format recognition does not promise that every DuckStation build supports every container.

Add Emulator → Select Console → Select Installed Emulator → Save. Known adapters are already configured; Save records the preference without creating a duplicate. If one app is detected it is preselected. App labels/icons come from PackageManager. Other launchable document handlers are labeled for the user to confirm console support, because Android does not expose an emulator-console capability registry. Discovery uses narrowly declared package/MAIN-launcher/VIEW queries; QUERY_ALL_PACKAGES is not requested. Android visibility restrictions can hide apps.

Advanced / Custom Emulator preserves package, exported activity, action and MIME configuration. Launches remain content URI/read-only. Existing action-less custom entries migrate to ACTION_VIEW. Manual known-emulator registrations migrate to the built-in entry and preference without duplicates; existing preference keys, library schema, consent and storage grants remain unchanged.
