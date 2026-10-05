#!/usr/bin/env bash
# Installs the Android SDK pieces this project needs into $ANDROID_HOME
# (default ~/android-sdk) and writes local.properties.
#
# Needs network access to dl.google.com. Where that host is blocked, use the
# JVM-only build instead:  ./gradlew jvmCheck -Pwot.jvmOnly=true  (ADR-002).
set -euo pipefail

SDK="${ANDROID_HOME:-$HOME/android-sdk}"
TOOLS_VERSION="13114758" # commandlinetools-linux, see developer.android.com/studio#command-tools
PLATFORM="platforms;android-36"
BUILD_TOOLS="build-tools;36.0.0"

if ! curl -fsSI https://dl.google.com >/dev/null 2>&1; then
  echo "dl.google.com is unreachable - allow it in the environment's network policy," >&2
  echo "or build without Android: ./gradlew jvmCheck -Pwot.jvmOnly=true" >&2
  exit 1
fi

mkdir -p "$SDK/cmdline-tools"
if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
  tmp=$(mktemp -d)
  curl -fsSL -o "$tmp/tools.zip" "https://dl.google.com/android/repository/commandlinetools-linux-${TOOLS_VERSION}_latest.zip"
  unzip -q "$tmp/tools.zip" -d "$tmp"
  rm -rf "$SDK/cmdline-tools/latest"
  mv "$tmp/cmdline-tools" "$SDK/cmdline-tools/latest"
  rm -rf "$tmp"
fi

yes | "$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" --licenses >/dev/null
"$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" "platform-tools" "$PLATFORM" "$BUILD_TOOLS"

root="$(cd "$(dirname "$0")/.." && pwd)"
echo "sdk.dir=$SDK" > "$root/local.properties"
echo "Android SDK ready in $SDK"
