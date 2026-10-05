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
Cookie|cookie|item_cookie
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
Gear|gear|ui_gear
Construction|construction|race_hurdle
Droplet|droplet|race_puddle
High voltage|high_voltage|race_boost
Chequered flag|chequered_flag|race_finish
1st place medal|1st_place_medal|medal_gold
2nd place medal|2nd_place_medal|medal_silver
3rd place medal|3rd_place_medal|medal_bronze
Trophy|trophy|medal_author
Stopwatch|stopwatch|race_timer
Shopping bags|shopping_bags|ui_shop
Ribbon|ribbon|wear_ribbon
Cherry blossom|cherry_blossom|wear_blossom
Bell|bell|wear_bell
Glasses|glasses|wear_glasses
Billed cap|billed_cap|wear_cap
Scarf|scarf|wear_scarf
Butterfly|butterfly|wear_butterfly
Sunglasses|sunglasses|wear_sunglasses
Graduation cap|graduation_cap|wear_grad_cap
Goggles|goggles|wear_goggles
Headphone|headphone|wear_headphones
Gem stone|gem_stone|wear_gem
Top hat|top_hat|wear_top_hat
Crown|crown|wear_crown
LIST
