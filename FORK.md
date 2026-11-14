# deeps-kaskad-client

Android client for [deeps kaskad](https://github.com/lange232/deeps-kaskad), forked
from [WG Tunnel](https://github.com/zaneschepke/wgtunnel) (MIT license,
copyright Zane Schepke — see `LICENSE`).

## What differs from upstream

- Rebranded: `applicationId` is `ru.deffhost.kaskad`, the app name is
  "deeps kaskad" (`buildSrc/.../Constants.kt`, `res/values/strings.xml`).
- "Sign in to panel" option in the tunnel-import sheet: server, email and
  password → `POST /api/login` → `GET /api/me/conf` on the kaskad panel, the
  returned awg0 config is imported through the regular config parser. Code:
  `data/network/panel/PanelApi.kt`,
  `ui/screens/tunnels/components/PanelLoginDialog.kt`,
  `SharedAppViewModel.provisionFromPanel`.
- Exit switching: with a saved panel session the tunnel settings gain an
  "Exit order" row that opens a full-screen page
  (`ui/screens/panel/PanelExitsScreen.kt`, `Route.PanelExits`). Exits are
  titled by their operator comment ("Amsterdam") with the provider config
  name underneath, up/down arrows reorder them, and the page has save and
  sign-out rows. It calls `GET /api/me/exits` and `POST /api/me/exits/order`;
  the order applies server-side immediately, the tunnel does not reconnect.
  The 30-day session token is kept in app-private SharedPreferences
  (`PanelSessionStore`); an expired session asks to sign in again.
- Two extra themes, "deeps kaskad" (dark `#161616`/`#242424`, accent
  `#fab283`) and "kaskad light" (accent `#d16427`), taken from the panel's
  `theme.css`. The automatic theme follows the system between these two.
- Iconography follows the upstream wg-tunnel language: a white "D" monogram
  made of thick rounded strokes on a graphite `#21272A` tile. Sources are
  `artwork/icon.svg` (tile + mark), `artwork/icon-foreground.svg` (adaptive
  foreground), `artwork/banner.svg` (Android TV banner) and
  `artwork/qs-logo.svg` (status bar/notification/QS). `scripts/render-icons.sh`
  regenerates every `mipmap-*` resource from them (cairosvg, rsvg-convert or
  headless chromium); `qs_logo.xml` is the vector twin of `qs-logo.svg` and is
  edited by hand. The adaptive background color is
  `values/ic_launcher_background.xml`, the banner color
  `values/ic_banner_background.xml`.
- Donation prompts and the Support bottom-nav tab are gone; licenses moved to
  Settings, and update checks look at this repository.
- `app/src/main/res/raw/panel.crt` — the panel's TLS certificate. The panel
  serves a self-signed certificate by default; drop its PEM here before
  building and `PanelApi` will trust only that certificate. With an empty
  file the system trust store is used (works when the panel is behind a real
  CA).

Get the panel certificate from the server:

```bash
cat /etc/kaskad/tls/panel.crt   # on S1, path may differ per deploy
```

## Building

```bash
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
scripts/build-release.sh
```

The APK lands in `app/build/outputs/apk/standalone/release/`; there is no
Play Store publishing.

## Releases

The main distribution channel is GitHub Releases of this repository:

1. Bump `versionName` and `versionCode` in `gradle/libs.versions.toml` and add
   `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.
2. Run the `release-apk` workflow (Actions → release-apk → Run workflow, tag
   can be left empty).
3. The release gets `deepskaskad-standalone-v<version>-arm64.apk`,
   `deepskaskad.apk` and `SHA256SUMS.txt`; the arm64 APK is the only split
   published. The fixed-name copy gives a stable link to the newest build:

   ```
   https://github.com/lange232/deeps-kaskad-client/releases/latest/download/deepskaskad.apk
   ```

   The versioned asset stays for pinning and for the panel's
   `scripts/fetch-apk.sh` (`*-arm64.apk` suffix). Never attach a second
   `-arm64.apk` asset to a release: the panel script fails on ambiguity.

Repository secrets used by CI: `KEYSTORE` (base64 of
`app/keystore/android_keystore.jks`),
`SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`, `SIGNING_STORE_PASSWORD`:

```bash
base64 -w0 app/keystore/android_keystore.jks | gh secret set KEYSTORE
```

The panel download stays as a mirror; `scripts/build-release.sh` builds the
same APK locally with the same key. Upstream `publish.yml` (fdroid, zapstore,
Play) has no tag trigger here, and the upstream nightly schedule is off.

Release builds are signed with `app/keystore/android_keystore.jks` (gitignored),
passwords live in `local.properties` (`SIGNING_STORE_PASSWORD`,
`SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`). The keystore is created once
with `keytool -genkeypair`; **back it up** — an APK signed with a new key
cannot update an installed app, users would have to reinstall. The debug
build uses the debug suffix `ru.deffhost.kaskad.debug`, so it installs next
to a release without conflicts, but not as an update for it.

## Updating from upstream

```bash
git fetch upstream
git merge upstream/master   # conflicts are expected mainly in resources
```

Keep the fork-specific surface small: Constants.kt, app_name strings,
`data/network/panel/`, `PanelLoginDialog.kt`, the extra sheet option in
`TunnelImportSheet.kt`, and `provisionFromPanel` in `SharedAppViewModel.kt`.
