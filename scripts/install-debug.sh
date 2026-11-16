#!/usr/bin/env bash
# Builds the debug APK and installs it on the phone over adb.
#
# Usage:
#   scripts/install-debug.sh                # build + install + launch
#   scripts/install-debug.sh --no-build     # install the APK already in build/outputs
#   scripts/install-debug.sh --no-launch    # install without starting the app
#   scripts/install-debug.sh -s R58M123ABC  # pick a device when several are connected
#
# The debug build installs next to the release app: its package is
# ru.deffhost.kaskad.debug, so it does not touch the release installation or its data.
set -euo pipefail
cd "$(dirname "$0")/.."

export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
ADB="$ANDROID_HOME/platform-tools/adb"
APK_DIR="app/build/outputs/apk/standalone/debug"
PACKAGE="ru.deffhost.kaskad.debug"
ACTIVITY="$PACKAGE/com.zaneschepke.wireguardautotunnel.MainActivity"

do_build=1
do_launch=1
serial=""

while [ $# -gt 0 ]; do
    case "$1" in
        --no-build) do_build=0; shift ;;
        --no-launch) do_launch=0; shift ;;
        -s|--serial) serial="${2:?--serial needs a device id}"; shift 2 ;;
        -h|--help)
            awk 'NR>1 && /^#/ {sub(/^# ?/, ""); print; next} NR>1 {exit}' "$0"
            exit 0
            ;;
        *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
done

if [ ! -x "$ADB" ]; then
    echo "adb not found at $ADB — set ANDROID_HOME to your Android SDK" >&2
    exit 1
fi

adb() {
    if [ -n "$serial" ]; then "$ADB" -s "$serial" "$@"; else "$ADB" "$@"; fi
}

if [ -z "$serial" ]; then
    mapfile -t ready < <("$ADB" devices | awk 'NR>1 && $2=="device" {print $1}')
    mapfile -t unauthorized < <("$ADB" devices | awk 'NR>1 && $2=="unauthorized" {print $1}')
    if [ "${#ready[@]}" -eq 0 ]; then
        if [ "${#unauthorized[@]}" -gt 0 ]; then
            echo "Device ${unauthorized[0]} is not authorized: accept the USB debugging prompt on the phone" >&2
        else
            echo "No device over adb. Connect the phone with USB debugging enabled." >&2
        fi
        exit 1
    fi
    if [ "${#ready[@]}" -gt 1 ]; then
        echo "Several devices connected, pick one with -s:" >&2
        printf '  %s\n' "${ready[@]}" >&2
        exit 1
    fi
    serial="${ready[0]}"
    echo "device: $serial ($(adb shell getprop ro.product.model | tr -d '\r'))"
fi

if [ "$do_build" -eq 1 ]; then
    echo "building $PACKAGE"
    ./gradlew :app:assembleStandaloneDebug
fi

abi="$(adb shell getprop ro.product.cpu.abi | tr -d '\r')"

# The version is in the file name, so pick the newest matching APK instead of guessing it.
pick_apk() {
    local pattern="$1"
    ls -t $APK_DIR/*"$pattern".apk 2>/dev/null | head -1
}

case "$abi" in
    arm64-*) apk="$(pick_apk '-arm64')" ;;
    armeabi-*) apk="$(pick_apk '-armv7')" ;;
    *) apk="" ;;
esac
# Fall back to the universal APK when the split for this ABI is missing.
if [ -z "$apk" ]; then
    apk="$(pick_apk '')"
fi
if [ -z "$apk" ] || [ ! -f "$apk" ]; then
    echo "No debug APK in $APK_DIR — run without --no-build" >&2
    exit 1
fi

echo "installing $(basename "$apk") on $abi"
adb install -r "$apk" | tail -1

if [ "$do_launch" -eq 1 ]; then
    adb shell am force-stop "$PACKAGE" >/dev/null 2>&1 || true
    adb shell am start -n "$ACTIVITY" >/dev/null
    echo "started $PACKAGE"
fi

installed="$(adb shell dumpsys package "$PACKAGE" | awk -F= '/versionName=/ {print $2; exit}' | tr -d '\r')"
echo "installed version: $installed"
