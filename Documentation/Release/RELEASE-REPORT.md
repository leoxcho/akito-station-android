# Android production release verification

Version 1.0.0; versionCode 1; package app.akitostation.android; Android 8+ ARM64.

Production APK: Akito-Station-Android-v1.0.0.apk
APK SHA-256: 7f3e67529dce3b13d2d0ec51d4a5d2e77d3b58cd95c1142448f4c6e58eeb9671
Production certificate SHA-256: babd07ffe949cdce48998f1b935778c3dba5285d69b2b48d20e67f5b6187b9b2

Owner confirmed physical-device acceptance in this session. This records the owner's general acceptance, not independent per-runtime gameplay verification. Signed payload exactly matches the previously tested unsigned release build. Prior validation: clean build, 85 JVM tests, phone 3/3 and tablet 3/3 Compose UI tests; lint zero errors and 20 non-blocking warnings.

Final production verification: APK signature and expected certificate PASS; no Android Debug certificate; single signer; 16 KB ZIP alignment PASS; ARM64-only packaging PASS; version/package/non-debuggable PASS; source and release content audit PASS. Secrets/signing/cache/game payloads excluded by allowlist and .gitignore. Permanent private key remains only in ignored signing/akito-android-release.jks. Keep an encrypted offline backup under owner control. LOCAL-REVIEW is internal only and excluded from public staging.

No macOS project writes were issued. No publishing, uploads, remote changes or GitHub releases were performed during signing and local preparation. The official Android channel is prepared for leoxcho/akito-station-android. Public availability/update checks require later owner-approved publication. PRO and broader desktop/runtime parity remain optional post-1.0.

See PUBLICATION-MANIFEST.json for the exact repository files and release assets. Existing third-party license texts and owner-controlled branding/application notices are preserved; no new license grant is invented. The owner subsequently authorized publication after independent verification. This is the pre-publication verification snapshot.

ANDROID PRODUCTION RELEASE: READY TO PUBLISH
