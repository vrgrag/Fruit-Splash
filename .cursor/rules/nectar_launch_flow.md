# Nectar launch flow

Read before editing `NectarLaunchActivity`, `NectarAf`, `NectarPush`.

## AppsFlyer

- `wire()` in `NectarApp.onCreate` — `init` + listeners, **no** `start`, **no** network.
- `kick(activity)` only after `NectarReach.hasRadio()` / `canTalk()`.
- `retrace(activity)` if the first conversion map was empty and the radio is now up.
- **No** GCD v4.0 HTTP re-check.

## State

### pending
- No adapter → `NectarOfflineActivity` on frame one. Nothing persisted.
- Adapter → kick AF → POST config.
  - `ok` + url → persist web → invite? → WebView.
  - timeout / IO (`answered=false`) → garden **this launch**, mode stays pending.
  - HTTP answer, no url, attribution present → persist native.

### web
- Cold push stash wins.
- Always re-POST config. Cache is fallback. Never open the garden.

### native
- `SplashActivity` immediately. Zero network. Push shows text only.

## Push

`extractUrl` reads `nectar_dest` **and** raw `url` / `link` / `target_url`.
Firebase-drawn notification blocks never run `NectarPushService`.

Skip → hide until local midnight **3 calendar days** later (not a 72-hour
timer — setting the date +3 days must bring the screen back).
Re-check on WebView `onStart`, not only in the launcher.
OS deny → never show invite again.
Both Accept and Skip are real gradient pills.

Token arrival (`onNewToken` or Accept) immediately re-POSTs config.
Do not poll `getToken()` in a loop. Omit empty `push_token`.
