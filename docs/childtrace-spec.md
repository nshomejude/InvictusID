# ChildTrace — Technical Design & Product Specification

_As of Oct 11, 2026 · Jude Nshome, Opesware Technologies Sarl_

> Living version of this spec (with the architecture and roadmap diagrams rendered):
> https://claude.ai/code/artifact/bf9b8b34-0d28-4020-86ee-65359892a734

> ## Revision 2 — product direction (supersedes the detail below where they conflict)
>
> Two decisions change the model described in the rest of this document. The invariants in [`CLAUDE.md`](../CLAUDE.md) are authoritative.
>
> 1. **Comprehensive monitoring, not flag-only.** A vetted parent may view their own minor child's device activity (messages across apps, browsing, installed apps, location, screen content), not only flagged items — **provided the Child app remains always visible and discloses this plainly to the child.** The on-device risk engine still ranks danger to the top so the parent is never left to scroll. Wherever the text below says "only flagged items leave the device" or "never a transcript", read it as superseded by this, with the always-visible rule intact.
> 2. **Scope is under-18, sold by application.** Profiles are provisioned for the buyer's own children **under 18** (18+ refused), not only 5–15. Sale is gated by a reviewed parent **application** (buyer 40+, documentary evidence, proof of own child) rather than an in-person counter; there is still no self-serve payment.
>
> **What does not change:** always-visible / never-covert (the firewall), own-minor-child-only, encrypted + retention-bounded data, no resale. **New hard gate:** because monitoring is now comprehensive and reaches under-18s, Cameroon counsel must sign off on lawful basis, proportionality, retention, and the child's own data-protection rights before launch — and the **16–17 band stays refused in code** until that sign-off and a defined policy band exist.

## Overview

ChildTrace is an Android child-safety application for parents of their own children under 18. It gives a vetted parent visible, disclosed monitoring of their child's device and timely warning when the child encounters real online danger — grooming, sexual content, bullying, self-harm pressure, a stranger escalating contact — with an on-device risk engine that ranks that danger to the top of what the parent sees. (This Overview and the sections below describe the original flag-only, 5–15 model; see **Revision 2** above for the current direction, which supersedes them where they conflict.)

The product is two native Kotlin apps backed by an Opesware cloud service:

- **Child app** — installed openly on the child's phone. It captures message and content signals on that device, runs them through an on-device risk engine, and sends only flagged items to the backend.
- **Parent app** — the parent's phone. It shows a severity-ranked feed of flagged alerts plus the control surface (app limits, screen-time schedules, location, install approval).
- **Backend** — pairs the two devices, carries flagged alerts, and stores the signed-consent record that makes each deployment legally accountable.

Two decisions define everything below. First, the Child app is **always visible on the child's device** — no hidden icon, no stealth. That single line is what separates ChildTrace from stalkerware, and it holds under Revision 2 without exception. Second, ChildTrace is **consent-gated and never self-serve**: provisioning is gated by a reviewed parent application in which the buyer proves they are the child's own parent, with no in-app purchase or online payment. (Revision 2 replaces the original "in-person counter" sale with an application-based one; the proof-of-own-child gate is unchanged.)

## Ethical and legal foundation

The capabilities ChildTrace needs are the same ones stalkerware uses. What makes ChildTrace defensible is not a weaker feature set — it is consent and visibility, enforced at both the sales counter and in the software.

**The design line: the app is always visible.** The Child app appears in the launcher and app list, states what it monitors, and cannot be hidden. There is no covert mode and no plan to add one. A hidden-icon feature would reclassify the product as stalkerware, trigger Play Store removal and antivirus detection, and expose Opesware to liability — so it is excluded by design, not by configuration.

**The sales model: in-person and consent-gated.** ChildTrace is never sold online and carries no payment of any kind inside the app. Every deployment passes a human gate before provisioning:

- The buyer must be **40 years or older**.
- The buyer signs a **consent agreement and certificate** taking responsibility for lawful use.
- The buyer must **prove the child is their own child** — not a partner, employee, or any other adult. This is the single most important abuse control: it structurally prevents the most common stalkerware use (monitoring a partner).
- Where the person to be monitored is **18**, that person must be **present and consent in person**. (The core product targets 5–15; this rule covers the edge where a dependent has just reached majority.)

**Why this holds up.** A parent monitoring their own minor child, in a manner the child is aware of, for the child's safety, is the defensible formulation in every jurisdiction that matters. ChildTrace builds each of those elements — own child, minor, aware, safety-driven — into the product and the sale, and keeps the signed record as proof. Opesware Technologies Sarl puts its registered name behind each unit sold; the consent file is what backs that name if a deployment is ever questioned.

## Age-tiered policy model

