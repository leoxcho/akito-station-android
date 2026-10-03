#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
export JAVA_HOME="$PWD/.tools/jdk-17.0.20.1+1/Contents/Home"
export ANDROID_HOME="$PWD/.tools/sdk"
export ANDROID_USER_HOME="$PWD/.tools/android-user"
export GRADLE_USER_HOME="$PWD/.tools/gradle-user"
mkdir -p "$PWD/.tools/tmp"
export TMPDIR="$PWD/.tools/tmp"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
exec ./gradlew "$@"
