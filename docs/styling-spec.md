# ChildTrace — Styling Specification

_Mobile design system for the Child and Parent Android apps. Authoritative: the Compose theme (`ui/theme/`) implements these tokens, and no screen hardcodes a value this document does not define._

The system is built on **Material 3** conventions so it reads as a clean, industry-standard Android app. Everything below is expressed as **tokens** — named values — so a change is made once, here and in the theme, never per screen.

---

## 1. Color

A **four-role** palette: a neutral ground, a brand accent, and a separate semantic-severity family. Severity colors are never reused as the accent, and the accent is never used to signal danger — keeping the two meanings from colliding.

### Light theme

| Token | Hex | Role |
| --- | --- | --- |
| `bg` | `#F4F6F6` | App background (behind everything) |
| `surface` | `#FFFFFF` | Cards, sheets, bars |
| `surfaceAlt` | `#EDF1F0` | Inset rows, muted fills |
| `ink` | `#11201D` | Primary text / high-emphasis |
| `inkSoft` | `#54635F` | Secondary text, captions |
| `inkFaint` | `#8A9793` | Tertiary / disabled text |
| `line` | `#DCE3E1` | Borders, dividers |
| `accent` | `#0E7A68` | Brand; primary actions, selected state |
| `onAccent` | `#FFFFFF` | Text/icons on an accent fill |
| `accentSoft` | `#D7ECE6` | Accent-tinted container |

### Dark theme

| Token | Hex | Role |
| --- | --- | --- |
| `bg` | `#0A1211` | App background |
| `surface` | `#13201D` | Cards, sheets, bars |
| `surfaceAlt` | `#0F1A18` | Inset rows, muted fills |
| `ink` | `#E9F1EF` | Primary text |
| `inkSoft` | `#9FB0AB` | Secondary text |
| `inkFaint` | `#6B7D78` | Tertiary / disabled |
| `line` | `#243330` | Borders, dividers |
| `accent` | `#3EC4AC` | Brand |
| `onAccent` | `#04201A` | Text/icons on accent fill |
| `accentSoft` | `#14332D` | Accent-tinted container |

### Semantic severity (both themes share the roles; values differ)

| Token | Light | Dark | Meaning |
| --- | --- | --- | --- |
| `critical` / `criticalBg` | `#C23526` / `#FAE7E4` | `#FF7063` / `#341915` | Critical danger |
| `high` / `highBg` | `#C9740A` / `#FBEED8` | `#F0A63F` / `#33240F` | High |
| `medium` / `mediumBg` | `#9A8405` / `#F5F0D0` | `#DDC74A` / `#2A2710` | Medium |
| `safe` / `safeBg` | `#0E7A68` / `#D7ECE6` | `#3EC4AC` / `#14332D` | Clear / OK |

---

## 2. Anti-collision rule (contrast)

The single rule that prevents "color collision" — unreadable foreground-on-background:

- **Every foreground token names the surface it sits on.** `ink`/`inkSoft` on `bg`/`surface`/`surfaceAlt`; `onAccent` on `accent`; a severity color only on its own `*Bg` container.
- **Contrast minimums (WCAG 2.1 AA):** body text ≥ **4.5:1**; large text (≥ 20sp bold / 24sp regular), icons, and UI borders ≥ **3:1**.
- `inkFaint` is for non-essential text only; it is never used for anything the user must read or act on.
- A severity hue is only ever drawn on its paired `*Bg` (e.g. `critical` on `criticalBg`), never on the raw app background, and never as filled text.
- **No raw literals in screens.** Components read tokens; CI/code-review rejects hardcoded hex. This is what guarantees the rule holds everywhere.

Both palettes above are validated at these ratios. If a token pair is ever edited, re-check it before merge.

---

## 3. Glow & elevation rule

Depth is expressed with **restraint**, the opposite of a neon/"glowing" look:

- Separation comes from **Material 3 tonal elevation** (surface tint) plus **one soft shadow level** for raised cards — not from colored glows.
- **Elevation scale:** `0dp` (flush background), `1dp` (card/list item), `3dp` (app bar, bottom nav), `8dp` (dialog, bottom sheet). Nothing above `8dp`.
- Shadow is neutral and subtle (`y-offset ≈ elevation`, low alpha). No multi-color or saturated shadows.
- **Glow is reserved for exactly one meaning:** a gentle pulsing ring on a *critical*-severity indicator (the "active & live" dot, a new critical alert). It uses `critical` at low alpha, animates per §8, and respects reduced-motion. Nothing else glows. This keeps a glow meaningful instead of decorative.

---

## 4. Typography

Three roles. Google Fonts; each with a real fallback stack.

| Role | Family | Fallback |
| --- | --- | --- |
| Display | **Fraunces** | Georgia, serif |
| Body / UI | **Public Sans** | system-ui, sans-serif |
| Data / mono | **IBM Plex Mono** | monospace |

### Type scale (sp — scales with the user's font setting)

