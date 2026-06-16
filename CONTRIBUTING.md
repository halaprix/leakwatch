# Contributing to LeakWatch

First off: thank you. LeakWatch is a build-in-public project, and every issue, PR, watch report, and typo fix helps. This document explains how to contribute effectively without friction.

> **TL;DR:** Conventional Commits, one logical change per commit, push after every commit, and read `AGENTS.md` if you're an LLM coding agent.

---

## Code of Conduct

This project follows the [Contributor Covenant v2.1](./CODE_OF_CONDUCT.md). By participating, you agree to uphold it. Be excellent to each other.

---

## What we welcome

- 🐛 **Bug reports** — open an issue with reproduction steps.
- 💡 **Feature requests** — open an issue first; let's discuss before you build.
- 📖 **Docs improvements** — typos, clarifications, screenshots all welcome.
- 🔧 **Pull requests** — bug fixes, refactors, new features (after discussion).
- 🌍 **Translations** — UI strings and docs, especially Chinese for the AppGallery audience.
- 🔋 **Battery measurements** — real-world drain reports on specific watch models are gold.

## What needs a heads-up first

- **Large refactors** — open an issue, link the design doc, get sign-off.
- **New HMS Kit integrations** — we don't add kits we can't justify.
- **Any payment-related code** — out of scope, do not propose.
- **Home Assistant / smart-home integration** — out of scope, belongs in a separate repo.

---

## Development Setup

### Prerequisites

| Tool | Version | Why |
|---|---|---|
| DevEco Studio | 5.1.0+ | Watch module (HarmonyOS NEXT) |
| Android Studio | Koala+ (2024.1.1) | Phone module |
| HMS Toolkit plugin | latest | HMS integration in Android Studio |
| JDK | 17+ | Kotlin + Gradle |
| Node.js | 20+ | Tooling (lint, scripts) |
| Git | 2.40+ | Conventional commit hooks |

### Cloning

```bash
git clone https://github.com/halaprix/leakwatch
cd leakwatch
```

> The `watch/` and `phone/` modules land in their first milestones. The repo today is scaffold + docs. Run `git log --oneline` to see the bootstrap.

---

## Git Workflow

### 1. Branch

Branch from `main`:

```bash
git checkout -b feat/<short-kebab-name>
git checkout -b fix/<issue-number>-<short-kebab-name>
git checkout -b docs/<topic>
git checkout -b chore/<topic>
```

### 2. Commit

**Conventional Commits are mandatory.** Format:

```
<type>(<scope>): <subject>

<body>

<footer>
```

| Type | Use for |
|---|---|
| `feat` | New user-visible feature |
| `fix` | Bug fix |
| `docs` | Documentation only |
| `style` | Formatting, no code change |
| `refactor` | Code change, no behavior change |
| `perf` | Performance improvement |
| `test` | Adding or fixing tests |
| `build` | Build system / dependency changes |
| `ci` | CI / GitHub Actions changes |
| `chore` | Tooling, scaffolding, housekeeping |
| `revert` | Reverting a previous commit |

**Scope examples:** `watch`, `phone`, `wear-engine`, `rdb`, `room`, `vico`, `ci`, `docs`.

**Subject:** imperative, lowercase, no period, max 72 chars. Example: `feat(watch): sample batterySOC at 120s interval`.

**Body:** wrap at 100 chars, explain *what* and *why* (not *how*). Reference the issue: `Refs #42.` or `Closes #42.`.

**Footer for breaking changes:** `BREAKING CHANGE: <explanation>`.

### 3. One logical change per commit

Do not batch unrelated fixes. If your PR has two independent fixes, split into two commits (and ideally two PRs).

### 4. Push after every commit

Marian reads diffs, not the working tree. Push as you go.

### 5. Open a PR

Open a PR against `main`. Fill in the PR template. **Wait for review from at least one other human or a different LLM agent** than the one that wrote the change.

### 6. Squash or rebase?

- **Feature branches:** rebase-merge to keep a linear history.
- **Hotfixes / single-commit PRs:** squash-merge.
- Marian decides per-PR; both are fine.

---

## Commit Message Examples

### Good ✅

```
feat(watch): sample batterySOC every 120s instead of 60s

Reduces watch-side drain by ~40% in early tests while still
capturing drain curves with <2% resolution. Listener-driven
events (charging, low-battery) still fire immediately.

Refs #17.
```

```
fix(wear-engine): retry P2P send on DEVICE_OFFLINE callback

Wear Engine occasionally returns DEVICE_OFFLINE for paired
watches that wake up after deep sleep. Adding one retry with
a 5s backoff recovers the message in 95% of cases.

Fixes #42.
```

### Bad ❌

```
fixed stuff
```

```
WIP feat(watch) Battery monitor (some changes to RDB, also bumped
gradle, also fixed typo in README, will clean up later)
```

---

## Coding Style

### ArkTS (watch)

- Follow Huawei's [ArkTS coding style guide](https://developer.huawei.com/consumer/en/doc/harmonyos-guides/arkts-coding-style-guide-0000001820340993).
- Prefer `@Entry` + `@Component` + `@State` for UI.
- Use the `BatteryMonitor` singleton pattern; see `docs/ARCHITECTURE.md` §3.
- **Memory-leak discipline:** always pair `batteryInfo.on(...)` with `batteryInfo.off(...)` in `aboutToDisappear`.

### Kotlin (phone)

- [Kotlin official style guide](https://kotlinlang.org/docs/coding-conventions.html).
- `ktlint` enforced via Gradle.
- `detekt` for additional lint.
- Compose: state hoisting, no side effects in composables.

### Imports

- Wildcard imports: ❌
- Group: stdlib → third-party → first-party, blank line between.

---

## Testing

| Layer | Tool | Run |
|---|---|---|
| ArkTS | Deveco Studio unit test + `@ohos.test` | DevEco test runner |
| Kotlin | JUnit5 + Turbine + Compose UI test | `./gradlew test phoneDebugUnitTest` |
| Architecture | (TBD) | (TBD once we have modules) |

Tests are not yet required for PRs (we have no app code), but they will become required before `v1.0.0`.

---

## Versioning & Releases

We follow [SemVer 2.0.0](https://semver.org/). Tags: `vMAJOR.MINOR.PATCH`, always `v`-prefixed.

- Until `v1.0.0`: breaking changes are allowed in `MINOR` bumps (pre-1.0 convention).
- After `v1.0.0`: breaking changes bump `MAJOR`.
- Every release updates `CHANGELOG.md` with the `Added / Changed / Fixed / Removed` sections.
- Releases are cut by Marian or via the `release.yml` GitHub Action.

---

## Reporting Bugs

Use the [Bug Report](./.github/ISSUE_TEMPLATE/bug_report.yml) template. Include:

- Watch model + HarmonyOS version
- Phone model + EMUI version
- HMS Core version (Settings → Apps → HMS Core)
- Reproduction steps
- Expected vs actual
- `adb logcat` or DevEco HiLog output (sanitised — no personal data!)

---

## Security Issues

**Do not file a public issue for security bugs.** Email `security@halaprix.dev` (Marian) or use GitHub's [private vulnerability reporting](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing-information-about-vulnerabilities/privately-reporting-a-security-vulnerability). See [SECURITY.md](./SECURITY.md).

---

## License

By contributing, you agree that your contributions are licensed under the [Apache License 2.0](./LICENSE), the same as the project.

---

## Questions?

Open a [Question issue](./.github/ISSUE_TEMPLATE/question.yml) or ask in the relevant PR. Don't DM Marian for support questions — keep the answers in public so others can find them.

Thanks for contributing. 🔋
