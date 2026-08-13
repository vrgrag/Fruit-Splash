# Nectar pitfalls (must apply)

1. First-frame offline uses `hasRadio()`, not a TCP probe.
2. VPN: heartbeat TCP to `1.1.1.1:443` / `8.8.8.8:53`. Adapter check lies.
3. Offline debounce 850 ms; online is instant.
4. `pending` + timeout must **not** persist native.
5. Web returning user + failed POST → cached URL, never garden.
6. Invite / Offline: no `systemBars` padding. Buttons `TopCenter` + ArtMetrics.
7. WebView padding: `getInsetsIgnoringVisibility(statusBars|displayCutout)` top, `navigationBars` bottom. Hidden bars must not collapse padding to 0. No IME.
8. Keyboard: `adjustPan`. JS `scrollIntoView({behavior:'auto', block:'center'})` once at 350 ms.
9. Safe-area CSS zeros variables only. Never reset `html,body,#app` padding.
10. JS sentinels `__fsNectarSa` / `__fsNectarKb` — unique per project.
11. File chooser: `FileChooserParams.createIntent()`, no storage permission.
12. Back: `retreatTowardLanding()` — jump to first same-domain page, skip tracker hops. First page is a no-op, never `finish()`.
13. Redirect loop: `retryPending` + cover stays up. Resume from `deepestHop`. Soft-cap hops at 16. Never show `ERR_TOO_MANY_REDIRECTS`. Pin `landingUrl` when the cover drops — do **not** `clearHistory()`.
14. Cover during redirects is a black panel, never splash artwork and never `web.draw()`. Do not drop it from `onProgressChanged` during the entry chain or while `loadFailed`/`retryPending`.
15. Cleartext hops upgraded to https via `NectarUrl.clean`.
16. `CHANNEL_ID` = `nectar_garden_notes` matches manifest meta-data.
17. Notification icon is a **flame** vector, not the fruit launcher.
18. No `setRequestedOrientation` on edge-to-edge activities (API 27+ crash).
19. `applicationIdSuffix` is forbidden.
20. Empty `push_token` / `firebase_project_id` must be omitted, never `""`.
21. White path: `SplashActivity` (asset warmup) then `MainActivity`. Garden has no network calls.
22. 16 KB pages: NDK 27.2, AGP 8.13.
23. Hide status + nav bars on Launch / Invite / Offline / WebView (`NectarFill`, after `setContentView`).
24. FCM `onNewToken` must immediately re-POST config (token is often missing on an offline-first install). Offline Retry prefetches the token (3 s) before the router.
25. Skip cooldown is 3 **calendar** days from local midnight. Re-offer on WebView `onStart` after the date rolls, not only via `NectarLaunchActivity`.