A 5-year-old and a 15-year-old cannot live under one policy. ChildTrace runs the same capture and risk engine on every device; the child's age sets a single sensitivity dial across three bands. Monitoring is heaviest and simplest for the youngest, and narrows to genuine danger for the oldest.

| Band | Posture | Messaging | Risk sensitivity | Parent sees |
| --- | --- | --- | --- | --- |
| 5–8 | Restrict | Open messaging mostly blocked; whitelisted apps only | Backstop only | App limits, location, screen time |
| 9–12 | Flag (high) | Allowed, fully analyzed | High — broad categories | Most flagged items + controls |
| 13–15 | Flag (narrowed) | Allowed, analyzed | Only real danger (grooming, sexual coercion, self-harm) | Serious flags only; everyday chatter never surfaced |

The principle across all three: **restrict for the little ones, flag for the tweens, converse with the teens.** Raw visibility goes _down_ as the child gets older, because a 15-year-old under read-everything monitoring both loses trust and pushes the product toward the stalkerware line.

**Age ceiling (Revision 2: under-18).** The product direction extends scope to the buyer's own children under 18; anyone 18 or older is refused. The age entered at provisioning is bound to the signed consent record, so it cannot be quietly bypassed after the sale. **In code the guard still refuses 16+** (the original ceiling) until the 16–17 policy band is defined and Cameroon counsel signs off — the 16–17 grey zone carries the highest legal risk and is not opened by changing a constant. The 18+ refusal is absolute.

## System architecture

Three components, one data direction: capture and analysis happen on the child's phone, only flagged items cross to the backend, and the parent sees alerts and sets controls.

```
┌─────────────────────────┐          ┌──────────────────┐          ┌─────────────────────────┐
│      Child's phone       │          │ Opesware backend │          │     Parent's phone       │
│                          │ flagged  │                  │  alerts  │                          │
│  ┌────────────────────┐  │  only    │   ┌──────────┐   │          │  ┌────────────────────┐  │
│  │   Capture layer    │  │ ───────► │   │ Pairing  │   │ ───────► │  │     Alert feed     │  │
│  └────────────────────┘  │          │   ├──────────┤   │          │  └────────────────────┘  │
│  ┌────────────────────┐  │          │   │Alert sync│   │          │  ┌────────────────────┐  │
│  │ On-device engine ★ │  │          │   ├──────────┤   │          │  │      Controls      │  │
│  └────────────────────┘  │          │   │ Consent  │   │          │  └────────────────────┘  │
│                          │          │   │ Record   │   │          │                          │
└─────────────────────────┘          │   └──────────┘   │          └─────────────────────────┘
                                      └──────────────────┘
        ▲                                                                        │
        └────────────────────  policy + controls flow back  ──────────────────◄─┘
```

Controls and policy flow back the other way; normal messages never leave the child's device.

## Child app — capture layer

Encrypted messaging apps (WhatsApp, Instagram, Telegram, Signal) have no server or network access point. The plaintext exists only on the device after the app decrypts it, so all capture happens **on the child's phone**, through standard Android APIs, with the app installed and visible.

Two feeds work together:

- **NotificationListenerService — primary feed.** Receives notification text system-wide, including DMs from WhatsApp, Instagram, Telegram, and TikTok. It is the most reliable source and survives most app UI changes. Limit: it sees the notification content (sender + preview); it misses messages read in-app with no notification, or when previews are turned off.
- **AccessibilityService — gap-filler.** Reads on-screen text as displayed, covering what the notification feed misses (long messages, in-app reading). More powerful, more fragile: it needs per-app parsers that are maintained as WhatsApp/Instagram/Telegram/TikTok change their layouts.

A **foreground service** keeps capture alive against Android's background limits, with a persistent notification — which doubles as honest disclosure that the app is running.

**The honest limitation, stated plainly:** this is _capture-at-display_. ChildTrace sees messages from the moment it is installed forward; it cannot retrieve history sent before install, and it cannot read a message the child never opens. Marketing must never claim "reads all messages on any app" — that claim is false and is exactly what reclassifies a child-safety app as spyware.

## On-device risk engine

This is the actual product. Captured text is analyzed on the device _before_ anything reaches the parent, and only flagged items ever leave the phone. Normal messages are scored and discarded.

**Risk categories:**

- Grooming patterns — an unknown contact escalating quickly, pushing to move platforms, asking for photos, or arranging to meet
- Sexual content
- Bullying and harassment
- Self-harm and suicidal ideation
- Violence and threats
- Stranger-contact escalation (new number → rapid intimacy)

**How it runs:**

