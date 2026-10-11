# CLAUDE.md — ChildTrace

Project context for Claude Code and any developer working in this repo. Read this first.

## What ChildTrace is

An Android child-safety product for parents of children **aged 5–15** (hard ceiling at 16). It warns a parent when their child hits real online danger — grooming, sexual content, bullying, self-harm pressure, stranger escalation — by flagging those signals, **not** by mirroring every message.

Built by **Opesware Technologies Sarl** (Cameroon). Full design in [`docs/childtrace-spec.md`](docs/childtrace-spec.md).

## Non-negotiable design constraints

These are product invariants. Do not implement anything that violates them, and flag any request that would:

1. **The Child app is always visible.** It shows in the launcher/app list and states what it monitors. There is **no hidden-icon / stealth mode** and none is to be added. Hiding it turns the product into stalkerware → Play removal, antivirus flags, legal liability.
2. **Flag, don't mirror.** The parent sees severity-ranked flagged items with minimal context — never a full inbox/transcript. Normal messages are analyzed on-device and discarded.
3. **On-device first.** Capture and classification run on the child's phone. Only flagged items leave the device. Any cloud NLP is on already-flagged snippets only (pending the decision in the spec's open comment).
4. **Consent-gated, in-person sale.** No in-app purchase, no online sale, no payment code anywhere in the apps. Provisioning is gated by a human sales process (buyer 40+, signed consent, proof the child is their own child, child present to consent at 18).
5. **Hard age ceiling at 16.** The app rejects provisioning any profile aged 16+. Age is bound to the ConsentRecord.
6. **No data resale.** Ever. Data is used only for child-safety alerting.

If a change request conflicts with any of the above, stop and raise it rather than implementing it.

**Note on per-app visibility (WhatsApp, Snapchat, Telegram, Instagram, TikTok).** The Child app analyzes messages from these apps *on the device*. The Parent app may show a per-app view (an "Activity by app" screen) that surfaces, per app, the **count and severity of flagged items** and lets the parent open those flagged items with minimal context. It must **never** present an app's messages as a readable inbox, thread, or transcript. A "show me WhatsApp/Snapchat/Telegram messages" request is a request for the full mirror that invariant 2 forbids — build the flag-only per-app view instead, and say why.

## Architecture

Native **Kotlin** apps + a Kotlin/Ktor backend, with a shared pure-Kotlin core. Gradle multi-module:

- `shared/` — **pure Kotlin/JVM, no Android dependency.** Domain model + the on-device risk engine and its rules, plus the provisioning age-ceiling guard. This is the testable product core; the child-app's analyzer calls into it. Builds and unit-tests on any JVM without the Android SDK.
- `child-app/` — capture (NotificationListenerService + AccessibilityService), foreground service, on-device analyzer wiring over `shared`, local encrypted store (Room + Keystore), always-visible disclosure UI (Jetpack Compose).
- `parent-app/` — alert console + per-app activity + control surface (app limits, screen-time, location, install approval), PIN/biometric auth (Jetpack Compose).
- `backend/` — **Kotlin + Ktor + PostgreSQL** (decided — one language across the whole stack). Device pairing, flagged-alert sync, ConsentRecord store. Holds flagged items only.

See the spec's **System architecture**, **Backend and data model**, and **Tech stack** sections for detail.

## Build & toolchain notes

- Build with the Gradle wrapper (`./gradlew`). `shared` targets Java 17 bytecode; use a JDK 17+.
- The Android modules require the **Android Gradle Plugin** from Google's Maven. They are wired into `settings.gradle.kts` **only when an Android SDK is detected** (`ANDROID_HOME`/`ANDROID_SDK_ROOT`/`local.properties`), so a CI box or sandbox without one still builds `:shared` and `:backend`. On a dev machine with Android Studio they wire in automatically.
- Run the core tests with `./gradlew :shared:test`.

## Design system

The UI reference lives in the design mockups (see the artifact linked from the session / README). The visual system: teal brand accent, a **separate** semantic severity palette (critical/high/medium), Fraunces (display) + Public Sans (body) + IBM Plex Mono (data). Implement the Compose theme from these tokens. Keep the Child app's disclosure framing — "Active & visible" state, plain "what it looks for", explicit "it does not read your chats".

## Build roadmap

- **Phase 1 · Core safety** (first milestone): capture layer, on-device risk engine, basic parent alert feed → pilot-ready.
- **Phase 2 · Control + sync**: app/screen-time controls, backend + alert sync, location, multi-child → sellable.
- **Phase 3 · Launch-ready**: consent/onboarding tooling, security hardening, Cameroon counsel sign-off → launch.

## Status

Phase 1 in progress. Done: Gradle multi-module scaffold, the `shared` core (domain model + on-device risk engine + provisioning guard) with passing unit tests, and the UI design mockups. Next: `child-app` capture services + analyzer wiring, `parent-app` Compose shell (alert feed, activity-by-app, controls), and the Ktor `backend`.

## Working notes for Claude Code

- Keep the spec (`docs/childtrace-spec.md`) as the source of truth; update it when design decisions change.
- Legal/compliance items (Cameroon Law No. 2010/012, data-protection regime, ConsentRecord retention) need local counsel sign-off — do not treat the spec's citations as settled law.
- When in doubt about a monitoring feature, apply the "always visible / flag don't mirror" test above.
