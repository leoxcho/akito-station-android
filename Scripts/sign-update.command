#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
OWNER_KEY="${AKITO_SIGNING_STORE:-signing/akito-android-release.jks}"
if [ ! -f "$OWNER_KEY" ] && [ -f "../Akito Station Android/signing/akito-android-release.jks" ]; then OWNER_KEY="../Akito Station Android/signing/akito-android-release.jks"; fi
test -f "$OWNER_KEY" || { echo "Existing permanent keystore missing; no key will be generated."; exit 1; }
exec python3 Scripts/sign-production.py --version 1.0.5 --keystore "$OWNER_KEY" --fingerprint babd07ffe949cdce48998f1b935778c3dba5285d69b2b48d20e67f5b6187b9b2
