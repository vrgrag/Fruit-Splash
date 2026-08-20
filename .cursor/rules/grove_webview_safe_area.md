# WebView safe area

Native padding (all four sides) = max(cutout, systemBars). IME excluded.
Use `getInsetsIgnoringVisibility` — hidden status/nav bars must not collapse padding to 0.

CSS injection on `onPageFinished` + delayed re-apply (zeros **site** CSS so it does not double the native gutter):

- Zero `--safe-area-inset-*`, `--sat/sar/sab/sal`, `--safe-top/bottom/left/right`
- Zero `padding-top` only on `.gameview-mobile-header`, `.app-header`, `.js-safe-top`
- `viewport-fit=contain`
- Skip while keyboard open
- Re-apply on pushState / replaceState / popstate / interval 2500 ms

Do **not** zero padding/margin on `html, body, #app, #root`.

Invite and Offline screens do **not** use this inset rule.
