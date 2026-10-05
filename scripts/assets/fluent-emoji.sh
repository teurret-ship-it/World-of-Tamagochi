#!/usr/bin/env bash
# Imports Microsoft Fluent Emoji (3D style, MIT licence) used by the app and
# converts them to WebP. Re-run after editing the list; every file must also be
# listed in docs/ASSETS.md. Source: github.com/microsoft/fluentui-emoji
set -euo pipefail

OUT="$(cd "$(dirname "$0")/../.." && pwd)/core/ui/src/main/res/drawable-nodpi"
BASE="https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets"
mkdir -p "$OUT"
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

# "Folder name in the repo|file stem|resource name"
while IFS='|' read -r folder stem name; do
  [ -z "$folder" ] && continue
  curl -fsSL --retry 4 --retry-all-errors --retry-delay 2 -o "$tmp/$stem.png" "$BASE/${folder// /%20}/3D/${stem}_3d.png"
  ffmpeg -nostdin -v error -y -i "$tmp/$stem.png" -c:v libwebp -quality 88 -compression_level 6 "$OUT/$name.webp"
  echo "$name.webp <- $folder"
done <<'LIST'
Red apple|red_apple|item_apple
Soap|soap|item_soap
Bubbles|bubbles|fx_bubbles
Soccer ball|soccer_ball|item_ball
Light bulb|light_bulb|item_light
Crescent moon|crescent_moon|item_moon
Coin|coin|reward_coin
Glowing star|glowing_star|reward_star
Red heart|red_heart|fx_heart
Sparkles|sparkles|fx_sparkles
Zzz|zzz|fx_zzz
LIST
