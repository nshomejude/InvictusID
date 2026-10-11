# ChildTrace

A visible, consent-gated Android child-safety product for parents of their own children **under 18**, by [Opesware Technologies Sarl](https://opesware.com). It gives a vetted parent disclosed monitoring of their child's device and warns them of real online danger — grooming, sexual content, bullying, self-harm pressure, stranger escalation — with an on-device risk engine that ranks danger to the top.

> **Read first:** [`CLAUDE.md`](CLAUDE.md) holds the non-negotiable product invariants. The Child app is **always visible** (never covert), monitoring is the parent's **own minor child only**, data is **encrypted end-to-end to the parent**, and the **16–17 band and launch both require counsel sign-off**. Nothing here is a finished legal position — see invariant 7.

- 📄 **Design spec:** [`docs/childtrace-spec.md`](docs/childtrace-spec.md)
- 🎨 **Styling system:** [`docs/styling-spec.md`](docs/styling-spec.md)

## Module layout

```
shared/      Pure Kotlin/JVM — domain model, on-device risk engine, provisioning guard,
             envelope-encryption + wire contract. Builds and unit-tests on any JDK 17+.
design/      Android library — the Compose design system (tokens, theme, motion).
child-app/   Android — capture services, on-device analyzer, encrypted store, visible
             disclosure + pairing UI.
parent-app/  Android — biometric-gated console: alerts, activity-by-app, full activity,
             controls; pairing; end-to-end decryption with a Keystore keyring.
backend/     Kotlin + Ktor + PostgreSQL — pairing/onboarding, encrypted alert & activity
             sync, consent records, audit log, bounded retention. Holds ciphertext only.
```

## What works end to end

Agent records consent → opens a pairing session → parent claims it (registers an RSA key) →
child device pairs (capture starts). The child's device analyzes activity on-device, seals it
to the parent's public key, and syncs via WorkManager. The parent console pulls and decrypts
it. Security: SQLCipher at rest, TLS cert pinning, RSA-OAEP + AES-GCM envelope encryption with
key rotation, per-access audit logging, and retention pruning.

## Build & test

Requires **JDK 17+**. Use the Gradle wrapper.

```bash
# Core engine + backend (no Android SDK needed):
./gradlew :shared:test :backend:test

# Run the backend locally (in-memory store; set DATABASE_URL for PostgreSQL):
./gradlew :backend:run        # serves on http://localhost:8080  (GET /api/v1/health)
```

The Android modules (`:design`, `:child-app`, `:parent-app`) require the **Android SDK** and the
Android Gradle Plugin (from Google's Maven). They wire into the build automatically when an SDK is
detected (`ANDROID_HOME` / `ANDROID_SDK_ROOT` / a `local.properties`); otherwise the build is just
`:shared` + `:backend`, so CI without an SDK still works. With Android Studio:

```bash
./gradlew :child-app:assembleDebug :parent-app:assembleDebug
```

## Deploy (backend)

```bash
# Build a runnable distribution:
./gradlew :backend:installDist     # -> backend/build/install/backend/bin/backend

# Or with Docker:
docker build -t childtrace-backend -f backend/Dockerfile .
docker run -p 8080:8080 \
  -e DATABASE_URL="jdbc:postgresql://db:5432/childtrace" \
  -e DATABASE_USER=childtrace -e DATABASE_PASSWORD=... \
  childtrace-backend
```

Environment variables — see [`.env.example`](.env.example):

| Var | Purpose | Default |
| --- | --- | --- |
| `DATABASE_URL` | PostgreSQL JDBC URL; unset → in-memory store (dev/test) | _(in-memory)_ |
| `DATABASE_USER` / `DATABASE_PASSWORD` | DB credentials | `childtrace` / _(empty)_ |
| `CHILDTRACE_ALLOW_16_17` | Enable the 16–17 band (counsel sign-off); `true` to allow | off |

Before shipping: replace the placeholder TLS pins in each app's `TlsPinning`, wire the three
Google Fonts in `:design`, and complete the Cameroon counsel sign-off (invariant 7).

## Status

Phase 1 core is complete and tested end to end: `shared` (30 tests) and `backend` (11 tests) are
green on any JDK 17+; the Android apps build with the SDK. See `CLAUDE.md` for the roadmap.

---

© Opesware Technologies Sarl. All rights reserved.
