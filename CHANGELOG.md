# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

> **Pre-1.0 note:** Until `v1.0.0`, breaking changes may ship in `MINOR` bumps. This is the standard pre-1.0 SemVer convention.

---

## [Unreleased]

### Added
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

### Changed
- `.github/workflows/ci.yml` now runs `scripts/privacy-scan.sh` in the hygiene job; CI job renamed to `Hygiene` to match branch protection requirement
- `AGENTS.md` references new governance files; explicitly forbids Tailscale hostnames, machine IDs, AGC fingerprints

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
