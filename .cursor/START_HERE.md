# START HERE — Fruit Splash (Nectar gray flow)

> Read this file first, then every file in `.cursor/rules/` before
> writing code.

Fruit Splash is a **native Android (Kotlin + Canvas game + Jetpack Compose
shell)** app. Gray-flow lives in the `nectar/` package. Do not copy
class names, folder layout or library pins from Olympus Surge (`oracle`),
Thunder Crest (`volt`) or the Flutter template.

References for *behaviour only*:

- Flutter contract: `D:\flutter_proj\gray_part_flow\`
- Kotlin ports: `D:\flutter_proj\Olympus_Surge\`, `D:\flutter_proj\Thunder Crest\`, `D:\flutter_proj\golden rittler\`

---

## 1. Dual mode

- **Gray** — `NectarWebActivity` WebView, URL from `https://fruitsplassh.com/config.php`.
- **White** — `SplashActivity` → `MainActivity` garden game. Must launch **offline**.

Decision is made once per install by the backend from AppsFlyer data.

### Map

| File | Role |
|---|---|
| `nectar/NectarApp.kt` | `Application`. Firebase + AppsFlyer `wire()`. |
| `nectar/NectarLaunchActivity.kt` | Launcher / state machine. |
| `nectar/NectarWebActivity.kt` | WebView shell. |
| `nectar/NectarInviteActivity.kt` | Accept / Skip push opt-in. |
| `nectar/NectarOfflineActivity.kt` | No-connection + Retry. |
| `nectar/net/NectarAf.kt` | AppsFlyer `wire` / `kick` / `retrace`. |
| `nectar/net/NectarPost.kt` | Config POST. |
| `nectar/net/NectarReach.kt` | Adapter + TCP probe. |
| `nectar/box/NectarBox.kt` | Prefs + encrypted URLs. |
| `nectar/box/NectarPush.kt` | FCM channel, `extractUrl`, service. |
| `nectar/box/NectarWarm.kt` | Warm push hand-off. |
| `nectar/mix/NectarXor.kt` | DJB2+MWC codec. Seed `kP9#wL2mQx7!` / stream 23. |

---

## 2. Brief (filled)

| Field | Value |
|---|---|
| Package | `com.fruitsplash.fruitsplashgame` |
| Config | `https://fruitsplassh.com/config.php` |
| Privacy | `https://fruitsplassh.com/privacy-policy.html` |
| Support | `https://fruitsplassh.com/support.html` |
| Site | `https://fruitsplassh.com` |
| AppsFlyer / Firebase | packed (XOR in `NectarBytes.kt`) |
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
9. AppsFlyer `wire` in `NectarApp`, `kick` only after connectivity.
10. Push URL extract reads custom extras **and** raw `url`/`link`/`target_url`.
11. Warm push → `NectarWarm` (never persist); cold → one-shot stash.

---

## 4. Fingerprint (already unique for Fruit Splash)

- Package theme: `nectar` (not `oracle` / `volt` / `gray`)
- Codec: DJB2+MWC, seed `kP9#wL2mQx7!`, stream 23
- Prefs: `pulp_plain_jar` / `pulp_sealed_jar`
- Channel: `nectar_garden_notes`
- Drawables: `nectar_*` / `ic_nectar_flame`
- Library minors differ from Thunder Crest (see `gradle/libs.versions.toml`)
- Buttons: lime→orange juice pills (not gold/ember, not sky-blue)
