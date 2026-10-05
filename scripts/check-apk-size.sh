#!/usr/bin/env bash
# APK size budget from CLAUDE.md section 4: target <= 15 MB, hard limit 30 MB.
set -euo pipefail

apk="${1:?usage: check-apk-size.sh <path-to-apk>}"
target=$((15 * 1024 * 1024))
limit=$((30 * 1024 * 1024))

bytes=$(stat -c %s "$apk")
mb=$(awk "BEGIN { printf \"%.2f\", $bytes / 1048576 }")
echo "APK size: ${mb} MB (${bytes} B)"

if [ "$bytes" -gt "$limit" ]; then
  echo "::error::APK exceeds the 30 MB hard limit"
  exit 1
fi
if [ "$bytes" -gt "$target" ]; then
  echo "::warning::APK exceeds the 15 MB target"
fi
