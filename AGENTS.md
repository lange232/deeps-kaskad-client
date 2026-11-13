# AGENTS.md

Android client for **deeps kaskad**, a fork of
[WG Tunnel](https://github.com/zaneschepke/wgtunnel) (MIT). The server side
lives in a separate repository (`../deeps-kaskad`) and a separate session.
Fork-specific details that do not belong here are in `FORK.md`.

This file is committed: it contains no secrets. Keystore and signing
passwords stay in gitignored files.

## Hard rules

- `namespace` stays `com.zaneschepke.wireguardautotunnel`; only
  `applicationId` differs (`ru.deffhost.kaskad`, `buildSrc/src/main/kotlin/Constants.kt`).
  Renaming the namespace would break every `R` and `BuildConfig` import.
- Never change `applicationId` or the signing key. The app is already
  installed on users' phones: a new key or a new id means a fresh install,
  losing tunnels and the panel session.
- `app/keystore/android_keystore.jks`, `local.properties` and `*.apk` are
  gitignored. Do not commit them, do not move the keystore.
- Do not add Play Store publishing. The APK is distributed from the panel.
- `qs_logo.xml` is our "D" mark (tiles and notifications), the launcher
  icons are the panel favicon. Do not restore the upstream artwork.
- Donation UI and the Support bottom-nav tab were removed on purpose. The
  Support screen itself stays because the update flow lives there.
- Update checks point at `lange232/deeps-kaskad-client`
  (`di/NetworkModule.kt`), never at the upstream repository.

## Building

```bash
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew :app:compileStandaloneDebugKotlin   # fast compile check
scripts/build-release.sh                      # signed release, all ABIs
```

There are no unit tests in this project; the compile check plus installing
the APK is what we have. The arm64 APK is
`app/build/outputs/apk/standalone/release/deepskaskad-standalone-v<version>-arm64.apk`.

`local.properties` must hold `sdk.dir`, `SIGNING_STORE_PASSWORD`,
`SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`; `scripts/build-release.sh`
reads them and exports `KEY_STORE_PATH` itself.

## Release and deploy

1. Bump `versionName` in `gradle/libs.versions.toml` when the change is
   worth a new number; the APK file name follows it.
2. `scripts/build-release.sh`.
3. Copy the arm64 APK to the server repo:
   `cp app/build/outputs/apk/standalone/release/deepskaskad-standalone-v<version>-arm64.apk ../deeps-kaskad/files/app/app.apk`
4. In `../deeps-kaskad`:
   `ansible-playbook -i inventory.yml playbooks/site.yml --tags panel`.
   The panel copies the APK to `/var/lib/kaskad/app.apk` and serves it at
   `https://omega.deffhost.ru:8080/download/android`.
5. Verify: `curl -s .../download/android -o /dev/null -w '%{size_download}\n'`
   matches the local file size.

Never run the deploy or the Ansible playbook without the user asking for it
explicitly. Production host, stands and their state are described in the
server repository, not here.

## Server contract

The panel API is the only way the client talks to the server (bearer token,
30-day TTL, no cookies):

| Endpoint | Purpose |
|---|---|
| `POST /api/login` | email + password → token, client name |
| `GET /api/me` | account and client info |
| `GET /api/me/conf` | ready awg0 client config |
| `GET /api/me/exits` | granted exits: `id`, `name`, `comment`, `alive`, `rtt_ms` |
| `POST /api/me/exits/order` | repeated `exit_id` form fields, every granted exit once |
| `POST /api/logout` | revoke the token |

- `name` is the provider config name (`tucana`), `comment` is the human
  label (`Amsterdam`). UI titles use `comment ifBlank name`, like the panel.
- The token is kept in `PanelSessionStore` (app-private SharedPreferences);
  an expired session must ask to sign in again, not silently drop the config.
- `res/raw/panel.crt` pins the panel certificate. Production uses a Let's
  Encrypt certificate, so the file is currently empty and the system trust
  store is used. A self-signed stand needs its PEM dropped in before building.
- Changing the contract is a server-repository change first, then a client
  one. Do not invent fields on the client side.

## UI conventions

- One theme entry point: `WireguardAutoTunnelTheme` and the palette in
  `ui/theme/Color.kt`. Brand themes are **deeps kaskad** (dark) and
  **kaskad light**; `AUTOMATIC` follows the system between those two. Add
  colors as tokens there, not as literals in screens.
- Build screens from the shared pieces: `SurfaceRow`, `GroupLabel`,
  `DescriptionText`, `SwitchWithDivider`. New settings rows belong in a
  `Column` with a `GroupLabel`, full-screen routes get a title bar entry in
  `currentBackStackEntryAsNavbarState.kt` and a `Route` value.
- Every user-visible string goes to `res/values/strings.xml` and
  `res/values-ru/strings.xml`. Brand names ("deeps kaskad", "kaskad light")
  are not translated.
- Russian is only UI localization; code, comments and docs stay English.

## Upstream

```bash
git fetch upstream && git merge upstream/master
```

Conflicts usually land in `strings.xml`, `ui/theme/Theme.kt`,
`ui/navigation/Route.kt`, `MainActivity.kt`, `di/NetworkModule.kt` and the
notification/tile resources. After a merge run the compile check and grep for
reappeared upstream branding (`wgtunnel`, `WG Tunnel`, donation strings,
repo `zaneschepke/wgtunnel`).
