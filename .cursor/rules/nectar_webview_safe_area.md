# WebView safe area

Native padding (all four sides) = max(cutout, systemBars). IME excluded.

CSS injection on `onPageFinished` + delayed re-apply:

- Zero `--safe-area-inset-*`, `--sat/sar/sab/sal`, `--safe-top/bottom/left/right`
- Zero `padding-top` only on `.gameview-mobile-header`, `.app-header`, `.js-safe-top`
- `viewport-fit=contain`
- Skip while keyboard open
- Re-apply on pushState / replaceState / popstate / interval 2500 ms

Do **not** zero padding/margin on `html, body, #app, #root`.

Invite and Offline screens do **not** use this inset rule.
