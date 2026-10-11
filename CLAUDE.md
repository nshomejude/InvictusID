# CLAUDE.md — ChildTrace

Project context for Claude Code and any developer working in this repo. Read this first.

## What ChildTrace is

An Android child-safety product for parents of their own children **under 18**. It gives a vetted parent visible, disclosed monitoring of their child's device and warns them of real online danger — grooming, sexual content, bullying, self-harm pressure, stranger escalation — with the on-device risk engine ranking that danger to the top. It is sold only through a reviewed parent application, never self-serve, and is never hidden from the child.

Built by **Opesware Technologies Sarl** (Cameroon). Full design in [`docs/childtrace-spec.md`](docs/childtrace-spec.md).

## Non-negotiable design constraints

These are product invariants. Do not implement anything that violates them, and flag any request that would:

1. **The Child app is always visible.** It shows in the launcher/app list and states what it monitors. There is **no hidden-icon / stealth mode** and none is to be added. This is the firewall: visible, disclosed monitoring of a parent's own minor child is a defensible parental-control category; covert monitoring is stalkerware (Play removal, antivirus flags, legal liability). Everything the parent can see, the child is told is visible. This invariant does not relax for any reason.
2. **Comprehensive monitoring is allowed — but only openly.** The Parent app may give the parent full visibility into their own minor child's device activity (messages across apps, browsing, installed apps, location, screen content), *provided* invariant 1 holds: the Child app discloses plainly, in age-appropriate terms, that this is happening. The on-device risk engine still ranks and surfaces danger first, so comprehensive access supplements the alert feed rather than leaving a parent to scroll. Nothing is ever collected covertly, and monitoring never extends to anyone but the buyer's own minor child.
3. **On-device classification; disciplined sync.** Capture and risk classification run on the child's phone. Content synced to the parent for viewing is encrypted in transit and at rest, access-logged, retention-bounded, and scoped strictly to the parent's own paired child. The danger-ranking signal is always computed on-device.
4. **Consent-gated, application-based sale.** No in-app purchase, no self-serve online sale, no payment code anywhere in the apps. Provisioning is gated by a reviewed application: buyer 40+, documentary evidence, and proof the child is the buyer's own child. This gate — controlling who ever holds the tool — is the primary anti-abuse control and must not be weakened.
5. **Under-18 only; adults refused.** The app provisions profiles for the buyer's own children **under 18** and rejects anyone 18 or older. The 16–17 band carries materially higher legal risk and **requires Cameroon counsel sign-off before launch** (see invariant 7). Age is bound to the ConsentRecord.
6. **No data resale.** Ever. Data is used only for child-safety monitoring and alerting, never sold, shared, or repurposed.
7. **Counsel sign-off gates launch.** Because the monitoring is now comprehensive and extends to under-18s, lawful-basis, proportionality, retention, and the child's own data-protection rights (Cameroon Law No. 2010/012 and the data-protection regime; COPPA-style and GDPR-K as design baselines) must be confirmed by local counsel before any unit ships. Do not treat the breadth of monitoring as settled law.

If a change request conflicts with any of the above, stop and raise it rather than implementing it. The one request that is always refused regardless of framing: making the Child app covert (hidden icon, stealth, disguised identity, or blocking uninstall by the device owner).

**Per-app visibility (WhatsApp, Snapchat, Telegram, Instagram, TikTok).** The Child app captures and analyzes these on-device. The Parent app's "Activity by app" view may show per-app activity and content for the parent's own minor child, with danger flagged and ranked on top. It stays within invariant 1: the child is told, on their device, that these apps are monitored. It is monitoring, not interception of third parties — only the child's own device is read.

## Architecture

Native **Kotlin** apps + a Kotlin/Ktor backend, with a shared pure-Kotlin core. Gradle multi-module:

- `shared/` — **pure Kotlin/JVM, no Android dependency.** Domain model + the on-device risk engine and its rules, plus the provisioning age-ceiling guard. This is the testable product core; the child-app's analyzer calls into it. Builds and unit-tests on any JVM without the Android SDK.
- `child-app/` — capture (NotificationListenerService + AccessibilityService), foreground service, on-device analyzer wiring over `shared`, local encrypted store (Room + Keystore), always-visible disclosure UI (Jetpack Compose).
- `parent-app/` — alert console (danger ranked first) + per-app activity + full device-activity views + control surface (app limits, screen-time, location, install approval), PIN/biometric auth (Jetpack Compose).
- `backend/` — **Kotlin + Ktor + PostgreSQL** (decided — one language across the whole stack). Device pairing, encrypted activity + alert sync, ConsentRecord store. All synced content encrypted, access-logged, retention-bounded, scoped to the parent's own child.

See the spec's **System architecture**, **Backend and data model**, and **Tech stack** sections for detail.

## Build & toolchain notes

- Build with the Gradle wrapper (`./gradlew`). `shared` targets Java 17 bytecode; use a JDK 17+.
- The Android modules require the **Android Gradle Plugin** from Google's Maven. They are wired into `settings.gradle.kts` **only when an Android SDK is detected** (`ANDROID_HOME`/`ANDROID_SDK_ROOT`/`local.properties`), so a CI box or sandbox without one still builds `:shared` and `:backend`. On a dev machine with Android Studio they wire in automatically.
- Run the core tests with `./gradlew :shared:test`.

## Design system

The UI reference lives in the design mockups (see the artifact linked from the session / README). The visual system: teal brand accent, a **separate** semantic severity palette (critical/high/medium), Fraunces (display) + Public Sans (body) + IBM Plex Mono (data). Implement the Compose theme from these tokens. The Child app's disclosure framing is load-bearing for invariant 1: an "Active & visible" state and a plain, honest, age-appropriate statement that the device is monitored and what the parent can see. The mockups predate the comprehensive-monitoring decision and still show flag-only copy ("it does not read your chats") — that child-facing copy must be rewritten to be truthful about the actual scope before it ships.

## Build roadmap

- **Phase 1 · Core safety** (first milestone): capture layer, on-device risk engine, basic parent alert feed → pilot-ready.
- **Phase 2 · Control + sync**: app/screen-time controls, backend + alert sync, location, multi-child → sellable.
- **Phase 3 · Launch-ready**: consent/onboarding tooling, security hardening, Cameroon counsel sign-off → launch.

## Status

Phase 1 in progress. Done: Gradle multi-module scaffold, the `shared` core (domain model + on-device risk engine + provisioning guard) with passing unit tests, and the UI design mockups. Next: `child-app` capture services + analyzer wiring, `parent-app` Compose shell (alert feed, activity-by-app, controls), and the Ktor `backend`.

## Working notes for Claude Code

- Keep the spec (`docs/childtrace-spec.md`) as the source of truth; it still describes the earlier flag-only model and needs updating to the comprehensive-but-visible monitoring decision recorded here.
- Legal/compliance items (Cameroon Law No. 2010/012, data-protection regime, ConsentRecord retention, lawful basis for comprehensive under-18 monitoring) need local counsel sign-off before launch — do not treat any of this as settled law.
- When in doubt about a monitoring feature, apply the test: is it **visible to the child**, on the buyer's **own minor child** only, encrypted and retention-bounded, and would counsel stand behind it? Covertness is the one answer that is always disqualifying.
