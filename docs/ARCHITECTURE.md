# Architecture

> **Status:** Skeleton. Hard-mode sampling rules land with `v0.1.0-alpha.1`. This doc is the canonical reference for "why the monitor drains so little" — every change to sampling, listeners, or sync must be cross-referenced here.

## Goals

1. **Watch-side total drain: < 0.5%/24h** when running in default mode.
2. **Phone-side total drain: < 0.1%/24h** additional (HMS is already running for other reasons).
3. **Sampling resolution: ≥ 2% SoC change per data point on average** — enough to draw a drain curve, sparse enough not to be noise.

## Hard-mode sampling rules

These rules are **non-negotiable** for v0.1.0-alpha.1 and beyond. Every change to sampling, listeners, or sync must be cross-referenced here and justified against the battery budget.

### Polling path (120s interval)
- `BatteryMonitor.sampleOnce()` calls `batteryInfo.getBatteryInfo()` every **120 seconds** (not 60s, not 30s).
- Each sample is persisted to the local RDB (`leakwatch.db`, table `battery_readings`).
- Older than 24h rows are pruned on every insert (rolling window).

### Listener path (event-driven, 30s cooldown)
- `batteryInfo.on('batteryInfoChange')` fires on state changes (charging plugged/unplugged, low-battery threshold).
- Listener updates `lastSample` and notifies UI listeners **immediately** for low-latency display.
- Listener does **not** persist to RDB — that's the polling path's job (avoids double-writes).
- **30s cooldown** per listener source: if `batteryInfoChange` fires more than once in 30s, subsequent events are dropped (debounce).

### Batched flush (10 min cadence)
- Watch → phone sync happens every **10 minutes** via HMS Wear Engine P2P (not per-sample).
- Batch = all RDB rows since last flush (tracked by `lastFlushTs` in memory).
- If the watch is in deep sleep (doze), flush is deferred until wake (no wake-locks).

### No foreground service
- Watch app does **not** run a foreground service. `BatteryMonitor` lives on the `UIAbility` lifecycle.
- `EntryAbility.onDestroy()` calls `BatteryMonitor.destroy()` to release the native listener and stop the poll timer.
- If the app is killed by the system, the next launch re-initialises from scratch (RDB persists across launches).

### Doze-mode awareness
- When the watch enters deep sleep, the 120s poll timer is suspended by the OS.
- On wake, the timer resumes; the first `sampleOnce()` after wake captures the current state.
- No explicit doze detection needed — HarmonyOS handles timer suspension transparently.

### Battery budget target
- **Watch-side total drain: < 0.5%/24h** when running in default mode (measured in `docs/BATTERY_BUDGET.md`).
- If a change pushes measured drain above 0.5%/24h, it must be reverted or justified with a user-visible benefit.

## Modules

```
leakwatch/
├── watch/     # ArkTS app (HarmonyOS NEXT)
└── phone/     # Kotlin app (HMS, no GMS)
```

## Data flow

```
batteryInfo.on('batteryInfoChange')
  → BatteryMonitor (singleton)
    → RDB (RelationalStore)  [local cache, 24h rolling]
    → WearEngine P2P [batched every 10 min]
      → Phone: WearEngineReceiver
        → Room (BatteryReading entity)
          → Vico chart (Compose UI)
```

## Open questions

- [ ] Do we need a foreground service on the phone to keep the Wear Engine P2P socket warm? (Huawei's behaviour here is inconsistent across EMUI versions.)
- [ ] Is `BatteryCapacityLevel` (categorical) enough for the UI, or do we need raw `batterySOC`?
- [ ] Should we record per-component drain (CPU, screen, sensor) — and if so, is that exposed to 3rd-party devs in HarmonyOS NEXT?

## See also

- `docs/BATTERY_BUDGET.md` — measured drain per build, updated each alpha.
- `docs/PRIVACY.md` — what leaves the device and what doesn't.
