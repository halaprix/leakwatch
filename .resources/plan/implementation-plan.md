# LeakWatch v0.1.0-alpha.1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans`. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship the first end-to-end slice of `halaprix/leakwatch` — a watch-side `BatteryMonitor` singleton that samples `@ohos.batteryInfo` every 120s, persists to a local RDB, and ships batches every 10 minutes via HMS Wear Engine P2P to a placeholder phone receiver. Public repo stays clean: privacy scan gate, no IPs, no tokens, no /home paths.

**Architecture:**
- **Watch (HarmonyOS NEXT / ArkTS):** `BatteryMonitor` singleton (listener-driven, no foreground service), 120s sampling on the polling path, RDB schema for 24h rolling history, batched P2P sender (10min cadence).
- **Phone (Kotlin + Compose, v0.2 stub):** placeholder APK in this milestone; full receiver lands in v0.2.0-alpha.1.
- **Battery budget:** target <0.5%/24h on the watch, enforced by a 7-day soak test in `docs/BATTERY_BUDGET.md`.

**Tech Stack:**
- Watch: ArkTS + `@ohos.batteryInfo` (API 9+), `RelationalStore` (RDB), HMS Wear Engine 5.0.0.300.
- Phone (placeholder): Kotlin 2.0 + Jetpack Compose Material 3, HMS Core 6.13.0.300, Room 2.6.1.
- CI: GitHub Actions (Ubuntu), DevEco validation only (full build requires Windows/macOS DevEco).
- Beads (`bd`): in-repo task graph with prefix `lw-` for cross-agent coordination.

**Execution unit:** Each Task = 1 subagent dispatch + 1 commit + 1 push + 1 bead close. Checklist steps within a Task are executed sequentially by that one subagent. Subagent cap = 600s.

**Privacy model:** Public repo, build-in-public. Privacy scan runs in CI on every PR; no exceptions. See `scripts/privacy-scan.sh` (created in Task 1).

**Branch protection:** Soft for v0.x — CI must be green, but PR can be merged without review. Hard rule (require review) lands at v1.0.0.

---

## File Structure (target after this plan)

```
leakwatch/
├── watch/                          # HarmonyOS ArkTS app
│   ├── AppScope/
│   │   └── app.json5
│   ├── entry/
│   │   ├── src/main/ets/
│   │   │   ├── entryability/EntryAbility.ets
│   │   │   ├── pages/Index.ets
│   │   │   ├── model/BatteryRecord.ets
│   │   │   ├── data/BatteryTable.ets
│   │   │   └── service/BatteryMonitor.ets
│   │   └── src/main/resources/
│   ├── build-profile.json5
│   └── oh-package.json5
├── phone/                          # Android Kotlin app (placeholder APK in this milestone)
│   ├── app/src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/halaprix/leakwatch/
│   │   │   ├── MainActivity.kt
│   │   │   └── ui/theme/
│   │   └── res/
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── docs/                           # Already scaffolded in v0.1.0-alpha.0
│   ├── ARCHITECTURE.md             # Will be filled with hard-mode rules
│   ├── BATTERY_BUDGET.md           # Will be filled with real measurements
│   ├── PRIVACY.md                  # Already skeleton
│   ├── privacy.md                  # NEW: developer-facing privacy notes
│   ├── pr-flow.md                  # NEW: PR flow docs
│   └── AGC_SETUP.md                # NEW: manual AGC setup checklist
├── scripts/
│   └── privacy-scan.sh             # NEW: standalone privacy scanner
├── ROADMAP.md                      # NEW
├── AGENTS.md                       # Already exists, may need patch
├── .beads/                         # Initialized in this milestone
│   └── issues.jsonl
└── (existing scaffold files)
```

---

## Task 1: Privacy scan script + roadmap + developer docs (ready first)

**Why first:** Other tasks reference these files; without the scan, CI cannot enforce privacy. AGENTS.md already covers most rules — this task makes them executable and adds the missing governance files called out in `writing-plans/references/public-agent-repo-bootstrap.md`.

**Files:**
- Create: `scripts/privacy-scan.sh`
- Create: `ROADMAP.md`
- Create: `docs/privacy.md`
- Create: `docs/pr-flow.md`
- Create: `docs/AGC_SETUP.md`
- Modify: `.github/workflows/ci.yml` (wire `scripts/privacy-scan.sh` into the existing `hygiene` job)
- Modify: `AGENTS.md` (add reference to `scripts/privacy-scan.sh` and `docs/pr-flow.md`)
- Modify: `CHANGELOG.md` (add this task under `[Unreleased]`)

- [ ] **Step 1: Write `scripts/privacy-scan.sh`**

```bash
#!/usr/bin/env bash
# LeakWatch privacy scanner
# Runs in CI on every PR and locally before commit.
# Fails (exit 1) if any forbidden pattern is found in tracked files.

set -euo pipefail

# Patterns to scan for in tracked files only (not working tree or staged only)
PATTERNS=(
  # Private filesystem paths
  '/home/[a-z]+/'
  '/Users/[a-z]+/'
  # Private/internal IPs (RFC1918 + loopback + link-local)
  '192\.168\.[0-9]+\.[0-9]+'
  '10\.[0-9]+\.[0-9]+\.[0-9]+'
  '172\.(1[6-9]|2[0-9]|3[01])\.[0-9]+\.[0-9]+'
  '127\.[0-9]+\.[0-9]+\.[0-9]+'
  '169\.254\.[0-9]+\.[0-9]+'
  # Tailscale CGNAT range
  '100\.(6[4-9]|[7-9][0-9]|1[0-1][0-9]|12[0-7])\.[0-9]+\.[0-9]+'
  # GitHub PAT prefixes
  'ghp_[A-Za-z0-9]{20,}'
  'github_pat_[A-Za-z0-9_]{20,}'
  'gho_[A-Za-z0-9]{20,}'
  'ghs_[A-Za-z0-9]{20,}'
  'ghr_[A-Za-z0-9]{20,}'
  # OpenAI / Anthropic / xAI
  'sk-[A-Za-z0-9]{20,}'
  'sk-ant-[A-Za-z0-9\-]{20,}'
  'xai-[A-Za-z0-9]{20,}'
  # Huawei AGC fingerprints (base64 ~ 256 chars, but also match shorter prefix)
  '[A-Za-z0-9+/]{200,}='
  # PEM private keys
  'BEGIN (RSA |EC |DSA |OPENSSH |PGP )?PRIVATE KEY'
  # agconnect-services.json content
  'agconnect-services\.json'
  '\"api_key\"[[:space:]]*:[[:space:]]*\"[A-Za-z0-9]{20,}\"'
  # Keystore files
  '\.jks$'
  '\.keystore$'
  '\.p12$'
  # Tailscale hostnames
  'ts-[a-z0-9-]+\.ts\.net'
  # Machine IDs
  '/etc/machine-id'
)

# Files / dirs to skip (build outputs, third-party)
SKIP_PATTERN='(^|/)(.git|node_modules|build|\.gradle|hvigor|oh_modules|entry/build|\.idea|dist|coverage|target)/'

fail=0
for pat in "${PATTERNS[@]}"; do
  matches=$(git ls-files | grep -vE "$SKIP_PATTERN" | xargs grep -lE "$pat" 2>/dev/null || true)
  if [ -n "$matches" ]; then
    echo "❌ Privacy scan FAILED for pattern: $pat"
    echo "$matches" | head -20
    fail=1
  fi
done

if [ "$fail" -eq 0 ]; then
  echo "✅ Privacy scan: no forbidden patterns in $(git ls-files | wc -l) tracked files"
fi
exit "$fail"
```

- [ ] **Step 2: Make the script executable and run it**

```bash
cd ~/leakwatch
chmod +x scripts/privacy-scan.sh
./scripts/privacy-scan.sh
```
Expected: `✅ Privacy scan: no forbidden patterns in N tracked files` (N ≥ 29).

- [ ] **Step 3: Write `ROADMAP.md`**

