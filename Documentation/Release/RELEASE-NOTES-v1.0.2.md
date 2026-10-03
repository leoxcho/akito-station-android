# Android v1.0.2

VersionName 1.0.2, versionCode 3, package app.akitostation.android.

- Added DuckStation / PlayStation detection and bootPath content-URI handoff.
- Added contextual PS1 format recognition without treating generic BIN firmware as games.
- Added readable installed status, automatic single-runtime selection and a chooser with a saved console preference.
- Simplified Add Emulator to console and installed-app selection; known adapters avoid duplicate registrations. Custom configuration is behind Advanced.
- Audited all existing 12 adapters against publisher contracts and tightened enabled/exported checks.
- Preserved library schema, artwork, favorites, consent, settings and preference keys. Legacy custom actions default to ACTION_VIEW.

Validation: 139 JVM tests; phone/tablet 7/7 each; release lint zero errors; ARM64 and 16 KB ELF/ZIP alignment; source/APK security scan; production v1.0.1 → v1.0.2 upgrade with library/artwork/settings/consent/preference/grant preservation. Same permanent production certificate. Manual known-runtime preferences migrate without duplicates. The owner tests physical hardware separately; no gameplay compatibility claim is made.

APK SHA-256: dfad977724388e0c09de81e25be73ecc66e69fed8dc9ecdb76eb00801d49e3c0
Certificate SHA-256: babd07ffe949cdce48998f1b935778c3dba5285d69b2b48d20e67f5b6187b9b2
