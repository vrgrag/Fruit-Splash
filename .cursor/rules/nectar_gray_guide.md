# Nectar gray guide — Fruit Splash

Native Kotlin port of the gray-part flow. Names, folders and library
pins are unique to this app. Behaviour matches
`gray_part_flow/.cursor/rules/android_gray_guide.md`.

## Config POST

- URL: XOR-decoded `NectarBrand.configEndpoint`
- Method POST, JSON, 15 s timeout, forged Chrome UA
- Body: AppsFlyer conversion map (verbatim) + UDL gap-fill + device fields last:
  `af_id`, `bundle_id`, `os`=`Android`, `store_id`, `locale` (RFC 3066 with `_`)
  `push_token` + `firebase_project_id` **both or neither**
- Success: `{ ok: true, url, expires }`
- Failure: `{ ok: false }` or non-2xx → answered=true, allowed=false
- Timeout / IO → answered=false (do not persist native)

## Screens

Loading / Invite / Offline artwork in `res/drawable-nodpi/nectar_*.webp`.
Buttons via Compose + `NectarArtMetrics`. No safe-area on Invite/Offline.
Status + nav bars hidden (`NectarFill` after `setContentView`).

## Notification icon

`res/drawable/ic_nectar_flame.xml` — 24 dp flame, white, evenOdd.
Not the fruit launcher icon.

## Setup after AF / Firebase arrive

1. Paste key + project number into `tools/pack_secrets.py`
2. Keep `NectarXor` seed (`kP9#wL2mQx7!` / 23)
3. Run packer → paste into `NectarBytes.kt`
4. Drop `google-services.json` into `app/`
5. OneLink host `fruitsplash.onelink.me`, path `/DqS8`
5. Set real OneLink host in `AndroidManifest.xml`
