# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

> **Pre-1.0 note:** Until `v1.0.0`, breaking changes may ship in `MINOR` bumps. This is the standard pre-1.0 SemVer convention.

---

## [Unreleased]

### Added
- **v0.4.0-alpha.1** — Real HMS Wear Engine P2P (end-to-end)
  - `phone/app/src/main/java/com/halaprix/leakwatch/p2p/BatteryReadingSerializer.kt` — JSON serialization for P2P
  - `phone/app/src/main/java/com/halaprix/leakwatch/p2p/WearEngineReceiver.kt` — real Wear Engine DataClient receiver
  - `watch/entry/src/main/ets/service/WearEngineSender.ets` — real Wear Engine DataClient sender (replaces stub)
  - `docs/E2E_TESTING.md` — end-to-end testing guide with AGC setup
  - HMS Wear Engine 5.0.0.300 dependency (both watch and phone)
  - HMS Maven repository in phone/build.gradle.kts
  - AndroidManifest.xml: WearEngineReceiverService declaration with intent filter
  - P2P data format: JSON array of BatteryReading objects
  - Path: `/leakwatch/battery`, Key: `battery_readings`
- **v0.2.0-alpha.1** — Phone-side data layer + daily aggregation
  - `phone/app/src/main/java/com/halaprix/leakwatch/data/DailySummary.kt` — daily battery summary entity
  - `phone/app/src/main/java/com/halaprix/leakwatch/data/DailySummaryDao.kt` — DAO for daily summaries
  - `phone/app/src/main/java/com/halaprix/leakwatch/worker/DailyAggregationWorker.kt` — WorkManager worker for daily stats
  - `phone/app/src/main/java/com/halaprix/leakwatch/LeakWatchApplication.kt` — Application class with WorkManager scheduling
  - Database schema v2: added `daily_summaries` table
  - 30-day retention policy for raw readings and summaries
  - Unit tests for aggregation logic
- `scripts/privacy-scan.sh` — standalone privacy scanner (also wired into CI)
- `ROADMAP.md` — living project roadmap
- `docs/privacy.md` — developer-facing privacy rules (distinct from `docs/PRIVACY.md` which is user-facing)
- `docs/pr-flow.md` — PR lifecycle documentation
- `docs/AGC_SETUP.md` — manual AGC setup checklist (1-time, before v0.4.0-alpha.1)
- `watch/` — HarmonyOS NEXT (ArkTS) project scaffold for wearable device
  - `watch/AppScope/app.json5` — app-level config (bundleName `com.halaprix.leakwatch`)
  - `watch/entry/` — entry module with placeholder EntryAbility and Index page
  - `watch/build-profile.json5`, `watch/oh-package.json5` — build tooling
  - Target: HarmonyOS 5.0.0(12), wearable device type
- `watch/entry/src/main/ets/model/BatteryRecord.ets` — data model for battery readings
- `watch/entry/src/main/ets/data/BatteryTable.ets` — RDB schema (24h rolling window)
- `watch/entry/src/main/ets/service/BatteryMonitor.ets` — singleton with 120s polling + listener API
- `watch/entry/src/main/ets/service/WearEngineSender.ets` — stub with 10min batched flush cadence
  - Real HMS Wear Engine P2P integration lands in v0.4.0-alpha.1 (requires AGC fingerprint)
  - Stub logs queue length so the batching cadence is verifiable in DevEco HiLog
- `phone/` — Android Kotlin + Compose placeholder APK
  - `phone/settings.gradle.kts`, `phone/build.gradle.kts`, `phone/gradle.properties` — Gradle config
  - `phone/app/build.gradle.kts` — app module (compileSdk 34, minSdk 26, Compose BOM 2024.08, Room 2.6.1, WorkManager 2.9.1)
  - `phone/app/src/main/AndroidManifest.xml` — manifest with MainActivity
  - `phone/app/src/main/java/com/halaprix/leakwatch/MainActivity.kt` — placeholder Compose UI
  - `phone/app/src/main/java/com/halaprix/leakwatch/ui/theme/` — Material3 theme (Color, Type, Theme)
  - `phone/app/src/main/res/values/` — strings.xml, themes.xml, colors.xml
  - Note: gradle wrapper JAR + scripts not committed (binary); generate locally with `gradle wrapper`
- `phone/app/src/main/java/com/halaprix/leakwatch/data/` — Room database layer
  - `BatteryReading.kt` — entity mirroring watch-side BatteryRecord
  - `BatteryReadingDao.kt` — DAO with Flow-based queries (all readings, since timestamp, latest, per-day)
  - `AppDatabase.kt` — Room database singleton (leakwatch.db, v1)
- `phone/app/src/main/java/com/halaprix/leakwatch/p2p/WearEngineReceiver.kt` — mock P2P receiver
  - Simulates watch data every 120s (matching watch polling interval)
  - `insertMockBatch(50)` — bulk insert for testing
  - Real HMS Wear Engine P2P integration lands in v0.4.0-alpha.1

### Changed
- `.github/workflows/ci.yml` now runs `scripts/privacy-scan.sh` in the hygiene job; CI job renamed to `Hygiene` to match branch protection requirement
- `AGENTS.md` references new governance files; explicitly forbids Tailscale hostnames, machine IDs, AGC fingerprints
- `watch/entry/src/main/ets/pages/Index.ets` — live battery card driven by BatteryMonitor
- `watch/entry/src/main/ets/entryability/EntryAbility.ets` — owns BatteryMonitor lifecycle (init/destroy)
- `docs/ARCHITECTURE.md` — filled hard-mode sampling rules (120s poll, 30s listener cooldown, 10min batch flush, no foreground service)
- `BatteryMonitor` now starts/stops `WearEngineSender` in its lifecycle
- `phone/app/src/main/java/com/halaprix/leakwatch/MainActivity.kt` — integrated mock P2P receiver with UI button for bulk insert testing

---

## [0.1.0-alpha.0] — 2026-06-16

### Added
- Initial repo scaffold (`halaprix/leakwatch`)
- `README.md` with project description, badge wall, platform support matrix, tech stack
- `AGENTS.md` with operating rules for LLM coding agents (privacy, git workflow, SemVer, no payments, no HA)
- `CONTRIBUTING.md` with Conventional Commits guide and one-commit-per-fix rule
- `CHANGELOG.md` (this file) following Keep a Changelog 1.1.0
- `CODE_OF_CONDUCT.md` (Contributor Covenant v2.1)
- `SECURITY.md` with security policy + privacy-policy pointer
- `LICENSE` (Apache-2.0)
- `.gitignore` for HarmonyOS + Android + Kotlin + ArkTS + Node
- `.editorconfig`
- `docs/ARCHITECTURE.md` (skeleton — hard-mode sampling rules to land in next commit)
- GitHub Actions: CI smoke, release drafter, CodeQL
- Issue templates: bug report, feature request, question
- PR template

### Not yet added (intentional)
- No app code in `watch/` or `phone/` — lands in the next alpha drops
- No screenshots yet — design and store assets land with the first UI milestone

---

## Versioning legend

- **MAJOR** — breaking API or data-format change
- **MINOR** — backwards-compatible feature
- **PATCH** — backwards-compatible bugfix
- **Pre-release suffix** — `-alpha.N`, `-beta.N`, `-rc.N` per SemVer §9

[Unreleased]: https://github.com/halaprix/leakwatch/compare/v0.1.0-alpha.0...HEAD
[0.1.0-alpha.0]: https://github.com/halaprix/leakwatch/releases/tag/v0.1.0-alpha.0
