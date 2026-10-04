#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
test -f signing/akito-android-release.jks || { echo "Existing permanent keystore missing; no key will be generated."; exit 1; }
exec python3 Scripts/sign-production.py --version 1.0.3 --keystore signing/akito-android-release.jks --fingerprint babd07ffe949cdce48998f1b935778c3dba5285d69b2b48d20e67f5b6187b9b2
