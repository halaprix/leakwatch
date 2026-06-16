# Architecture

> **Status:** Skeleton. Hard-mode sampling rules land with `v0.1.0-alpha.1`. This doc is the canonical reference for "why the monitor drains so little" — every change to sampling, listeners, or sync must be cross-referenced here.

## Goals

1. **Watch-side total drain: < 0.5%/24h** when running in default mode.
2. **Phone-side total drain: < 0.1%/24h** additional (HMS is already running for other reasons).
3. **Sampling resolution: ≥ 2% SoC change per data point on average** — enough to draw a drain curve, sparse enough not to be noise.

## Hard-mode sampling rules

(To be filled in `v0.1.0-alpha.1` — placeholder here so the file exists.)

- **120s interval** for the polling path.
- **Listener-driven** for state changes (charging, low-battery) with **30s cooldown** per source.
- **Batched flush** over HMS Wear Engine every **10 min**, not per-sample.
- **No foreground service** on the watch. `Service Extension` + work scheduling only.
- **Doze-mode aware**: when the watch is in deep sleep, defer sync until wake.

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
