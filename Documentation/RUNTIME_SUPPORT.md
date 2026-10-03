# Android runtime adapters — 1.0.0

Akito is a frontend. No emulator binaries, ROMs, BIOS, firmware, keys or paid apps are bundled. Install compatible apps independently from their official publisher and complete their first-run setup. Select each system's runtime explicitly in Consoles. Package visibility declarations detect each built-in app; missing apps produce an installation message. Launches pass the selected document URI and read permission, never shell commands or raw filesystem paths. A successful handoff updates recent history; it does not establish game boot or play time.

| App | Android package | Systems | Exported activity |
|---|---|---|---|
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

All use ACTION_VIEW except melonDS, which uses `me.magnum.melonds.LAUNCH_ROM`. All supply data plus ClipData read grants. No new adapter has gameplay verification. Automated tests establish intent construction, supported-system restrictions and missing-package detection only. Codex did not independently exercise a third-party runtime with a real game. The owner confirmed general real-device app acceptance; no per-runtime gameplay matrix was supplied.

Primary interface evidence:

- [PPSSPP frontend documentation](https://www.ppsspp.org/docs/reference/front-end-integration/).
- [Dolphin StartupHandler](https://github.com/dolphin-emu/dolphin/blob/a0c3ab78d391ff4703fed5a1d583e947dc8e7750/Source/Android/app/src/main/java/org/dolphinemu/dolphinemu/utils/StartupHandler.kt): reads ClipData or intent data and launches emulation.
- [melonDS integration instructions](https://github.com/rafaelvcaetano/melonDS-android/blob/c42995caff0144f37ad9791dc9bc96d82e8a6f65/README.md): read-granted ROM URI; scan the game in melonDS first. Saves and folder access remain runtime-owned.
- [Mupen64Plus-AE source](https://github.com/mupen64plus-ae/mupen64plus-ae/tree/33bf7021549d7873e976884370fbd1a8e7333bc7): SplashActivity passes document data to GalleryActivity. This package is the upstream alpha app, not an assertion of compatibility with FZ or other forks.
- [EX-family source](https://github.com/Rakashazi/emu-ex-plus-alpha/tree/1c12fac5ce49badaadff2e2f210dcc30b89f4943): shared exported BaseActivity consumes content-URI data; metadata defines packages, including PCE's Android override.

Version changes in third-party apps may break these interfaces. Multi-file CUE discs, archives, BIOS-dependent systems and saves may require granting the emulator its own library folder access; a URI grant for one game does not grant sibling files. Test legal single-file/homebrew content first, then representative multi-file content. Install required BIOS yourself where lawful. Akito never supplies or copies it.

PS1/PS2/PS3/PS4/Vita/3DS/Dreamcast/Wii U/Xbox/Xbox 360/Switch are library categories, not built-in Android gameplay claims. Compatible custom external apps can be registered with package, exported activity and MIME type. Unsupported systems, embedded rendering, emulator-owned save managers and additional adapters are post-1.0 work. SkyEmu, DuckStation and RetroArch were not added on guessed interfaces.
