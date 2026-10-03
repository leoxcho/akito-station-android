# Separate Android update channel

Owner-selected official repository: `leoxcho/akito-station-android`.

The app defaults to this repository, uses GitHub's HTTPS releases API, and accepts only non-draft/non-prerelease `android-vMAJOR.MINOR.PATCH` releases with exact `Akito-Station-Android-vMAJOR.MINOR.PATCH.apk` assets, positive size, uploaded state, GitHub SHA-256 digest, matching repository/tag/download URL and a newer versionCode. It opens the release page for owner-directed review; it does not download or install APKs. macOS tags, ZIPs and DMGs cannot qualify. Configuration may be changed to another Android repository for testing, but the same Android asset/tag constraints apply.

The official public Android repository is https://github.com/leoxcho/akito-station-android. Stable releases use the Android tag and asset conventions below; GitHub errors are displayed if the endpoint is unavailable.

Publication review configuration:
- Repository: leoxcho/akito-station-android
- Tag: android-v1.0.0
- APK asset: Akito-Station-Android-v1.0.0.apk (owner-signed only)
- versionCode: 1; next 1.0.1 uses 2; formula `(major-1)*10000 + minor*100 + patch + 1`.
- Add SHA256SUMS and reviewed release notes; never upload LOCAL-REVIEW, UNSIGNED, keystores or local logs.

Before future updates, test offline, rate-limit, 404, empty releases, mixed macOS/Android metadata, digest absence, malformed URLs and a same-certificate upgrade. Owner-approved repository configuration is prepared locally; live endpoint verification is performed after the owner-authorized release is published.