- **On-device first.** Classification happens on the phone — better for latency, privacy, data minimization, and the compliance story. The normal, non-flagged message stream never leaves the device.
- **Flag with context, not a transcript.** Output is a flagged item with enough surrounding context for the parent to understand it — never a dump of the whole conversation.
- **Optional hybrid.** Heavier NLP may run in the cloud, but only on snippets that are _already flagged_ on-device. The default and the privacy promise is on-device analysis of everything else.

**Age drives sensitivity.** The same engine applies the band from the policy model: broad and sensitive for 9–12, narrowed to serious danger for 13–15, backstop-only for 5–8 (where messaging is mostly blocked anyway). Catching the one dangerous message in a stream of two thousand is precisely what a parent scrolling manually would miss — which is why flagging is more protective than mirroring, not less.

## Parent app

The Parent app is **not an inbox mirror.** It is an alert console plus a control surface. A parent opens it to see what needs attention and to adjust the child's limits — not to read their child's conversations.

**Alert feed:**

- Flagged items, **severity-ranked** (critical danger at top)
- Each alert carries category, time, app, and the minimum context needed to act
- Status tracking (new / reviewed / dismissed) so nothing is lost
- Optional push notification for critical-severity flags

**Control surface:**

- App restrictions — block or allow specific apps
- Screen-time schedules and daily limits, downtime windows
- Location sharing — visible to the child, age-appropriate
- App-install approval for the younger bands
- Per-child policy: set the age, which sets the sensitivity band

**Multi-child:** one parent account can hold several child profiles, each with its own age, band, device pairing, and consent record.

The parent authenticates (PIN/biometric) to open the app, and the control actions are the parent's alone. The child, on their device, can always see that ChildTrace is installed and what category of thing it watches for — the transparency that keeps the whole design defensible.

## Backend and data model

The Opesware backend does three jobs: pair parent and child devices, carry flagged alerts, and hold the consent record. It deliberately holds as little as possible — flagged items only, never the normal message stream.

**Core entities:**

| Entity | Holds |
| --- | --- |
| Parent | Account, authentication, contact; one per buyer |
| Child | Profile, age, assigned policy band; belongs to a Parent |
| Device | The child's phone: pairing token, app version, capture status |
| Policy | The band's rules: sensitivity, app restrictions, schedules, location settings |
| Alert | A flagged item: category, severity, timestamp, app, context snippet, status |
| ConsentRecord | The signed agreement, buyer eligibility, proof-of-guardianship reference, provisioning date — the legal paper trail |

**Pairing:** at provisioning, the Child device and the Parent account are bound by a one-time token (QR or code), creating the Device record and linking it to the Child profile.

**Sync pipeline:** Child device → on-device engine flags an item → encrypted payload to backend → Parent app pulls/receives it in the alert feed. Only flagged Alerts travel this path.

**Data minimization and retention:**

- Normal messages are never transmitted or stored — analyzed and discarded on-device.
- Alerts are retained for a bounded window (e.g. 90 days), then auto-deleted.
- The ConsentRecord is retained for the life of the deployment as the accountability anchor.
- Full deletion on account closure, with the ConsentRecord handled per the retention policy in the compliance section.

## Onboarding and consent flow

Provisioning splits into what the **sales process** enforces (a trained Opesware agent, in person) and what the **app** enforces (technical gates). Both must pass before a child profile goes live.

**Sales process (human gate):**

1. Verify the buyer is **40 or older** (ID check).
2. Verify the buyer is the **child's own parent/guardian** — proof of guardianship (birth record, family record, or equivalent). This step blocks partner/employee surveillance.
3. Buyer reads and **signs the consent agreement and certificate**, accepting responsibility for lawful use.
4. If the person to be monitored is **18**, that person is present and **signs their own consent**.
5. Agent records buyer identity, child age, and the signed documents.

**App provisioning (technical gate):**

1. Agent creates the Parent account and enters the **child's age** — rejected if 16+.
2. The signed consent is captured into the **ConsentRecord** (scanned/photographed, linked to the profile).
3. **Child-awareness acknowledgement**: the Child app's first run shows the child (age-appropriately) that it is installed and what it watches for. For older children this is an explicit screen; for the youngest it is parent-mediated.
4. **Device pairing** via one-time token binds the child's phone to the parent account.
5. Policy band auto-selected from the entered age; capture begins.

**Division of responsibility:** the app cannot verify guardianship or the buyer's age — those are human checks, and the signed certificate is the record that they happened. The app enforces what software can: the 16+ rejection, the visible-app disclosure, the pairing binding, and the age-to-band mapping. Neither gate alone is sufficient; the design requires both.

## Data protection and compliance

Parental authority lets the parent monitor their minor child; it does **not** exempt Opesware from handling that child's data lawfully. The two obligations are separate and both apply.

**Frameworks in scope:**