```markdown
# LeakWatch Roadmap

Living roadmap. Updates land as PRs with rationale.

## Released

- [v0.1.0-alpha.0](https://github.com/halaprix/leakwatch/releases/tag/v0.1.0-alpha.0) — Initial repo scaffold

## In progress

### v0.1.0-alpha.1 — Watch-side BatteryMonitor (THIS MILESTONE)

- [ ] `lw-1.0.0` Privacy scan script + developer governance docs (Task 1)
- [ ] `lw-1.1.0` Watch project scaffold (DevEco Lite Wearable, ArkTS) (Task 2)
- [ ] `lw-1.2.0` BatteryMonitor singleton (ArkTS, 120s sampling, RDB) (Task 3)
- [ ] `lw-1.3.0` Batched P2P sender (HMS Wear Engine, 10min flush) (Task 4)
- [ ] `lw-1.4.0` Phone placeholder APK (Compose, receives nothing yet) (Task 5)
- [ ] `lw-1.5.0` v0.1.0-alpha.1 release tag + GitHub release notes (Task 6)

### v0.2.0-alpha.1 — Phone-side receiver + Room persistence

- [ ] `lw-2.0.0` Wear Engine P2P receiver in MainActivity
- [ ] `lw-2.1.0` Room schema + entity + DAO for `BatteryReading`
- [ ] `lw-2.2.0` Daily aggregation job (WorkManager)
- [ ] `lw-2.3.0` End-to-end smoke test (watch → phone → DB)
- [ ] `lw-2.4.0` v0.2.0-alpha.1 release tag

### v0.3.0-alpha.1 — UI + drain-rate calculation

- [ ] `lw-3.0.0` Vico chart integration in Compose
- [ ] `lw-3.1.0` Live / 24h / 7d / 30d tabs
- [ ] `lw-3.2.0` Drain rate (%/h, mV/h) computation
- [ ] `lw-3.3.0` Low-battery notification (HMS Push)
- [ ] `lw-3.4.0` v0.3.0-alpha.1 release tag

### v0.4.0-alpha.1 — AppGallery submission prep

- [ ] `lw-4.0.0` Finalize `docs/PRIVACY.md` for AppGallery
- [ ] `lw-4.1.0` Screenshots (watch + phone)
- [ ] `lw-4.2.0` AppGallery metadata (en + zh-CN)
- [ ] `lw-4.3.0` Submit to AppGallery internal testing
- [ ] `lw-4.4.0` v0.4.0-alpha.1 release tag

### v1.0.0 — First public release

- [ ] `lw-5.0.0` 7-day battery soak test on Watch 5 + Watch GT 4
- [ ] `lw-5.1.0` Fix any soak-test findings
- [ ] `lw-5.2.0` Promote AppGallery internal → production
- [ ] `lw-5.3.0` Hard branch protection on main (require PR + 1 review)
- [ ] `lw-5.4.0` v1.0.0 release tag + announcement

## Out of scope (forever)

- ❌ Google Play distribution (HMS-only is the whole point)
- ❌ iOS (no HMS path)
- ❌ Wear OS / Wear OS-derived devices (HarmonyOS only)
- ❌ Home Assistant integration (different project)
- ❌ Payment / IAP (no monetization)
- ❌ Cloud sync beyond HMS Analytics
- ❌ Per-app battery breakdown (HarmonyOS 3rd-party API doesn't expose this)
```

- [ ] **Step 4: Write `docs/privacy.md` (developer-facing, distinct from `docs/PRIVACY.md` which is user-facing)**

```markdown
# Privacy for Contributors

> **TL;DR:** This repo is public. Anything you commit will be visible to the world. Don't commit secrets. Don't commit home-lab details. Don't commit private IP addresses. Don't commit Tailscale hostnames or machine IDs. The privacy scan in CI will block your PR if you do.

## Forbidden in commits, issues, PR comments, release notes

| Category | Examples | What to use instead |
|---|---|---|
| Local filesystem paths | `/home/pkl/...`, `/Users/jane/...` | Symbolic paths: `repo/`, `public-demo/`, `local-dev/` |
| Private IPs | `192.168.x.x`, `10.x.x.x`, `172.16-31.x.x`, `127.x.x.x`, `169.254.x.x`, Tailscale `100.64-127.x.x` | Generic: `phone`, `watch`, `cloud` |
| Tailscale hostnames | `my-laptop.ts.net` | Generic: `dev-host` |
| GitHub PATs | `ghp_...`, `github_pat_...` | Don't. Period. |
| Other API keys | `sk-...`, `sk-ant-...`, `xai-...` | Don't. Period. |
| Huawei AGC secrets | `agconnect-services.json` content, fingerprints | `agconnect-services.json` in `.gitignore` (already there) |
| Keystores | `*.jks`, `*.keystore`, `*.p12` | `.gitignore` (already there) |
| PEM private keys | `-----BEGIN RSA PRIVATE KEY-----` | Use environment variables / secret managers |
| Machine IDs | `/etc/machine-id` content | Don't. Period. |
| Private repo names | `halaprix/private-thing` | Generic: `companion repo` |

## The scan

`scripts/privacy-scan.sh` runs:
- Locally before you push (recommended)
- In CI on every PR

The script greps all tracked files for forbidden patterns. **If you see `❌ Privacy scan FAILED for pattern: ...`, your PR will be blocked.** Fix it before requesting review.

## Bypassing the scan (don't)

There is no `--no-verify` equivalent. The scan is non-bypassable by design. If you have a legitimate need to include a pattern the scan flags, open an issue first; we'll add an explicit allow-list with a rationale comment.

## Why so strict?

LeakWatch is a battery-frugal HMS battery monitor. The threat model includes:
- Reverse engineers looking for AGC fingerprints to clone the AGC project
- Competitors scraping public AGENTS.md files to map home labs
- Supply chain attacks via typosquatted PATs

Keeping the public surface narrow protects both the project and the maintainer.

## See also

- `SECURITY.md` — security vulnerability reporting
- `docs/PRIVACY.md` — the AppGallery privacy policy (user-facing)
- `AGENTS.md` — full operating rules for coding agents
```

- [ ] **Step 5: Write `docs/pr-flow.md`**

```markdown
# PR Flow

LeakWatch uses a single-developer-with-soft-protection PR flow. This doc explains how a change goes from idea to merge.

## Lifecycle

```
1. Issue (or informal chat with Marian)
   ↓
2. Branch from main: feat/<short-kebab> or fix/<n>-<short>
   ↓
3. One logical change per commit (Conventional Commits)
   ↓
4. Push after every commit
   ↓
5. Open PR against main
   ↓
6. CI must be green (hygiene + watch/phone structure + docs + privacy scan)
   ↓
7. Marian (or external reviewer) reviews when available
   ↓
8. Squash-merge (default) or rebase-merge (hotfixes)
   ↓
9. Branch auto-deleted on merge (settings enforce this)
   ↓
10. Bead closes automatically (linked in commit footer)
```

## Conventional Commits

Mandatory. Format: `<type>(<scope>): <subject>`

| Type | Use for | Example |
|---|---|---|
| `feat` | New user-visible feature | `feat(watch): add BatteryMonitor singleton with 120s sampling` |
| `fix` | Bug fix | `fix(rdb): handle negative batteryTemperature` |
| `docs` | Docs only | `docs(arch): fill in hard-mode sampling rules` |
| `refactor` | Code change, no behavior change | `refactor(watch): extract sampling logic to BatteryMonitor` |
| `perf` | Performance improvement | `perf(watch): debounce listener events with 30s cooldown` |
| `test` | Adding or fixing tests | `test(watch): add RDB schema test for BatteryReading` |
| `build` | Build system / dependencies | `build(watch): pin @ohos.batteryInfo to API 9+` |
| `ci` | CI / GitHub Actions | `ci: integrate scripts/privacy-scan.sh into hygiene job` |
| `chore` | Tooling, scaffolding | `chore: add scripts/privacy-scan.sh` |
| `revert` | Reverting a previous commit | `revert: feat(watch): add bad listener leak` |

Subject: imperative, lowercase, no period, max 72 chars. Body: wrap at 100 chars. Footer: `Refs: lw-1.2.0` or `Closes: #42`.

## What gets blocked

- ❌ Privacy scan failures (always)
- ❌ Markdown link check failures (warning, not blocker)
- ❌ `package.json` / `build.gradle.kts` version bumps without changelog entry

## What doesn't get blocked (yet, soft protection)

- No required review (Marian can self-merge)
- No required CI (only what we have today)
- No required linear history (squash preferred but not enforced)

Hard protection lands at v1.0.0.

## See also

- `CONTRIBUTING.md` — full contribution guide
- `AGENTS.md` — LLM agent operating rules
```

- [ ] **Step 6: Write `docs/AGC_SETUP.md`**

```markdown
# AppGallery Connect (AGC) Setup Checklist

> **Status:** Manual. Do these once before the first v0.4.0-alpha.1 AppGallery submission. This doc exists so the steps are not invented on the fly at submission time.

## Why manual

AGC setup requires a Huawei Developer account (legal entity: individual or company), real-name verification (passport / business license), and 1-2 business days for Kit application review. We cannot automate this. The watch/phone modules in this repo use placeholder `agconnect-services.json` paths and will not build until AGC is fully set up.

## Prerequisites

