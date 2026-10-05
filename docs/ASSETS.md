# Assets and licenses

Every asset shipped in the app or the server, with its origin and license.
Only own, procedural or CC0 assets are allowed (CLAUDE.md section 2).

| Asset | Where | Origin | License |
|---|---|---|---|
| Launcher icon (speckled egg) | `app/src/main/res/drawable/ic_launcher_foreground.xml` | drawn for this project | own |
| Pet rig (body, ears, tail, face, markings) | `core/ui/.../PetArt.kt` | procedural (Compose Canvas), genome-driven | own |
| Palette and typography | `core/designsystem` | designed for this project; platform default font | own / Apache 2.0 (Roboto) |
| Item, reward and effect images (`item_*`, `reward_*`, `fx_*`) | `core/ui/src/main/res/drawable-nodpi/` | Microsoft Fluent Emoji, 3D style, via `scripts/assets/fluent-emoji.sh` (source folder per file in the script) | MIT (`docs/licenses/FluentEmoji-MIT.txt`) |
| Fredoka variable font | `core/designsystem/src/main/res/font/fredoka.ttf` | github.com/google/fonts `ofl/fredoka` | SIL OFL 1.1 (`docs/licenses/Fredoka-OFL.txt`) |
| UI and reward sounds (sound sprite, kit 03) | generated at build time into `core/ui/build/generated/snd` | SND (snd.dev), npm `snd-lib@1.2.4`, SHA-256 pinned | SND terms: free for commercial use; not redistributed standalone, so never committed |