| Token | Size / line | Weight | Use |
| --- | --- | --- | --- |
| `displayL` | 30 / 36 | 600 | Screen hero (rare) |
| `titleL` | 22 / 28 | 600 | Screen title |
| `titleM` | 18 / 24 | 600 | Section / card title |
| `body` | 16 / 24 | 400 | Default body |
| `bodyS` | 14 / 20 | 400 | Secondary |
| `label` | 13 / 16 | 500 | Buttons, chips, nav |
| `caption` | 12 / 16 | 400 | Captions, timestamps |
| `mono` | 13 / 18 | 400/500 | Codes, counts, aligned data |

Rules: body line-height ≈ 1.5; titles use `text-wrap: balance` equivalent (Compose `LineBreak.Heading`); uppercase labels get `+0.08em` tracking; never go below `caption` (12sp) for readable text.

---

## 5. Spacing & layout (4dp grid)

All spacing is a multiple of **4dp**, from one scale: `xs 4`, `sm 8`, `md 12`, `lg 16`, `xl 24`, `xxl 32`.

- **Screen gutter:** `lg` (16dp) on both sides at every width.
- **Gap between cards / list rows:** `md`–`lg`.
- **Inside a card:** `lg` padding.
- Group siblings with a container + `gap`; never per-element margins that can double or collapse.

### Radii & borders

`rSm 8dp` (chips, inputs), `rMd 12dp` (cards), `rLg 20dp` (sheets, hero). Borders are `1dp` `line`. One radius per role — consistency over variety.

---

## 6. Icons

| Token | dp | Use |
| --- | --- | --- |
| `iconSm` | 16 | Inline with text, chips |
| `iconMd` | 20 | Default (nav, list leading) |
| `iconLg` | 24 | Toolbar actions, emphasis |
| `iconXl` | 40 | Hero / empty-state |

- Default icon color is `inkSoft`; **active/selected** is `accent`; on an accent fill, `onAccent`; a severity icon takes its severity color **on its `*Bg`**, never bare.
- Stroke-style icons at ~2dp stroke for optical weight matching Public Sans.
- **Touch target ≥ 48×48dp** regardless of icon size (pad the hit area; the glyph stays its token size).

---

## 7. Responsive adaptation (auto-adjust)

The UI adapts to device size **and** to the user's font/display scaling — a hard requirement, not optional:

- **Text in `sp`, everything else in `dp`.** Text therefore honors the system font-scale (up to 200%); layouts never assume a fixed text height.
- **No fixed heights on text containers.** Rows and cards wrap to their content; a long message or a large font scale grows the row, never clips it. Every text node may wrap; none is single-line unless truncation with an ellipsis is explicitly intended (titles, never body).
- **Width breakpoints (Material window size classes):** compact `<600dp` (phone, single column), medium `600–840dp` (large phone / small tablet, may show a two-pane list/detail), expanded `>840dp` (tablet, two-pane). The console's feed/detail uses list-only in compact and list+detail in medium/expanded.
- **Min touch target 48dp; min readable body 16sp.** Content stays within the safe area (status bar, notch, nav bar / gesture inset).
- Images and media `fillMaxWidth` with intrinsic aspect; nothing has a `minWidth` wider than a compact screen.
- Test matrix: 360dp and 411dp widths × font scale 1.0 and 1.3 × light and dark — no overflow, no clipping, no overlap.

---

## 8. Motion & animation

Purposeful, quick, and reduced-motion aware. Durations and easing are tokens:

| Token | ms | Use |
| --- | --- | --- |
| `motionFast` | 120 | Pressed state, ripple, toggle |
| `motionBase` | 220 | Enter/exit, expand/collapse, nav |
| `motionSlow` | 400 | Shared-element / screen transition |
| `pulse` | 2400 (loop) | The one critical-alert glow |

- **Easing:** standard `FastOutSlowIn` for most; `LinearOutSlowIn` for enters, `FastOutLinearIn` for exits.
- **Where motion is used:** list items fade+slide in `motionBase` from a visible resting state (never left invisible); tab/content changes crossfade; the critical-alert ring pulses at `pulse`; buttons ripple at `motionFast`.
- **No gratuitous motion.** No parallax, no bouncing, no looping decoration other than the single critical pulse and indeterminate progress.
- **Respect `prefers-reduced-motion` / the system "remove animations" setting:** when set, replace transitions with instant state changes and stop the pulse (show a static ring). Never gate information behind an animation.

---

## 9. Enforcement

- The Compose theme (`ui/theme/Theme.kt`, `DesignTokens.kt`) is the single source of these values; screens consume `MaterialTheme.colorScheme` and the token objects.
- Code review rejects: raw hex/`Color(0x…)` in screens, `dp`/`sp` literals outside the token files, fixed heights on text, touch targets under 48dp, and any glow outside the critical-alert rule.
- When a token changes, update this document and the theme together; they must never drift.
