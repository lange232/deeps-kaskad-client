#!/usr/bin/env bash
# Renders the two-line "DEEPS / KASKAD" wordmark into the launcher resources.
#
# Text needs the app's own Inter font, and a browser refuses to load fonts into an
# <img>, so these icons are rendered from an HTML page instead of an SVG like
# artwork/icon.svg. Everything else about the pipeline is the same: 4x supersampling
# with headless chromium, then downscale and encode to webp with Pillow.
#
# Usage: scripts/render-text-icons.sh [--preview DIR]
set -euo pipefail
cd "$(dirname "$0")/.."

preview_dir=""
if [ "${1:-}" = "--preview" ]; then
    preview_dir="${2:?preview directory required}"
    mkdir -p "$preview_dir"
fi

mkdir -p build
work=$(mktemp -d "${PWD}/build/text-icon.XXXXXX")
trap 'rm -rf "$work"' EXIT

font_file="$PWD/app/src/main/res/font/inter.ttf"
renderer=""
for candidate in chromium chromium-browser google-chrome; do
    if command -v "$candidate" >/dev/null 2>&1; then
        renderer="$candidate"
        break
    fi
done
if [ -z "$renderer" ]; then
    echo "No headless browser found (chromium/chrome) — text icons need one" >&2
    exit 1
fi

# $1 canvas css px, $2 font px, $3 tile (1/0), $4 out png
render() {
    local size="$1" font_size="$2" tile="$3" out="$4"
    local scale=4
    local px=$((size * scale))
    local radius=$((px * 22 / 100))
    local background="#21272A"
    [ "$tile" -eq 0 ] && background="transparent"

    python3 - "$px" "$font_size" "$scale" "$radius" "$background" "$font_file" > "$work/page.html" <<'PY'
import base64
import sys

px, font_size, scale, radius, background, font_file = sys.argv[1:7]
# Inlined as base64: a browser screenshots the page before a file:// webfont
# finishes loading and silently falls back to a serif face.
font = "data:font/ttf;base64," + base64.b64encode(open(font_file, "rb").read()).decode()
font_px = float(font_size) * float(scale)
html = f"""<!doctype html>
<html><head><meta charset="utf-8"><style>
@font-face {{ font-family: InterLocal; src: url('{font}'); }}
html,body{{margin:0;padding:0;background:transparent}}
.tile{{width:{px}px;height:{px}px;background:{background};border-radius:{radius}px;
 display:flex;flex-direction:column;align-items:center;justify-content:center;
 font-family:InterLocal;font-weight:700;letter-spacing:0.01em;line-height:0.98;user-select:none}}
.w{{color:#FFFFFF;font-size:{font_px}px}}
.k{{color:#FAB283;font-size:{font_px}px}}
</style></head><body>
<div class="tile"><div class="w">DEEPS</div><div class="k">KASKAD</div></div>
</body></html>
"""
sys.stdout.write(html)
PY

    "$renderer" --headless --no-sandbox --disable-gpu --hide-scrollbars \
        --force-device-scale-factor=1 --allow-file-access-from-files --virtual-time-budget=4000 \
        --default-background-color=00000000 --window-size="$px,$px" \
        --screenshot="$work/page.png" "file://$work/page.html" >/dev/null 2>&1

    python3 - "$work/page.png" "$out" "$size" <<'PY'
import sys
from PIL import Image

src, dst, size = sys.argv[1], sys.argv[2], int(sys.argv[3])
Image.open(src).convert("RGBA").resize((size, size), Image.LANCZOS).save(dst)
PY
}

to_webp() {
    python3 - "$1" "$2" <<'PY'
import sys
from PIL import Image

Image.open(sys.argv[1]).convert("RGBA").save(
    sys.argv[2], "WEBP", quality=95, method=6
)
PY
}

legacy="mdpi:48 hdpi:72 xhdpi:96 xxhdpi:144 xxxhdpi:192"
adaptive="mdpi:108 hdpi:162 xhdpi:216 xxhdpi:324 xxxhdpi:432"

for entry in $legacy; do
    density="${entry%%:*}"
    size="${entry##*:}"
    # 18.5 units on a 108 unit canvas, expressed in this size
    font=$(python3 -c "print($size * 18.5 / 108)")
    render "$size" "$font" 1 "$work/legacy.png"
    to_webp "$work/legacy.png" "app/src/main/res/mipmap-$density/ic_launcher.webp"
    [ -n "$preview_dir" ] && cp "$work/legacy.png" "$preview_dir/text_ic_launcher-$density.png"
done

for entry in $adaptive; do
    density="${entry%%:*}"
    size="${entry##*:}"
    # The adaptive foreground must stay inside the 66/108 safe zone
    font=$(python3 -c "print($size * 13.5 / 108)")
    render "$size" "$font" 0 "$work/fg.png"
    to_webp "$work/fg.png" "app/src/main/res/mipmap-$density/ic_launcher_foreground.webp"
    [ -n "$preview_dir" ] && cp "$work/fg.png" "$preview_dir/text_ic_launcher_foreground-$density.png"
done

echo "done"
