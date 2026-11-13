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
- Launcher icons are the panel favicon (dark `#141413` tile, orange `#fab283`
  D), regenerated from the SVG with a small cairosvg script; the adaptive
  background color is `values/ic_launcher_background.xml`. The quick settings
  tiles and the notification icon use the same D (`res/drawable/qs_logo.xml`).
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

The APK lands in `app/build/outputs/apk/standalone/release/` and is uploaded
to the download site manually; there is no Play Store publishing.

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
