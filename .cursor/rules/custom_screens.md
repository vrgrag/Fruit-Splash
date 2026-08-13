# Custom screens

| Screen | Source | Drawable |
|---|---|---|
| Loading portrait | `assets/Vertical_Loading_Screen.webp` | `nectar_splash_port.webp` |
| Loading landscape | `assets/Horizontal_Loading_Screen.webp` | `nectar_splash_land.webp` |
| Notifications portrait | `assets/Vertical_Notifications_Screen.webp` | `nectar_invite_port.webp` |
| Notifications landscape | `assets/Horizontal_Notifications_Screen.webp` | `nectar_invite_land.webp` |
| NoWifi portrait | `assets/Vertical_Nowifi_Screen.webp` | `nectar_quiet_port.webp` |
| NoWifi landscape | `assets/Horizontal_Nowifi_Screen.webp` | `nectar_quiet_land.webp` |

Art size is 1080×2400 (portrait) and 2400×1080 (landscape).
Source copies live under project `assets/`.

Buttons are **not** baked into the artwork. They are Compose pills
pinned under the wooden plaque via `NectarArtMetrics` (Crop math).

Invite / Offline: **no** safe-area padding. Horizontal centre only.

Accept / Skip / Retry are real lime→orange (or orange→pink Skip)
rounded pills. Skip is never a text link.

Re-measure `NectarArtMetrics` card `bottom` fractions if artwork changes.
