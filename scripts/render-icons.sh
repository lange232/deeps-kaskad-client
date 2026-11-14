#!/usr/bin/env bash
# Renders artwork/*.svg into the launcher, banner and icon resources.
#
# Usage: scripts/render-icons.sh [--preview DIR]
#
# Renderer: cairosvg, rsvg-convert or headless chromium, whichever is found.
# Writes app/src/main/res/mipmap-*/ic_launcher.webp,
# ic_launcher_foreground.webp and ic_banner.png. The notification/QS icon
# (drawable/qs_logo.xml) is a vector and mirrors artwork/qs-logo.svg by hand.
set -euo pipefail
cd "$(dirname "$0")/.."

preview_dir=""
if [ "${1:-}" = "--preview" ]; then
    preview_dir="${2:?preview directory required}"
    mkdir -p "$preview_dir"
fi

mkdir -p build
# Headless chromium runs in a snap on some machines and cannot see /tmp.
work=$(mktemp -d "${PWD}/build/icon-render.XXXXXX")
trap 'rm -rf "$work"' EXIT

renderer=""
for candidate in cairosvg rsvg-convert chromium chromium-browser google-chrome; do
    if command -v "$candidate" >/dev/null 2>&1; then
        renderer="$candidate"
        break
    fi
done
if [ -z "$renderer" ]; then
    echo "No SVG renderer found: install cairosvg (python3 -m pip install cairosvg) or librsvg" >&2
    exit 1
fi
echo "renderer: $renderer"

render_png() {
    local svg="$1" width="$2" height="$3" out="$4"
    case "$renderer" in
        cairosvg) cairosvg "$svg" -o "$out" -W "$width" -H "$height" ;;
        rsvg-convert) rsvg-convert -w "$width" -h "$height" -o "$out" "$svg" ;;
        *)
            # A headless browser renders the SVG at its intrinsic size, the
            # default body margin would crop it, and small windows come out
            # blank on some setups. So render at 4x with no margin and let
            # Pillow downscale afterwards.
            local scale=4
            local rw=$((width * scale)) rh=$((height * scale))
            sed -e "s/ width=\"[0-9]*\"/ width=\"$rw\"/" \
                -e "s/ height=\"[0-9]*\"/ height=\"$rh\"/" \
                "$svg" > "$work/page.svg"
            cat > "$work/page.html" <<HTML
<!doctype html>
<html><head><style>html,body{margin:0;padding:0;overflow:hidden;background:transparent}
img{display:block;width:${rw}px;height:${rh}px}</style></head>
<body><img src="page.svg"></body></html>
HTML
            "$renderer" --headless --no-sandbox --disable-gpu --hide-scrollbars \
                --force-device-scale-factor=1 --default-background-color=00000000 \
                --window-size="$rw,$rh" --screenshot="$work/page.png" \
                "file://$work/page.html" >/dev/null 2>&1
            python3 - "$work/page.png" "$out" "$width" "$height" <<'PY'
import sys
from PIL import Image

src, dst, width, height = sys.argv[1], sys.argv[2], int(sys.argv[3]), int(sys.argv[4])
Image.open(src).convert("RGBA").resize((width, height), Image.LANCZOS).save(dst)
PY
            ;;
    esac
}

to_webp() {
    local png="$1" out="$2"
    python3 - "$png" "$out" <<'PY'
import sys
from PIL import Image

src, dst = sys.argv[1], sys.argv[2]
Image.open(src).convert("RGBA").save(dst, "WEBP", quality=95, method=6)
PY
}

legacy="mdpi:48 hdpi:72 xhdpi:96 xxhdpi:144 xxxhdpi:192"
adaptive="mdpi:108 hdpi:162 xhdpi:216 xxhdpi:324 xxxhdpi:432"
banner="mdpi:160x90 hdpi:240x135 xhdpi:320x180 xxhdpi:480x270 xxxhdpi:640x360"

for entry in $legacy; do
    density="${entry%%:*}"
    size="${entry##*:}"
    render_png artwork/icon.svg "$size" "$size" "$work/legacy.png"
    to_webp "$work/legacy.png" "app/src/main/res/mipmap-$density/ic_launcher.webp"
    if [ -n "$preview_dir" ]; then cp "$work/legacy.png" "$preview_dir/ic_launcher-$density.png"; fi
done

for entry in $adaptive; do
    density="${entry%%:*}"
    size="${entry##*:}"
    render_png artwork/icon-foreground.svg "$size" "$size" "$work/fg.png"
    to_webp "$work/fg.png" "app/src/main/res/mipmap-$density/ic_launcher_foreground.webp"
    if [ -n "$preview_dir" ]; then cp "$work/fg.png" "$preview_dir/ic_launcher_foreground-$density.png"; fi
done

for entry in $banner; do
    density="${entry%%:*}"
    dims="${entry##*:}"
    render_png artwork/banner.svg "${dims%x*}" "${dims#*x}" "$work/banner.png"
    python3 - "$work/banner.png" "app/src/main/res/mipmap-$density/ic_banner.png" <<'PY'
import sys
from PIL import Image

src, dst = sys.argv[1], sys.argv[2]
# The banner is opaque: the tile colour covers the whole canvas.
Image.open(src).convert("RGB").save(dst, "PNG", optimize=True)
PY
    if [ -n "$preview_dir" ]; then cp "app/src/main/res/mipmap-$density/ic_banner.png" "$preview_dir/ic_banner-$density.png"; fi
done

if [ -n "$preview_dir" ]; then
    render_png artwork/qs-logo.svg 96 96 "$preview_dir/qs-logo.png"
fi

echo "done"