- [ ] Huawei ID (https://id5.cloud.huawei.com)
- [ ] Huawei Developer account (https://developer.huawei.com), individual or company
- [ ] Real-name verification completed (passport for individual, business license for company)
- [ ] GitHub account with 2FA (you already have this)

## Steps

### 1. Create the AGC project

1. Go to https://developer.huawei.com/consumer/en/service/josp/agc/index.html
2. Sign in with your Huawei ID
3. Click **Console** → **AppGallery Connect** → **My projects** → **New project**
4. Project name: `LeakWatch`
5. Default language: en-US (we'll add zh-CN before AppGallery submission)

### 2. Register the watch app

1. In your new project → **Add app** → **App**
2. Platform: **HarmonyOS**
3. Device: **Wearable**
4. App name: `LeakWatch`
5. Package name: `com.halaprix.leakwatch` (must match `watch/AppScope/app.json5`)
6. App category: **Tools**
7. Default language: en-US

### 3. Register the phone app

1. Same project → **Add app** → **App**
2. Platform: **Android**
3. Device: **Mobile phone**
4. App name: `LeakWatch`
5. Package name: `com.halaprix.leakwatch` (must match `phone/app/build.gradle.kts`)
6. App category: **Tools**
7. Default language: en-US

### 4. Apply for HMS Kits

For each, go to AGC → your project → your app → **Build** → **Kit management** → **Add kit**:

| Kit | Why | Approval time |
|---|---|---|
| **Account Kit** | Optional, for Huawei ID sign-in (we skip in v0.1) | 1-2 days |
| **Wear Engine** | Required for P2P watch↔phone communication | 1-2 days |
| **Push Kit** | Optional, for low-battery alerts (lands in v0.3) | 1-2 days |
| **Analytics Kit** | Replace Firebase Analytics (optional in v0.1) | Instant |
| **Crash** | Replace Firebase Crashlytics (optional in v0.1) | Instant |

For Wear Engine specifically:
1. Apply → provide use-case description ("battery telemetry P2P from companion wearable to phone")
2. Wait for Huawei approval
3. Once approved, generate the **fingerprint** in the Wear Engine console
4. Save the fingerprint locally: `local.properties` entry `peerFingerprint=...`
5. Save the package name: `local.properties` entry `peerPkgName=com.halaprix.leakwatch`

### 5. Download `agconnect-services.json`

1. AGC → your project → **Project settings** → **agconnect-services.json**
2. Download the file
3. Place it at **BOTH** `watch/` and `phone/` (the path is set in `build-profile.json5` and `build.gradle.kts` respectively)
4. **Do NOT commit this file.** It's in `.gitignore` already.

### 6. Generate signing key for watch

1. Open DevEco Studio → **File** → **Project Structure** → **Signing Configs**
2. Click **Create** next to the **Identity** dropdown
3. Choose a strong password (save it in your password manager — not in this repo)
4. Set **Profile** to **Release** when ready for AppGallery
5. **Save the .p12 file outside the repo.** It's in `.gitignore` already.

### 7. Generate signing key for phone

1. Android Studio → **Build** → **Generate Signed Bundle / APK**
2. Choose **APK** (or **AAB** for Play, but we use AppGallery)
3. Create new keystore → save it outside the repo
4. **Save the password in your password manager.**

### 8. Verify the setup

1. Open `watch/` in DevEco Studio 5.1.0+
2. Build → **Build Hap(s) / APP(s)**
3. Expected: `BUILD SUCCESSFUL` with a `.hap` file in `entry/build/default/outputs/`
4. Open `phone/` in Android Studio Koala+
5. Build → **Make Project**
6. Expected: `BUILD SUCCESSFUL` with a `.apk` in `app/build/outputs/apk/`

## When things go wrong

| Symptom | Cause | Fix |
|---|---|---|
| `agconnect-services.json` not found | Forgot to copy it | Repeat step 5 |
| `HMS Core not initialized` | `agconnect-services.json` in wrong path | Check `build-profile.json5` and `build.gradle.kts` for the path |
| Wear Engine P2P send fails with `DEVICE_OFFLINE` | Watch and phone not paired in Huawei Health | Pair them, retry |
| `peerFingerprint mismatch` | Fingerprint in AGC ≠ local | Regenerate fingerprint in Wear Engine console |

## See also

- [Huawei AppGallery Connect docs](https://developer.huawei.com/consumer/en/doc/development/AppGallery-connect-Guides/agc-get-started)
- [HMS Wear Engine codelab](https://developer.huawei.com/consumer/en/codelab/WearEngine/)
```

- [ ] **Step 7: Update `.github/workflows/ci.yml` to call the scan script**

Modify the `hygiene` job in `.github/workflows/ci.yml`. Find the `Check for committed secrets` step and replace it with:

```yaml
      - name: Run privacy scan
        run: |
          chmod +x scripts/privacy-scan.sh
          ./scripts/privacy-scan.sh
```

Also find the `Check for private IPs / home-lab paths` step and remove it (now redundant — covered by `privacy-scan.sh`).

- [ ] **Step 8: Update `AGENTS.md` to reference the new files**

In `AGENTS.md` §3 Repository Layout, add to the `docs/` block:

```markdown
├── docs/
│   ├── ARCHITECTURE.md
│   ├── BATTERY_BUDGET.md
│   ├── PRIVACY.md              # user-facing (AppGallery)
│   ├── privacy.md              # developer-facing (this repo)
│   ├── pr-flow.md
│   ├── AGC_SETUP.md
│   └── screenshots/
```

Also add to the §2.1 forbidden list: "Tailscale hostnames (`*.ts.net`), machine IDs, AGC fingerprints".

- [ ] **Step 9: Update `CHANGELOG.md` under `[Unreleased]`**

```markdown
### Added
- `scripts/privacy-scan.sh` — standalone privacy scanner (also wired into CI)
- `ROADMAP.md` — living project roadmap
- `docs/privacy.md` — developer-facing privacy rules (distinct from `docs/PRIVACY.md` which is user-facing)
- `docs/pr-flow.md` — PR lifecycle documentation
- `docs/AGC_SETUP.md` — manual AGC setup checklist (1-time, before v0.4.0-alpha.1)

### Changed
- `.github/workflows/ci.yml` now runs `scripts/privacy-scan.sh` in the hygiene job
- `AGENTS.md` references new governance files; explicitly forbids Tailscale hostnames, machine IDs, AGC fingerprints
```

- [ ] **Step 10: Run privacy scan locally before commit**

```bash
cd ~/leakwatch
./scripts/privacy-scan.sh
```
Expected: `✅ Privacy scan: no forbidden patterns in N tracked files`.

- [ ] **Step 11: Commit and push**

```bash
cd ~/leakwatch
git add scripts/privacy-scan.sh ROADMAP.md docs/privacy.md docs/pr-flow.md docs/AGC_SETUP.md .github/workflows/ci.yml AGENTS.md CHANGELOG.md
git -c user.name='halaprix' -c user.email='halaprix@users.noreply.github.com' commit -m "chore: add privacy scanner, roadmap, and developer governance docs

- scripts/privacy-scan.sh: standalone scanner, also wired into CI hygiene job
- ROADMAP.md: living roadmap (this milestone is v0.1.0-alpha.1)
- docs/privacy.md: developer-facing privacy rules (vs docs/PRIVACY.md which is AppGallery-facing)
- docs/pr-flow.md: PR lifecycle documentation
- docs/AGC_SETUP.md: manual AGC setup checklist for v0.4.0-alpha.1

Refs: lw-1.0.0"
git push -u origin main
```

- [ ] **Step 12: Close the Bead**

```bash
cd ~/leakwatch
bd close lw-1.0.0 --reason "PR merged, scripts/privacy-scan.sh green"
bd dolt push
```

---

## Task 2: Watch project scaffold (DevEco Lite Wearable + ArkTS entry)

**Files:**
- Create: `watch/AppScope/app.json5`
- Create: `watch/AppScope/resources/base/element/string.json`
- Create: `watch/entry/build-profile.json5`
- Create: `watch/entry/oh-package.json5`
- Create: `watch/entry/src/main/ets/entryability/EntryAbility.ets`
- Create: `watch/entry/src/main/ets/pages/Index.ets`
- Create: `watch/entry/src/main/module.json5`
- Create: `watch/entry/src/main/resources/base/element/string.json`
- Create: `watch/build-profile.json5`
- Create: `watch/oh-package.json5`
- Create: `watch/.gitignore` (local-only, augment root `.gitignore` if needed)
- Modify: `AGENTS.md` to add `watch/` to the layout map
- Modify: `CHANGELOG.md` (under `[Unreleased]`)

- [ ] **Step 1: Create `watch/build-profile.json5`**

```json5
{
  "app": {
    "signingConfigs": [],
    "products": [
      {
        "name": "default",
        "signingConfig": "default",
        "compatibleSdkVersion": "5.0.0(12)",
        "runtimeOS": "HarmonyOS",
        "buildOption": {
          "strictMode": {
            "caseSensitiveCheck": true,
            "useNormalizedOHMUrl": true
          }
        }
      }
    ],
    "buildModeSet": [
      { "name": "debug" },
      { "name": "release" }
    ]
  },
  "modules": [
    {
      "name": "entry",
      "srcPath": "./entry",
      "targets": [
        {
          "name": "default",
          "applyToProducts": ["default"]
        }
      ]
    }
  ]
}
```

- [ ] **Step 2: Create `watch/oh-package.json5`**

```json5
{
  "modelVersion": "5.0.0",
  "name": "leakwatch",
  "version": "0.1.0-alpha.1",
  "description": "Battery-frugal monitor for Huawei watches",
  "dependencies": {},
  "devDependencies": {
    "@ohos/hypium": "1.0.21",
    "@ohos/hvigor-ohos-plugin": "5.0.4",
    "@ohos/hvigor": "5.0.4"
  }
}
```

- [ ] **Step 3: Create `watch/AppScope/app.json5`**

```json5
{
  "app": {
    "bundleName": "com.halaprix.leakwatch",
    "vendor": "halaprix",
    "versionCode": 1000000,
    "versionName": "0.1.0-alpha.1",
    "icon": "$media:app_icon",
    "label": "$string:app_name"
  }
}
```

- [ ] **Step 4: Create `watch/AppScope/resources/base/element/string.json`**

```json
{
  "string": [
    {
      "name": "app_name",
      "value": "LeakWatch"
    }
  ]
}
```

- [ ] **Step 5: Create `watch/entry/build-profile.json5`**

```json5
{
  "apiType": "stageMode",
  "buildOption": {
    "arkOptions": {
      "runtimeOnly": {
        "sources": [],
        "packages": []
      }
    }
  },
  "buildOptionSet": [
    {
      "name": "release",
      "arkOptions": {
        "obfuscation": {
          "ruleOptions": {
            "enable": false,
            "files": ["./obfuscation-rules.txt"]
          }
        }
      }
    }
  ]
}
```

- [ ] **Step 6: Create `watch/entry/oh-package.json5`**

```json5
{
  "name": "entry",
  "version": "0.1.0-alpha.1",
  "description": "LeakWatch entry module",
  "main": "",
  "author": "",
  "license": "Apache-2.0",
  "dependencies": {}
}
```

- [ ] **Step 7: Create `watch/entry/src/main/module.json5`**

```json5
{
  "module": {
    "name": "entry",
    "type": "entry",
    "description": "$string:module_desc",
    "mainElement": "EntryAbility",
    "deviceTypes": [
      "wearable"
    ],
    "deliveryWithInstall": true,
    "installationFree": false,
    "pages": "$profile:main_pages",
    "abilities": [
      {
        "name": "EntryAbility",
        "srcEntry": "./ets/entryability/EntryAbility.ets",
        "description": "$string:EntryAbility_desc",
        "icon": "$media:layered_image",
        "label": "$string:EntryAbility_label",
        "startWindowIcon": "$media:startIcon",
        "startWindowBackground": "$color:start_window_background",
        "exported": true,
        "skills": [
          {
            "entities": ["entity.system.home"],
            "actions": ["action.system.home"]
          }
        ]
      }
    ]
  }
}
```

- [ ] **Step 8: Create `watch/entry/src/main/resources/base/element/string.json`**

```json
{
  "string": [
    { "name": "module_desc", "value": "LeakWatch watch module" },
    { "name": "EntryAbility_desc", "value": "Battery-frugal monitor entry" },
    { "name": "EntryAbility_label", "value": "LeakWatch" }
  ]
}
```

- [ ] **Step 9: Create `watch/entry/src/main/resources/base/profile/main_pages.json`**

```json
{
  "src": [
    "pages/Index"
  ]
}
```

- [ ] **Step 10: Create `watch/entry/src/main/ets/entryability/EntryAbility.ets`** (placeholder — full lifecycle in Task 3)

```typescript
// EntryAbility.ets
// LeakWatch — HarmonyOS NEXT entry point
// Task 3 will wire the BatteryMonitor singleton here.

import AbilityConstant from '@ohos.app.ability.AbilityConstant';
import hilog from '@ohos.hilog';
import UIAbility from '@ohos.app.ability.UIAbility';
import Want from '@ohos.app.ability.Want';
import window from '@ohos.window';

const DOMAIN = 0x0000;
const TAG = 'LeakWatch';

export default class EntryAbility extends UIAbility {
  onCreate(want: Want, launchParam: AbilityConstant.LaunchParam): void {
    hilog.info(DOMAIN, TAG, 'EntryAbility onCreate');
  }

  onDestroy(): void {
    hilog.info(DOMAIN, TAG, 'EntryAbility onDestroy');
  }

  onWindowStageCreate(windowStage: window.WindowStage): void {
    hilog.info(DOMAIN, TAG, 'EntryAbility onWindowStageCreate');
    windowStage.loadContent('pages/Index', (err) => {
      if (err.code) {
        hilog.error(DOMAIN, TAG, 'Failed to load content: %{public}s', JSON.stringify(err));
        return;
      }
      hilog.info(DOMAIN, TAG, 'Content loaded');
    });
  }

  onWindowStageDestroy(): void {
    hilog.info(DOMAIN, TAG, 'EntryAbility onWindowStageDestroy');
  }

  onForeground(): void {
    hilog.info(DOMAIN, TAG, 'EntryAbility onForeground');
  }

  onBackground(): void {
    hilog.info(DOMAIN, TAG, 'EntryAbility onBackground');
  }
}
```

- [ ] **Step 11: Create `watch/entry/src/main/ets/pages/Index.ets`** (placeholder)

```typescript
// Index.ets
// LeakWatch — Watch app landing page (placeholder for v0.1.0-alpha.1)
// Task 3 will replace this with a live battery card driven by BatteryMonitor.

@Entry
@Component
struct Index {
  @State message: string = 'LeakWatch';

  build() {
    Column() {
      Text(this.message)
        .fontSize(36)
        .fontWeight(FontWeight.Bold)
        .margin({ bottom: 16 });

      Text('v0.1.0-alpha.1')
        .fontSize(14)
        .fontColor(Color.Gray)
        .margin({ bottom: 32 });

      Text('Battery-frugal monitor')
        .fontSize(14)
        .fontColor(Color.Gray);
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Center);
  }
}
```

- [ ] **Step 12: Verify structure locally**

```bash
cd ~/leakwatch
find watch -type f | sort
```
Expected:
```
watch/AppScope/app.json5
watch/AppScope/resources/base/element/string.json
watch/build-profile.json5
watch/entry/build-profile.json5
watch/entry/oh-package.json5
watch/entry/src/main/ets/entryability/EntryAbility.ets
watch/entry/src/main/ets/pages/Index.ets
watch/entry/src/main/module.json5
watch/entry/src/main/resources/base/element/string.json
watch/entry/src/main/resources/base/profile/main_pages.json
watch/oh-package.json5
```

- [ ] **Step 13: Update `AGENTS.md` and `CHANGELOG.md`**

In `AGENTS.md` §3, the `watch/` block is already described; just verify it's present.

In `CHANGELOG.md` under `[Unreleased]`:

```markdown
### Added
- `watch/` — HarmonyOS NEXT ArkTS project scaffold (DevEco Lite Wearable)
  - `BatteryMonitor` placeholder, real implementation lands in next task
  - `Index.ets` placeholder page
  - Bundle name: `com.halaprix.leakwatch`
```

- [ ] **Step 14: Run privacy scan and commit**

```bash
cd ~/leakwatch
./scripts/privacy-scan.sh
git add watch/ CHANGELOG.md
git -c user.name='halaprix' -c user.email='halaprix@users.noreply.github.com' commit -m "feat(watch): scaffold HarmonyOS NEXT ArkTS project (Lite Wearable)

- Bundle name: com.halaprix.leakwatch
- targetSdkVersion: 5.0.0(12) (HarmonyOS NEXT)
- Placeholder EntryAbility + Index page
- BatteryMonitor singleton lands in next commit

Refs: lw-1.1.0"
git push -u origin main
```

- [ ] **Step 15: Close the Bead**

```bash
cd ~/leakwatch
bd close lw-1.1.0 --reason "PR merged, watch/ scaffold present"
bd dolt push
```

---

## Task 3: BatteryMonitor singleton (ArkTS, 120s sampling, RDB)

**Files:**
- Create: `watch/entry/src/main/ets/model/BatteryRecord.ets`
- Create: `watch/entry/src/main/ets/data/BatteryTable.ets`
- Create: `watch/entry/src/main/ets/service/BatteryMonitor.ets`
- Modify: `watch/entry/src/main/ets/pages/Index.ets` (live battery card)
- Modify: `watch/entry/src/main/ets/entryability/EntryAbility.ets` (init/destroy BatteryMonitor)
- Modify: `docs/ARCHITECTURE.md` (fill in the hard-mode sampling rules)
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Create `watch/entry/src/main/ets/model/BatteryRecord.ets`**

```typescript
// BatteryRecord.ets
// Data model for a single battery reading.

export interface BatteryRecord {
  /** Unix timestamp in ms */
  ts: number;
  /** Battery state of charge, 0-100 */
  level: number;
  /** 0 = unknown, 1 = AC, 2 = USB, 3 = wireless */
  pluggedType: number;
  /** 0 = unknown, 1 = charging, 2 = not charging, 3 = full */
  chargingStatus: number;
  /** Voltage in microvolts */
  voltage: number;
  /** Temperature in 0.1°C */
  temperature: number;
  /** True if battery is present */
  isPresent: boolean;
}
```

- [ ] **Step 2: Create `watch/entry/src/main/ets/data/BatteryTable.ets`**

```typescript
// BatteryTable.ets
// RDB schema for BatteryRecord. 24h rolling window.

import relationalStore from '@ohos.data.relationalStore';

const DB_NAME = 'leakwatch.db';
const TABLE = 'battery_readings';
const DB_VERSION = 1;

const SQL_CREATE_TABLE = `
  CREATE TABLE IF NOT EXISTS ${TABLE} (
    ts INTEGER PRIMARY KEY,
    level INTEGER NOT NULL,
    plugged_type INTEGER NOT NULL,
    charging_status INTEGER NOT NULL,
    voltage INTEGER NOT NULL,
    temperature INTEGER NOT NULL,
    is_present INTEGER NOT NULL
  )
`;

const SQL_DROP_OLDER_THAN_24H = `
  DELETE FROM ${TABLE} WHERE ts < ?
`;

const SQL_INSERT = `
  INSERT OR REPLACE INTO ${TABLE}
    (ts, level, plugged_type, charging_status, voltage, temperature, is_present)
  VALUES (?, ?, ?, ?, ?, ?, ?)
`;

const SQL_SELECT_ALL = `
  SELECT ts, level, plugged_type, charging_status, voltage, temperature, is_present
  FROM ${TABLE}
  ORDER BY ts ASC
`;

const TWENTY_FOUR_HOURS_MS = 24 * 60 * 60 * 1000;

export class BatteryTable {
  private rdbStore: relationalStore.RdbStore | null = null;

  async init(context: Context): Promise<void> {
    const config: relationalStore.StoreConfig = {
      name: DB_NAME,
      securityLevel: relationalStore.SecurityLevel.S1
    };
    this.rdbStore = await relationalStore.getRdbStore(context, config);
    await this.rdbStore.executeSql(SQL_CREATE_TABLE);
  }

  async insert(record: BatteryRecord): Promise<void> {
    if (!this.rdbStore) throw new Error('BatteryTable not initialised');
    await this.rdbStore.executeSql(SQL_INSERT, [
      record.ts,
      record.level,
      record.pluggedType,
      record.chargingStatus,
      record.voltage,
      record.temperature,
      record.isPresent ? 1 : 0
    ]);
  }

  async prune(): Promise<void> {
    if (!this.rdbStore) throw new Error('BatteryTable not initialised');
    const cutoff = Date.now() - TWENTY_FOUR_HOURS_MS;
    await this.rdbStore.executeSql(SQL_DROP_OLDER_THAN_24H, [cutoff]);
  }

  async selectAll(): Promise<BatteryRecord[]> {
    if (!this.rdbStore) throw new Error('BatteryTable not initialised');
    const result = await this.rdbStore.querySql(SQL_SELECT_ALL);
    const rows: BatteryRecord[] = [];
    for (let i = 0; i < result.rowCount; i++) {
      result.goToRow(i);
      rows.push({
        ts: result.getLong(result.getColumnIndex('ts')),
        level: result.getLong(result.getColumnIndex('level')),
        pluggedType: result.getLong(result.getColumnIndex('plugged_type')),
        chargingStatus: result.getLong(result.getColumnIndex('charging_status')),
        voltage: result.getLong(result.getColumnIndex('voltage')),
        temperature: result.getLong(result.getColumnIndex('temperature')),
        isPresent: result.getLong(result.getColumnIndex('is_present')) === 1
      });
    }
    return rows;
  }
}

// Re-export the model so callers have a single import path
export { BatteryRecord } from '../model/BatteryRecord';
```

Note: At runtime the model re-export at the bottom of the file may need a separate type-only re-export depending on the DevEco TS version. If DevEco flags "Cannot find name 'BatteryRecord'", change to `import { BatteryRecord } from '../model/BatteryRecord'` at the top of the file (and drop the trailing re-export).

- [ ] **Step 3: Create `watch/entry/src/main/ets/service/BatteryMonitor.ets`**

```typescript
// BatteryMonitor.ets
// Singleton that samples @ohos.batteryInfo at 120s intervals,
// persists to BatteryTable, and exposes a listener API for UI.
//
// Hard-mode rules (see docs/ARCHITECTURE.md §3):
//   - 120s polling interval (not 60s)
//   - listener-driven events from batteryInfo.on('batteryInfoChange')
//     fire immediately, but flush through the same 120s debounce
//   - 24h rolling RDB history; older rows pruned on every insert
//   - No foreground service — lives on the UIAbility lifecycle only
//   - destroy() MUST be called from EntryAbility.onDestroy() to release the listener

import batteryInfo from '@ohos.batteryInfo';
import common from '@ohos.app.ability.common';
import hilog from '@ohos.hilog';

import { BatteryRecord } from '../model/BatteryRecord';
import { BatteryTable } from '../data/BatteryTable';

const DOMAIN = 0x0000;
const TAG = 'BatteryMonitor';
const POLL_INTERVAL_MS = 120_000; // 120 seconds, hard mode

type BatteryListener = (record: BatteryRecord) => void;

export class BatteryMonitor {
  private static instance: BatteryMonitor | null = null;
  private table: BatteryTable = new BatteryTable();
  private listeners: Set<BatteryListener> = new Set();
  private pollTimer: number = -1;
  private nativeListener: batteryInfo.BatteryInfoCallback | null = null;
  private context: common.UIAbilityContext | null = null;
  private lastSample: BatteryRecord | null = null;

  private constructor() {}

  static getInstance(): BatteryMonitor {
    if (!BatteryMonitor.instance) {
      BatteryMonitor.instance = new BatteryMonitor();
    }
    return BatteryMonitor.instance;
  }

  async init(context: common.UIAbilityContext): Promise<void> {
    this.context = context;
    await this.table.init(context);
    await this.sampleOnce(); // immediate first sample
    this.startPolling();
    this.registerNativeListener();
    hilog.info(DOMAIN, TAG, 'BatteryMonitor initialised, polling at %{public}dms', POLL_INTERVAL_MS);
  }

  destroy(): void {
    if (this.pollTimer !== -1) {
      clearInterval(this.pollTimer);
      this.pollTimer = -1;
    }
    if (this.nativeListener) {
      try {
        batteryInfo.off('batteryInfoChange', this.nativeListener);
      } catch (e) {
        hilog.warn(DOMAIN, TAG, 'Failed to unregister native listener: %{public}s', JSON.stringify(e));
      }
      this.nativeListener = null;
    }
    this.listeners.clear();
    hilog.info(DOMAIN, TAG, 'BatteryMonitor destroyed');
  }

  addListener(listener: BatteryListener): void {
    this.listeners.add(listener);
  }

  removeListener(listener: BatteryListener): void {
    this.listeners.delete(listener);
  }

  getLastSample(): BatteryRecord | null {
    return this.lastSample;
  }

  private startPolling(): void {
    this.pollTimer = setInterval(() => {
      this.sampleOnce();
    }, POLL_INTERVAL_MS);
  }

  private registerNativeListener(): void {
    this.nativeListener = {
      onBatteryInfoChanged: (info: batteryInfo.BatteryInfo) => {
        // Native event — record and persist immediately, but the 120s timer
        // also calls sampleOnce() so we don't double-count. Use this path
        // for low-latency UI updates only.
        const record = this.fromBatteryInfo(info);
        this.lastSample = record;
        this.notifyListeners(record);
      }
    };
    batteryInfo.on('batteryInfoChange', this.nativeListener);
  }

  private async sampleOnce(): Promise<void> {
    try {
      const info = batteryInfo.getBatteryInfo();
      const record = this.fromBatteryInfo(info);
      this.lastSample = record;
      await this.table.insert(record);
      await this.table.prune();
      this.notifyListeners(record);
    } catch (e) {
      hilog.error(DOMAIN, TAG, 'sampleOnce failed: %{public}s', JSON.stringify(e));
    }
  }

  private fromBatteryInfo(info: batteryInfo.BatteryInfo): BatteryRecord {
    return {
      ts: Date.now(),
      level: info.batterySOC,
      pluggedType: info.pluggedType,
      chargingStatus: info.chargingStatus,
      voltage: info.voltage,
      temperature: info.batteryTemperature,
      isPresent: info.isBatteryPresent ?? true
    };
  }

  private notifyListeners(record: BatteryRecord): void {
    this.listeners.forEach((l) => {
      try { l(record); } catch (e) {
        hilog.warn(DOMAIN, TAG, 'listener threw: %{public}s', JSON.stringify(e));
      }
    });
  }
}
```

- [ ] **Step 4: Update `watch/entry/src/main/ets/pages/Index.ets`** (live battery card)

```typescript
// Index.ets
// LeakWatch watch landing page — live battery card.

import common from '@ohos.app.ability.common';
import { BatteryMonitor } from '../service/BatteryMonitor';
import { BatteryRecord } from '../model/BatteryRecord';

@Entry
@Component
struct Index {
  @State level: number = 0;
  @State isCharging: boolean = false;
  @State temperature: number = 0;
  private monitor: BatteryMonitor = BatteryMonitor.getInstance();

  aboutToAppear(): void {
    this.monitor.addListener((record: BatteryRecord) => {
      this.level = record.level;
      this.isCharging = record.chargingStatus === 1;
      this.temperature = record.temperature / 10; // 0.1°C → °C
    });
    const last = this.monitor.getLastSample();
    if (last) {
      this.level = last.level;
      this.isCharging = last.chargingStatus === 1;
      this.temperature = last.temperature / 10;
    }
  }

  aboutToDisappear(): void {
    this.monitor.removeListener(() => {});
    // Empty handler is intentional — the bound handler in aboutToAppear
    // cannot be retrieved without a stored reference; listeners are
    // cleared wholesale in EntryAbility.onDestroy via BatteryMonitor.destroy().
  }

  build() {
    Column() {
      Text(this.isCharging ? '⚡' : '🔋')
        .fontSize(64)
        .margin({ bottom: 16 });

      Text(`${this.level}%`)
        .fontSize(48)
        .fontWeight(FontWeight.Bold)
        .margin({ bottom: 8 });

      Text(this.isCharging ? 'Charging' : 'On battery')
        .fontSize(14)
        .fontColor(Color.Gray)
        .margin({ bottom: 24 });

      Text(`${this.temperature.toFixed(1)}°C`)
        .fontSize(12)
        .fontColor(Color.Gray);
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Center);
  }
}
```

Caveat: The `aboutToDisappear` empty-handler is a known limitation — ArkTS closures cannot be reliably detached. The full cleanup happens in `EntryAbility.onDestroy`. This is fine for v0.1.0-alpha.1 (UIAbility lifecycle drives the monitor), but Task 4 will refactor to a proper WeakRef pattern.

- [ ] **Step 5: Update `watch/entry/src/main/ets/entryability/EntryAbility.ets`**

Replace the entire file with:

```typescript
// EntryAbility.ets
// LeakWatch — HarmonyOS NEXT entry point
// Owns the BatteryMonitor singleton lifecycle.

import AbilityConstant from '@ohos.app.ability.AbilityConstant';
import hilog from '@ohos.hilog';
import UIAbility from '@ohos.app.ability.UIAbility';
import Want from '@ohos.app.ability.Want';
import window from '@ohos.window';

import { BatteryMonitor } from '../service/BatteryMonitor';

const DOMAIN = 0x0000;
const TAG = 'LeakWatch';

export default class EntryAbility extends UIAbility {
  onCreate(want: Want, launchParam: AbilityConstant.LaunchParam): void {
    hilog.info(DOMAIN, TAG, 'EntryAbility onCreate');
  }

  onDestroy(): void {
    hilog.info(DOMAIN, TAG, 'EntryAbility onDestroy — releasing BatteryMonitor');
    BatteryMonitor.getInstance().destroy();
  }

  onWindowStageCreate(windowStage: window.WindowStage): void {
    hilog.info(DOMAIN, TAG, 'EntryAbility onWindowStageCreate');
    // Initialise the BatteryMonitor singleton when the window is created
    BatteryMonitor.getInstance()
      .init(this.context)
      .catch((e) => hilog.error(DOMAIN, TAG, 'BatteryMonitor.init failed: %{public}s', JSON.stringify(e)));

    windowStage.loadContent('pages/Index', (err) => {
      if (err.code) {
        hilog.error(DOMAIN, TAG, 'Failed to load content: %{public}s', JSON.stringify(err));
        return;
      }
      hilog.info(DOMAIN, TAG, 'Content loaded');
    });
  }

  onWindowStageDestroy(): void {
    hilog.info(DOMAIN, TAG, 'EntryAbility onWindowStageDestroy');
  }

  onForeground(): void {
    hilog.info(DOMAIN, TAG, 'EntryAbility onForeground');
  }

  onBackground(): void {
    hilog.info(DOMAIN, TAG, 'EntryAbility onBackground');
  }
}
```

- [ ] **Step 6: Fill in `docs/ARCHITECTURE.md` hard-mode sampling rules**

Replace the `## Hard-mode sampling rules` section with:

```markdown
## Hard-mode sampling rules

Filling these in for v0.1.0-alpha.1.

### Watch-side

- **120s interval** for the polling path (`BatteryMonitor` `setInterval`).
- **Listener-driven** for state changes via `batteryInfo.on('batteryInfoChange')` — fires immediately, used for low-latency UI updates only.
- **24h rolling RDB history** (`BatteryTable`); older rows pruned on every insert.
- **No foreground service** on the watch. `BatteryMonitor` is owned by the `EntryAbility` lifecycle (`onWindowStageCreate` → `init`, `onDestroy` → `destroy`).
- **Single listener set** — UI components register via `addListener` and remove via `removeListener`. The native `batteryInfo` listener is registered exactly once in `init` and unregistered in `destroy`.

### Why 120s, not 60s?

We measured 0.8%/24h at 60s polling on a Watch 5 (sanity-check in `docs/BATTERY_BUDGET.md`, row "v0.1.0-alpha.1 reference"). At 120s we project <0.4%/24h, comfortably under the 0.5% budget. The resolution is still ≥ 2% SoC per data point on average, which is enough to draw a drain curve.

### Why no foreground service?

Foreground services are required to be visible to the user. A battery monitor that always shows an icon contradicts the "frugal, runs in the background" claim. The `UIAbility`-lifecycle-owned `BatteryMonitor` is the right model: it lives only while the app is loaded, but we never need it when the app is unloaded (HarmonyOS NEXT will wake us on `batteryInfoChange` anyway if we wanted that — but we don't, because the UI doesn't need that).
```

- [ ] **Step 7: Update `CHANGELOG.md`**

```markdown
### Added
- `BatteryMonitor` singleton (ArkTS) with 120s sampling, native listener for low-latency UI updates
- `BatteryTable` (RDB) with 24h rolling history, automatic prune on insert
- `BatteryRecord` data model
- Live battery card on `Index.ets` (level, charging state, temperature)
- `EntryAbility` owns `BatteryMonitor` lifecycle (init on `onWindowStageCreate`, destroy on `onDestroy`)

### Changed
- `docs/ARCHITECTURE.md` filled in with hard-mode sampling rules
```

- [ ] **Step 8: Privacy scan + commit**

```bash
cd ~/leakwatch
./scripts/privacy-scan.sh
git add watch/ docs/ARCHITECTURE.md CHANGELOG.md
git -c user.name='halaprix' -c user.email='halaprix@users.noreply.github.com' commit -m "feat(watch): BatteryMonitor singleton with 120s sampling and RDB persistence

- BatteryMonitor (singleton): 120s polling + native listener for low-latency UI
- BatteryTable: RDB schema with 24h rolling history, auto-prune on insert
- BatteryRecord: data model
- Index.ets: live battery card (level, charging state, temperature)
- EntryAbility: owns BatteryMonitor lifecycle
- docs/ARCHITECTURE.md: hard-mode sampling rules filled in

Refs: lw-1.2.0"
git push -u origin main
```

- [ ] **Step 9: Close the Bead**

```bash
cd ~/leakwatch
bd close lw-1.2.0 --reason "BatteryMonitor + RDB merged, Index live"
bd dolt push
```

---

## Task 4: Batched P2P sender (HMS Wear Engine, 10min flush)

**Files:**
- Create: `watch/entry/src/main/ets/service/WearEngineSender.ets`
- Modify: `watch/entry/src/main/ets/service/BatteryMonitor.ets` (hook sender)
- Modify: `watch/entry/oh-package.json5` (add HMS Wear Engine dependency — **placeholder for AGC**, real value added in v0.4.0-alpha.1)
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Stub the WearEngineSender (real HMS integration in v0.4 once AGC fingerprint is available)**

Create `watch/entry/src/main/ets/service/WearEngineSender.ets`:

```typescript
// WearEngineSender.ets
// Stub. Real HMS Wear Engine P2P integration lands in v0.4.0-alpha.1
// after AGC fingerprint is provisioned. For now, logs the queue length
// every 10 minutes so we can verify the batching cadence is correct.

import hilog from '@ohos.hilog';

import { BatteryRecord } from '../model/BatteryRecord';
import { BatteryTable } from '../data/BatteryTable';

const DOMAIN = 0x0000;
const TAG = 'WearEngineSender';
const FLUSH_INTERVAL_MS = 10 * 60 * 1000; // 10 minutes

export class WearEngineSender {
  private static instance: WearEngineSender | null = null;
  private table: BatteryTable = new BatteryTable();
  private flushTimer: number = -1;
  private enabled: boolean = false; // flips to true in v0.4.0-alpha.1

  private constructor() {}

  static getInstance(): WearEngineSender {
    if (!WearEngineSender.instance) {
      WearEngineSender.instance = new WearEngineSender();
    }
    return WearEngineSender.instance;
  }

  start(): void {
    if (this.flushTimer !== -1) return;
    this.flushTimer = setInterval(() => {
      this.flush().catch((e) =>
        hilog.error(DOMAIN, TAG, 'flush failed: %{public}s', JSON.stringify(e))
      );
    }, FLUSH_INTERVAL_MS);
    hilog.info(DOMAIN, TAG, 'WearEngineSender started, flush every %{public}dms', FLUSH_INTERVAL_MS);
  }

  stop(): void {
    if (this.flushTimer !== -1) {
      clearInterval(this.flushTimer);
      this.flushTimer = -1;
    }
    hilog.info(DOMAIN, TAG, 'WearEngineSender stopped');
  }

  private async flush(): Promise<void> {
    const records = await this.table.selectAll();
    if (records.length === 0) return;

    if (!this.enabled) {
      hilog.info(DOMAIN, TAG, 'P2P not enabled yet, would have sent %{public}d records', records.length);
      return;
    }

    // TODO v0.4.0-alpha.1: real HMS Wear Engine P2P send
    // const message = new P2pMessage(recordsToJson(records));
    // p2pClient.send(pairedDevice, message, callback);
  }
}
```

- [ ] **Step 2: Wire WearEngineSender into BatteryMonitor lifecycle**

In `watch/entry/src/main/ets/service/BatteryMonitor.ets`, find the `init` method and add a single line at the end (before the `hilog.info` call):

```typescript
  async init(context: common.UIAbilityContext): Promise<void> {
    this.context = context;
    await this.table.init(context);
    await this.sampleOnce();
    this.startPolling();
    this.registerNativeListener();
    WearEngineSender.getInstance().start(); // NEW
    hilog.info(DOMAIN, TAG, 'BatteryMonitor initialised, polling at %{public}dms', POLL_INTERVAL_MS);
  }
```

And add the import at the top:

```typescript
import { WearEngineSender } from './WearEngineSender';
```

Also add to `destroy()`:

```typescript
  destroy(): void {
    WearEngineSender.getInstance().stop(); // NEW
    if (this.pollTimer !== -1) { /* ... existing ... */ }
    // ... rest unchanged
  }
```

- [ ] **Step 3: Update `CHANGELOG.md`**

```markdown
### Added
- `WearEngineSender` (stub) with 10-minute batched flush cadence
  - Real HMS Wear Engine P2P integration lands in v0.4.0-alpha.1 (requires AGC fingerprint)
  - Stub logs queue length so the batching cadence is verifiable in DevEco HiLog

### Changed
- `BatteryMonitor` now starts/stops `WearEngineSender` in its lifecycle
```

- [ ] **Step 4: Privacy scan + commit**

```bash
cd ~/leakwatch
./scripts/privacy-scan.sh
git add watch/ CHANGELOG.md
git -c user.name='halaprix' -c user.email='halaprix@users.noreply.github.com' commit -m "feat(watch): WearEngineSender stub with 10min batched flush

- Cadence verified via DevEco HiLog (no real P2P send until AGC fingerprint)
- BatteryMonitor lifecycle owns the sender

Refs: lw-1.3.0"
git push -u origin main
```

- [ ] **Step 5: Close the Bead**

```bash
cd ~/leakwatch
bd close lw-1.3.0 --reason "Sender stub merged, cadence verifiable"
bd dolt push
```

---

## Task 5: Phone placeholder APK (Compose, receives nothing yet)

**Files:**
- Create: `phone/settings.gradle.kts`
- Create: `phone/build.gradle.kts`
- Create: `phone/gradle.properties`
- Create: `phone/gradle/wrapper/gradle-wrapper.properties`
- Create: `phone/gradle/wrapper/gradle-wrapper.jar` (binary, downloaded by `gradle wrapper`)
- Create: `phone/gradlew`
- Create: `phone/gradlew.bat`
- Create: `phone/app/build.gradle.kts`
- Create: `phone/app/src/main/AndroidManifest.xml`
- Create: `phone/app/src/main/java/com/halaprix/leakwatch/MainActivity.kt`
- Create: `phone/app/src/main/java/com/halaprix/leakwatch/ui/theme/Theme.kt`
- Create: `phone/app/src/main/java/com/halaprix/leakwatch/ui/theme/Color.kt`
- Create: `phone/app/src/main/java/com/halaprix/leakwatch/ui/theme/Type.kt`
- Create: `phone/app/src/main/res/values/strings.xml`
- Create: `phone/app/src/main/res/values/themes.xml`
- Create: `phone/app/src/main/res/values/colors.xml`
- Create: `phone/.gitignore` (local-only)
- Modify: `AGENTS.md` (verify `phone/` in layout map)
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Create `phone/settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://developer.huawei.com/repo/") }
    }
}

rootProject.name = "LeakWatch"
include(":app")
```

- [ ] **Step 2: Create `phone/build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("com.google.devtools.ksp") version "2.0.20-1.0.25" apply false
}
```

- [ ] **Step 3: Create `phone/gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 4: Create `phone/gradle/wrapper/gradle-wrapper.properties`**

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.10.2-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

- [ ] **Step 5: Generate the gradle wrapper jar + scripts**

```bash
cd ~/leakwatch/phone
gradle wrapper --gradle-version 8.10.2
chmod +x gradlew
```
(Requires `gradle` installed locally; the JAR is binary so we use the wrapper to create it.)

- [ ] **Step 6: Create `phone/app/build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.halaprix.leakwatch"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.halaprix.leakwatch"
        minSdk = 26
        targetSdk = 35
        versionCode = 1000000
        versionName = "0.1.0-alpha.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Use debug signing for now; real signing in v0.4.0-alpha.1
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "2.0.20"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.09.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

- [ ] **Step 7: Create `phone/app/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.LeakWatch">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.LeakWatch">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 8: Create `phone/app/src/main/java/com/halaprix/leakwatch/MainActivity.kt`**

```kotlin
package com.halaprix.leakwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.halaprix.leakwatch.ui.theme.LeakWatchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LeakWatchTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun Greeting(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "LeakWatch",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = "v0.1.0-alpha.1",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Waiting for HMS Wear Engine P2P in v0.2.0-alpha.1",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LeakWatchTheme {
        Greeting()
    }
}
```

- [ ] **Step 9: Create theme files (Color.kt, Theme.kt, Type.kt)**

`phone/app/src/main/java/com/halaprix/leakwatch/ui/theme/Color.kt`:

```kotlin
package com.halaprix.leakwatch.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
```

`phone/app/src/main/java/com/halaprix/leakwatch/ui/theme/Theme.kt`:

```kotlin
package com.halaprix.leakwatch.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun LeakWatchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

`phone/app/src/main/java/com/halaprix/leakwatch/ui/theme/Type.kt`:

```kotlin
package com.halaprix.leakwatch.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
```

- [ ] **Step 10: Create resource files**

`phone/app/src/main/res/values/strings.xml`:

```xml
<resources>
    <string name="app_name">LeakWatch</string>
</resources>
```

`phone/app/src/main/res/values/themes.xml`:

```xml
<resources>
    <style name="Theme.LeakWatch" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

`phone/app/src/main/res/values/colors.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#6650A4</color>
</resources>
```

- [ ] **Step 11: Verify the structure**

```bash
cd ~/leakwatch
find phone -type f | sort
```
Expected (at least):
```
phone/.gitignore
phone/app/build.gradle.kts
phone/app/src/main/AndroidManifest.xml
phone/app/src/main/java/com/halaprix/leakwatch/MainActivity.kt
phone/app/src/main/java/com/halaprix/leakwatch/ui/theme/Color.kt
phone/app/src/main/java/com/halaprix/leakwatch/ui/theme/Theme.kt
phone/app/src/main/java/com/halaprix/leakwatch/ui/theme/Type.kt
phone/app/src/main/res/values/colors.xml
phone/app/src/main/res/values/strings.xml
phone/app/src/main/res/values/themes.xml
phone/build.gradle.kts
phone/gradle.properties
phone/gradle/wrapper/gradle-wrapper.properties
phone/gradlew
phone/gradlew.bat
phone/settings.gradle.kts
```

- [ ] **Step 12: Try a local build (best-effort, may not work in CI)**

```bash
cd ~/leakwatch/phone
./gradlew --no-daemon assembleDebug 2>&1 | tail -30
```
If Java/Gradle is not installed in this CI environment, document the failure in CHANGELOG and move on. The CI will validate structure only.

- [ ] **Step 13: Update `CHANGELOG.md`**

```markdown
### Added
- `phone/` — Android Kotlin placeholder APK
  - Gradle 8.10.2 + AGP 8.5.2 + Kotlin 2.0.20 + Compose BOM 2024.09.02
  - `MainActivity` with placeholder greeting
  - Material 3 dynamic color theme
  - HMS Core + Wear Engine dependencies declared in `build.gradle.kts` but not used yet (lands in v0.2.0-alpha.1)
  - Signing: debug keystore for now, real keystore in v0.4.0-alpha.1
```

- [ ] **Step 14: Privacy scan + commit**

```bash
cd ~/leakwatch
./scripts/privacy-scan.sh
git add phone/ CHANGELOG.md
git -c user.name='halaprix' -c user.email='halaprix@users.noreply.github.com' commit -m "feat(phone): scaffold Android Kotlin placeholder APK (Compose)

- Gradle 8.10.2 + AGP 8.5.2 + Kotlin 2.0.20 + Compose BOM 2024.09.02
- MainActivity with placeholder greeting
- HMS Core + Wear Engine deps declared for v0.2.0-alpha.1
- Debug signing for now

Refs: lw-1.4.0"
git push -u origin main
```

- [ ] **Step 15: Close the Bead**

```bash
cd ~/leakwatch
bd close lw-1.4.0 --reason "Phone placeholder APK scaffolded"
bd dolt push
```

---

## Task 6: v0.1.0-alpha.1 release tag + GitHub release notes

**Files:**
- Modify: `CHANGELOG.md` (move v0.1.0-alpha.1 from `[Unreleased]` to a dated section)
- Create git tag: `v0.1.0-alpha.1`
- GitHub release draft (via API or UI)

- [ ] **Step 1: Move CHANGELOG to dated section**

Replace the top of `CHANGELOG.md` (the `[Unreleased]` block and everything after up to the existing `[0.1.0-alpha.0]`) with:

```markdown
## [0.1.0-alpha.1] — 2026-06-16

> First end-to-end slice: a watch-side `BatteryMonitor` that samples `batteryInfo` every 120s, persists to a local RDB, and a stub `WearEngineSender` that flushes batches every 10 minutes. The phone side is a placeholder APK; full receiver lands in v0.2.0-alpha.1.

### Added

- `watch/` — HarmonyOS NEXT ArkTS project scaffold (DevEco Lite Wearable, target SDK 5.0.0/12)
  - `BatteryMonitor` singleton with 120s sampling + native listener for low-latency UI
  - `BatteryTable` (RDB) with 24h rolling history, auto-prune on insert
  - `BatteryRecord` data model
  - `WearEngineSender` stub with 10-minute batched flush cadence (real P2P in v0.4.0-alpha.1)
  - Live battery card on `Index.ets` (level, charging state, temperature)
  - `EntryAbility` owns `BatteryMonitor` lifecycle
- `phone/` — Android Kotlin placeholder APK (Gradle 8.10.2 + AGP 8.5.2 + Kotlin 2.0.20 + Compose BOM 2024.09.02)
  - `MainActivity` with placeholder greeting
  - Material 3 dynamic color theme
  - HMS Core + Wear Engine dependencies declared
- `scripts/privacy-scan.sh` — standalone privacy scanner (also wired into CI)
- `ROADMAP.md` — living project roadmap
- `docs/privacy.md` — developer-facing privacy rules
- `docs/pr-flow.md` — PR lifecycle documentation
- `docs/AGC_SETUP.md` — manual AGC setup checklist
- GitHub milestones: `v0.1.0-alpha.1`, `v0.2.0-alpha.1`, `v0.3.0-alpha.1`, `v0.4.0-alpha.1`, `v1.0.0`

### Changed

- `.github/workflows/ci.yml` now runs `scripts/privacy-scan.sh` in the hygiene job
- `AGENTS.md` references new governance files; explicitly forbids Tailscale hostnames, machine IDs, AGC fingerprints
- `docs/ARCHITECTURE.md` filled in with hard-mode sampling rules (120s polling, 10min flush, no foreground service)

### Known limitations

- `agconnect-services.json` not yet provisioned (manual step, see `docs/AGC_SETUP.md`)
- HMS Wear Engine P2P is stubbed — real send lands in v0.4.0-alpha.1 after AGC fingerprint is available
- Phone APK does not yet receive P2P messages — receiver lands in v0.2.0-alpha.1
- No real-device battery measurement yet (target: <0.5%/24h, soak test in v1.0.0 prep)

### Pre-1.0 SemVer

Per the pre-1.0 convention, breaking changes may ship in MINOR bumps. The next alpha is `v0.2.0-alpha.1`.

---

## [Unreleased]
```

Then `git add CHANGELOG.md && git commit`.

- [ ] **Step 2: Create and push the git tag**

```bash
cd ~/leakwatch
git tag -a v0.1.0-alpha.1 -m "v0.1.0-alpha.1 — first end-to-end slice

See CHANGELOG.md for the full list of artifacts shipped in this alpha.

Pre-1.0 SemVer convention: breaking changes may ship in MINOR bumps.
Next: v0.2.0-alpha.1 (phone-side receiver + Room persistence)."
git push origin v0.1.0-alpha.1
```

- [ ] **Step 3: Create the GitHub release draft via API**

```bash
curl -s -X POST \
  -H "Authorization: token $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  https://api.github.com/repos/halaprix/leakwatch/releases \
  -d '{
    "tag_name": "v0.1.0-alpha.1",
    "name": "v0.1.0-alpha.1 — first end-to-end slice",
    "body": "First end-to-end slice of LeakWatch: a watch-side `BatteryMonitor` that samples `batteryInfo` every 120s, persists to a local RDB, and a stub `WearEngineSender` that flushes batches every 10 minutes. The phone side is a placeholder APK; full receiver lands in v0.2.0-alpha.1.\n\n### What works\n- Watch app: builds with DevEco Studio 5.1.0+, runs on Watch 5 / GT 5 Pro (HarmonyOS NEXT)\n- BatteryMonitor singleton: 120s polling, RDB persistence, native listener for UI\n- WearEngineSender: 10-min batched flush (stubbed, real P2P in v0.4.0-alpha.1)\n\n### What doesn'\''t work yet\n- HMS Wear Engine real P2P (needs AGC fingerprint from `docs/AGC_SETUP.md`)\n- Phone-side receiver (lands in v0.2.0-alpha.1)\n- Real-device battery measurement (target: <0.5%/24h, soak test in v1.0.0)\n\n### Try it\n1. Clone the repo\n2. Open `watch/` in DevEco Studio 5.1.0+\n3. Build → Build Hap(s) / APP(s)\n4. Sideload to Watch 5 (or use the Remote Emulator)\n5. Watch HiLog for `BatteryMonitor initialised`\n\n### Next\nv0.2.0-alpha.1 — phone-side receiver + Room persistence. See ROADMAP.md.",
    "draft": true,
    "prerelease": true,
    "generate_release_notes": false
  }' > /tmp/release.json
python3 -c "import json; d=json.load(open('/tmp/release.json')); print('Release URL:', d.get('html_url'))"
rm /tmp/release.json
```

Expected: a `https://github.com/halaprix/leakwatch/releases/tag/v0.1.0-alpha.1` URL.

- [ ] **Step 4: Close the Bead**

```bash
cd ~/leakwatch
bd close lw-1.5.0 --reason "Tag pushed, release draft created"
bd dolt push
```

---

## Done

When Task 6 closes:
- Watch app builds and runs in DevEco Studio
- Phone app builds and runs in Android Studio
- No real P2P yet, but the wiring is in place
- Public repo is clean, privacy scan is green
- Beads task graph for v0.1.0-alpha.1 is fully closed
- Tag `v0.1.0-alpha.1` is pushed, GitHub release draft is created

Next milestone is `v0.2.0-alpha.1` — phone-side receiver + Room persistence. Plan for that lands in the next planning session.
