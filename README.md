# ChildTrace

An Android child-safety app for parents of children aged 5–15, by [Opesware Technologies](https://opesware.com).

ChildTrace warns a parent when their child encounters real online danger — grooming, sexual content, bullying, self-harm pressure, a stranger escalating contact — by **flagging** those signals on the child's device, not by mirroring every message. The app is always visible to the child, sold only in person under signed parental consent, and carries no in-app payment.

- 📄 **Design specification:** [`docs/childtrace-spec.md`](docs/childtrace-spec.md)
- 🤖 **Project guide for contributors / Claude Code:** [`CLAUDE.md`](CLAUDE.md)

## Core principles

| Principle | What it means |
| --- | --- |
| Always visible | The Child app is never hidden. No stealth mode. |
| Flag, don't mirror | Parents see flagged danger with context, not a full inbox. |
| On-device first | Analysis runs on the phone; only flagged items sync. |
| Consent-gated | In-person sale, signed consent, proof the child is the buyer's own. |
| Age ceiling 16 | No profile is provisioned for anyone 16 or older. |

## Structure (planned)

```
child-app/    Kotlin — capture + on-device risk engine (Android)
parent-app/   Kotlin — alert console + controls (Android)
backend/      Kotlin or Node + PostgreSQL — pairing, alert sync, consent records
docs/         Specification and design docs
```

## Status

Design phase. This repository currently contains the specification; application scaffolding is the next step (Phase 1 — Core safety).

---

© Opesware Technologies Sarl. All rights reserved.
