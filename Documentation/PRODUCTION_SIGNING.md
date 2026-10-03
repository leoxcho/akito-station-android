# Owner-controlled production signing

The permanent production key is stored privately in ignored signing/akito-android-release.jks. Its public fingerprint is recorded in the production release metadata. Normal builds remain unsigned unless the existing optional Gradle AKITO_SIGNING_* environment configuration is explicitly supplied. Prefer the standalone signing script below so build/test operations never need production credentials.

From the Android project root, with Java keytool available:

```sh
mkdir -p signing
chmod 700 signing
keytool -genkeypair -keystore signing/akito-android-release.jks -alias akito-android -keyalg RSA -keysize 4096 -validity 10000
chmod 600 signing/akito-android-release.jks
keytool -list -v -keystore signing/akito-android-release.jks -alias akito-android
python3 Scripts/sign-production.py --keystore signing/akito-android-release.jks --fingerprint OWNER_SHA256_FROM_KEYTOOL
```

keytool prompts for passwords and certificate identity. Do not place passwords in shell commands, source, logs, or screenshots. Keep an encrypted offline backup of this long-lived key and record its alias and SHA-256 certificate fingerprint securely. Losing the key prevents same-certificate upgrades outside a separately established store signing service. The signing script prompts privately, passes passwords through child environment variables, checks the expected certificate, rejects Android Debug certificates, verifies APK signatures and 16 KB ZIP alignment, and refuses overwriting an existing output.

Expected private location: `signing/akito-android-release.jks`. All signing folders, keystore formats, environment files, and `production-candidate/` are ignored. The public source export is allowlisted and excludes these directories. Never attach or publish them.

Output: `production-candidate/Akito-Station-Android-v1.0.0.apk`, plus `SIGNATURE.txt`. Hash with `shasum -a 256` and complete the device checklist before publication review. Signing does not publish anything. LOCAL-REVIEW.apk is development-signed and cannot upgrade to the production key. Uninstalling it removes private metadata; preserve original library files and reimport. Test future upgrades using the same production certificate and increasing versionCode.
