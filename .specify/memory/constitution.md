# CaféOS Tablet Constitution

CaféOS Tablet is a native Android application for café POS terminals (tablets).
It operates as a local-first, offline-capable node within the CaféOS ecosystem,
syncing with the main CaféOS server when connectivity is available but never
requiring it for core operations.

This document supersedes any agent's defaults. If a request conflicts with a
principle below, follow the principle and say so — do not quietly pick one.

## Core Principles

### I. Offline-first, offline-always (NON-NEGOTIABLE)

Ordering, payment recording, kitchen flow, and receipts MUST keep working with
zero internet connectivity. Each tablet's local SQLite database is the source of
truth for that device's session; nothing may assume a central database, a shared
cache, or a remote API is reachable. A feature that degrades to unusable when the
network drops is a defect, not a trade-off.

### II. Data integrity and money handling (NON-NEGOTIABLE)

- **One total, one place it is computed.** Order totals are calculated by
  exactly one function, from line items. No screen gets its own rounding or
  formatting logic.
- **Never trust a client-supplied amount.** Prices are derived from local
  product records. A peso figure arriving in an intent or API response is an
  input to validate, never a value to apply.
- **Receipts are sequential and immutable.** Receipt numbers are assigned inside
  the same transaction that finalizes the order.
- A finalized order or payment is never deleted or edited. Corrections happen
  via a linked reversal or adjustment record.
- Database writes that affect financial records use a single transaction.

### III. Thin data layer, clear boundaries (NON-NEGOTIABLE)

- Room DAOs are the sole owners of database access. UI layer (Composables) MUST
  NOT call SQL or query builders directly.
- ViewModel exposes state via StateFlow. All UI reads are from StateFlow.
- Validation happens at every boundary: form input, barcode scans, NFC reads,
  imported data.

### IV. Premium minimal design

- Material 3 with a moss-green / paper color palette.
- Typography hierarchy with clear visual weight.
- Touch targets ≥ 48dp.
- Motion is purposeful and fast (< 300ms).

### V. Tests are the deliverable, not the chore (NON-NEGOTIABLE)

- Every ViewModel function gets a JUnit test.
- Every UI state transition gets a Compose UI test where feasible.
- Every bug fix adds its regression test first (failing), then the fix.
- Instrumentation tests run via `./gradlew connectedAndroidTest`.
- Never delete or weaken a test to make a build pass.

## Locked Technical Decisions

Settled. Do not reopen these.

| Concern | Choice |
|---|---|
| Language | Kotlin 2.0 (JVM 21 target) |
| UI | Jetpack Compose (Material 3) |
| Persistence | Room (SQLite) |
| Navigation | Jetpack Navigation Compose (single-activity) |
| Architecture | MVVM + StateFlow |
| Minimum SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |
| Build system | Gradle (Kotlin DSL) |
| Async | Kotlin Coroutines + Flow |
| Testing | JUnit 5, Compose UI Test, AndroidX Test |
| Offline | Local-first; sync is opportunistic, never required |

## Development Workflow

Specs live in `specs/NNN-slug/`. The workflow is Spec Kit's:
`specify → clarify → plan → tasks → analyze → implement`, plus `converge` for
reconciling a spec against code that already exists.

Work is done when: tests pass, lint passes, build passes, every acceptance
criterion is checked, no spec deviation is unresolved, and no generated output,
secrets, or debug scripts are committed. Debug scripts are deleted as part of
finishing a task.

Never commit build output (`.gradle/`, `build/`, generated files) or real
secrets. `.env.example` documents every required variable with no values.

## Governance

This constitution supersedes agent defaults and prior practice. Amendments are
made by editing this file with a version bump and a note in the PR that changes
it — not by an agent deciding a principle is inconvenient. Where a rule can be
enforced mechanically (lint, CI, a type), the mechanical enforcement is the
control and the written rule is documentation of it.

**Version**: 1.0.0 | **Ratified**: 2026-09-07 | **Last Amended**: 2026-09-07
