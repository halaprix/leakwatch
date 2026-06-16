# Battery Budget

> **Status:** Placeholder. Real measurements land in `v0.1.0-alpha.1` after the first end-to-end build.

## Target

| Component | Budget (24h) | Measurement method |
|---|---|---|
| Watch — `BatteryMonitor` singleton | **< 0.5%** | HiLog trace, 7-day soak test on Watch 5 |
| Watch — RDB writes | < 0.1% | SQL trace |
| Watch — Wear Engine P2P sender | < 0.1% | HMS Profiler |
| Phone — Wear Engine receiver | < 0.05% | Android Studio Energy Profiler |
| Phone — Room writes | < 0.02% | Macrobenchmark |
| Phone — Vico rendering | < 0.02% | GPU profiler |
| Phone — HMS Analytics (default) | < 0.01% | Network profiler |

**Total target: < 0.5% on the watch, < 0.1% on the phone** (excluding background OS drain).

## Measurement protocol

1. **Baseline:** factory-reset watch, 100% battery, fresh app install, no user interaction.
2. **Run:** LeakWatch in default config for 7 days.
3. **Read:** battery at start and end, plus HiLog trace of all `batteryInfo.on(...)` events.
4. **Compute:** (battery_start - battery_end) - (control_watch_drain) = leakwatch_drain.
5. **Report:** add the result as a row in the table below.

## Results

| Build | Watch | Soak days | Drain/24h | Pass? | Notes |
|---|---|---|---|---|---|
| _pending v0.1.0-alpha.1_ | — | — | — | — | — |

## Failure modes

If the budget is exceeded, the PR is blocked until either:

1. The cause is identified and fixed (e.g. listener leak, RDB write storm), or
2. The budget is revised with new evidence and a maintainer sign-off.

There is no "we'll fix it later" — the project's whole point is being frugal.
