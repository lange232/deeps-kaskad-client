#!/usr/bin/env bash
# Builds the signed standalone release APK.
#
# The upstream signing config resolves the keystore path relative to the
# build, so we export absolute paths and the passwords from the gitignored
# local.properties ourselves instead of relying on buildSrc's lookup.
set -euo pipefail
cd "$(dirname "$0")/.."

export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export KEY_STORE_PATH="$PWD/app/keystore/android_keystore.jks"
export SIGNING_STORE_PASSWORD="$(grep '^SIGNING_STORE_PASSWORD=' local.properties | cut -d= -f2-)"
export SIGNING_KEY_ALIAS="$(grep '^SIGNING_KEY_ALIAS=' local.properties | cut -d= -f2-)"
export SIGNING_KEY_PASSWORD="$(grep '^SIGNING_KEY_PASSWORD=' local.properties | cut -d= -f2-)"

./gradlew :app:assembleStandaloneRelease "$@"
ls -la app/build/outputs/apk/standalone/release/*.apk
