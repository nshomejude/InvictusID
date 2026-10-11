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

## Architecture (planned)

Native **Kotlin**, three components:

- `child-app/` — capture (NotificationListenerService + AccessibilityService), on-device risk engine, foreground service, local encrypted store (Room + Keystore).
- `parent-app/` — alert console + control surface (app limits, screen-time, location, install approval), PIN/biometric auth.
- `backend/` — device pairing, flagged-alert sync, ConsentRecord store. Kotlin (Ktor/Spring Boot) or Node; PostgreSQL. Holds flagged items only.

See the spec's **System architecture**, **Backend and data model**, and **Tech stack** sections for detail.

## Build roadmap

- **Phase 1 · Core safety** (first milestone): capture layer, on-device risk engine, basic parent alert feed → pilot-ready.
- **Phase 2 · Control + sync**: app/screen-time controls, backend + alert sync, location, multi-child → sellable.
- **Phase 3 · Launch-ready**: consent/onboarding tooling, security hardening, Cameroon counsel sign-off → launch.

## Status

This repo currently holds the **design specification only**. No application code has been scaffolded yet. The next step is the Phase 1 Kotlin scaffold (child-app capture services + risk-engine stub + parent-app shell).

## Working notes for Claude Code

- Keep the spec (`docs/childtrace-spec.md`) as the source of truth; update it when design decisions change.
- Legal/compliance items (Cameroon Law No. 2010/012, data-protection regime, ConsentRecord retention) need local counsel sign-off — do not treat the spec's citations as settled law.
- When in doubt about a monitoring feature, apply the "always visible / flag don't mirror" test above.
