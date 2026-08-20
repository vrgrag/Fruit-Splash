# START HERE — Fruit Splash (Grove gray flow)

> Read this file first, then every file in `.cursor/rules/` before
> writing code.

Fruit Splash is a **native Android (Kotlin + Canvas game + Jetpack Compose
shell)** app. Gray-flow lives in the `grove/` package. Do not copy
class names, folder layout or library pins from Olympus Surge (`oracle`),
Thunder Crest (`volt`) or the Flutter template.

References for *behaviour only*:

- Flutter contract: `D:\flutter_proj\gray_part_flow\`
- Kotlin ports: `D:\flutter_proj\Olympus_Surge\`, `D:\flutter_proj\Thunder Crest\`, `D:\flutter_proj\golden rittler\`

---

## 1. Dual mode

- **Gray** — `CanopyShell` WebView, URL from `https://fruitsplassh.com/config.php`.
- **White** — `SplashActivity` → `MainActivity` garden game. Must launch **offline**.

Decision is made once per install by the backend from AppsFlyer data.

### Map

| File | Role |
|---|---|
| `grove/GroveApp.kt` | `Application`. Firebase + AppsFlyer `wire()`. |
| `grove/HarvestGate.kt` | Launcher / state machine. |
| `grove/CanopyShell.kt` | WebView shell. |
| `grove/TrellisAsk.kt` | Accept / Skip push opt-in. |
| `grove/QuietCanopy.kt` | No-connection + Retry. |
| `grove/vine/VineAf.kt` | AppsFlyer `wire` / `kick` / `retrace`. |
| `grove/vine/VinePost.kt` | Config POST. |
| `grove/vine/VineReach.kt` | Adapter + TCP probe. |
| `grove/crate/PulpVault.kt` | Prefs + encrypted URLs. |
| `grove/crate/GroveNote.kt` | FCM channel, `extractUrl`, service. |
| `grove/crate/WarmPip.kt` | Warm push hand-off. |
| `grove/rind/GroveXor.kt` | DJB2+MWC codec. Seed `kP9#wL2mQx7!` / stream 23. |

---

## 2. Brief (filled)

| Field | Value |
|---|---|
| Package | `com.fruitsplash.fruitsplashgame` |
| Config | `https://fruitsplassh.com/config.php` |
| Privacy | `https://fruitsplassh.com/privacy-policy.html` |
| Support | `https://fruitsplassh.com/support.html` |
| Site | `https://fruitsplassh.com` |
| AppsFlyer / Firebase | packed (XOR in `GroveBytes.kt`) |
| OneLink | `https://fruitsplash.onelink.me/DqS8/w7m9rf78` |
| Theme | crash / casual — **no** `appid/` `appname/` UA suffix |

Debug without AF: `pulp.properties`

```
probeLink=https://example.com
stickyVerdict=false
```

---

## 3. Invariants

1. Pending + offline → NoWifi on **frame one**; mode stays pending.
2. Native persisted only when endpoint **answered** AND attribution map was non-empty.
3. Once native, stay native — push never opens WebView.
4. Web install never drops to the garden because one config call failed.
5. Loading bar fills to 1.0 at the hand-over frame.
6. Invite / Offline: **no** systemBars padding; buttons horizontally centered under the plaque.
7. Status bar and nav-button bar are hidden on gray screens (same as the garden).
8. WebView respects cutout; white garden launches offline via `SplashActivity`.
9. AppsFlyer `wire` in `GroveApp`, `kick` only after connectivity.
10. Push URL extract reads custom extras **and** raw `url`/`link`/`target_url`.
11. Warm push → `WarmPip` (never persist); cold → one-shot stash.

---

## 4. Fingerprint (already unique for Fruit Splash)

- Package theme: `grove` / `crate` / `vine` / `trellis` (not `nectar` / `oracle` / `volt`)
- Codec: DJB2+MWC, seed `kP9#wL2mQx7!`, stream 23
- Prefs: `pulp_plain_jar` / `pulp_sealed_jar` (do not rotate on an already-shipped install)
- Channel: `ch_jx4grove`
- JS sentinels: `__jx4sa` / `__jx4kb`
- Intent extras: `jx4_href` / `jx4_ask` / `jx4_back` / `jx4_note` / `jx4_tap`
- Drawables: `grove_*` / `ic_grove_ember`
- R8: minify helpers; `-repackageclasses jx`; do not `-keep grove.**`
- Library minors differ from Thunder Crest (see `gradle/libs.versions.toml`)
- Buttons: lime→orange juice pills (not gold/ember, not sky-blue)
