# PR Flow

LeakWatch uses a single-developer-with-soft-protection PR flow. This doc explains how a change goes from idea to merge.

## Lifecycle

```
1. Issue (or informal chat with maintainer)
   ↓
2. Branch from main: feat/<short-kebab> or fix/<n>-<short>
   ↓
3. One logical change per commit (Conventional Commits)
   ↓
4. Push after every commit
   ↓
5. Open PR against main
   ↓
6. CI must be green (Hygiene job — privacy scan + secrets + Conventional Commits)
   ↓
7. Maintainer (or external reviewer) reviews when available
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
- ❌ `Hygiene` status check failure (Hygiene = privacy scan + secrets + Conventional Commits)
- ❌ Markdown link check failures (warning, not blocker)

## What doesn't get blocked (yet, soft protection)

- No required review (maintainer can self-merge)
- No required CI beyond Hygiene
- No required linear history (squash preferred but not enforced)

Hard protection lands at v1.0.0.

## See also

- `CONTRIBUTING.md` — full contribution guide
- `AGENTS.md` — LLM agent operating rules