- **Cameroon** — the primary jurisdiction: Law No. 2010/012 on cybersecurity/cybercriminality and the national personal-data-protection regime. Age of majority is 18. _Local counsel must confirm the exact obligations and sign off before launch_ — the citations here are a starting point, not legal advice.
- **COPPA-style rules (under-13)** — the strictest band: a child under 13 cannot consent to data processing themselves, so the parent is the verifiable consent-holder. Relevant if ChildTrace ever ships to or serves US-linked users.
- **GDPR-K** — the EU child-data standard (digital-consent age 13–16 by member state); relevant for any European exposure and a good baseline to design against.

**How ChildTrace meets them:**

- **Parent as consent-holder**, with consent captured and signed in person — stronger than the click-through consent most apps rely on.
- **Data minimization** — flagged items only; normal messages never stored.
- **Bounded retention** — alerts auto-expire; defined deletion on account closure.
- **The ConsentRecord** — the stored, signed proof that lawful consent was obtained for each specific child, available if a regulator or court ever asks.
- **Purpose limitation** — data used only for child-safety alerting, never resold or repurposed.

**Open item for counsel:** the retention period for the ConsentRecord after a deployment ends — long enough to defend the deployment, not so long it becomes its own liability. Flagged as a decision for Cameroon counsel.

## Security

ChildTrace holds sensitive data about children, so the security bar is high — and a breach would be both a safety failure and a reputational one for Opesware.

- **In transit** — all Child→backend and backend→Parent traffic over TLS, with certificate pinning on both apps to resist interception.
- **At rest** — the flagged-alert store and the ConsentRecord encrypted; context snippets treated as the most sensitive field and encrypted at the application layer.
- **Parent authentication** — strong account auth plus local PIN/biometric to open the Parent app, so a found phone does not expose a child's alerts.
- **Child-device integrity** — the Child app resists casual disabling (e.g. detects if its services are turned off and notifies the parent), but **stays within the visible-app line**: it never hides, never fakes another identity, and never blocks the device owner from seeing or uninstalling it. Tamper-_resistance_ is fine; tamper-_concealment_ is the stalkerware trap and is excluded.
- **Backend hardening** — least-privilege access, audit logging on every alert and ConsentRecord access, no third-party analytics on child data, and a defined incident-response path.
- **No data resale, ever** — a hard architectural and policy rule, and a core part of the trust proposition.

## Tech stack and dependencies

| Layer | Choice | Why |
| --- | --- | --- |
| Child + Parent apps | Native **Kotlin**, Android SDK; min API ~26 (Android 8), target current | The core (NotificationListener, AccessibilityService, foreground service, device policy) is deep system Android — native handles it cleanly |
| On-device engine | **TensorFlow Lite** / ONNX Runtime Mobile for classifiers, with a lightweight rules layer for escalation patterns | Keeps analysis on-device; small models run acceptably on mid-range phones |
| Local storage | Room (SQLite) + Android Keystore-backed encryption | Encrypted on-device state |
| Backend | Kotlin (Ktor/Spring Boot) **or** Node — team's call; PostgreSQL | REST + push; relational fits the entity model |
| Push | Firebase Cloud Messaging | Standard for Android alerts |
| Build | Gradle, Android Studio | Standard Android toolchain |

**Key Android permissions** (all requested openly, each with an in-app explanation):

- `BIND_NOTIFICATION_LISTENER_SERVICE` — the notification feed
- `BIND_ACCESSIBILITY_SERVICE` — the gap-filler feed
- Foreground-service + location permissions
- Usage-access / device-policy for app and screen-time controls

**Distribution:** Google Play restricts monitoring apps heavily. ChildTrace fits Play's disclosed-parental-control allowances _because_ it is transparent, but budget for a careful Play listing **and** a direct/sideload channel — which also aligns with the in-person sales model, where the agent can install and provision directly.

**On-device ML note:** start with a rules + lightweight-classifier hybrid for the launch categories (grooming escalation, explicit content, self-harm keywords in context), then improve models over time. Do not block the first build on a perfect model — a good rules layer plus a modest classifier already beats manual scrolling.

## Build roadmap

ChildTrace is built in three gated phases. Phase 1 is the first working milestone — capture, the on-device engine, and a basic parent alert feed — enough to run a supervised pilot.

```
Phase 1 · Core safety      ──►  [pilot-ready]  ──►  Phase 2 · Control + sync   ──►  [sellable]  ──►  Phase 3 · Launch-ready   ──►  [LAUNCH]
  • Capture layer                                     • App + screen-time limits                       • Consent + onboarding
  • On-device risk engine                             • Backend + alert sync                           • Security hardening
  • Parent alert feed                                 • Location, multi-child                          • Counsel sign-off
```

Each gate is go/no-go: pilot-ready before field testing, sellable before the in-person launch, and counsel sign-off inside Phase 3 before any unit ships.
