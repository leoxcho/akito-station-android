# Android v1.0.4 · build 5

- Appearance → App Logo offers seven persistent A/S logo variants. Predefined launcher aliases change the launcher entry; refresh timing is launcher-dependent. The installed APK is never modified.
- Landscape uses compact header navigation, a single search/control row, and expandable filters, leaving the main area to the library. Portrait retains bottom navigation.
- Game cards have 20dp horizontal and 24dp vertical gaps. The five saved density choices adapt to viewport width and font size, with a maximum of the selected number of columns. Covers retain their aspect ratios.
- Library schema, scanners, emulator mappings, launch adapters and artwork services are unchanged. Debug validation uses a separate test-only application ID to preserve the production installation.

Validated with 157 JVM tests, release lint and production certificate continuity. Emulator checks cover portrait and landscape on phone, narrow, tablet and large layouts, all five grid preferences, persistent search/filter state and scroll-region retention. No physical-device gameplay claim is made.
