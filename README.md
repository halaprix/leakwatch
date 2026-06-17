<div align="center">

# LeakWatch

**A battery-frugal monitor for Huawei watches and HMS phones.**

> Watches the watcher. Samples `batteryInfo` on a Huawei smartwatch, ships it over **HMS Wear Engine** to a Huawei phone, and draws the drain curve. Designed so the monitor itself costs **less than 0.5%/24h**.

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](./LICENSE)
[![Status](https://img.shields.io/badge/status-alpha-orange)](#status)
[![Platform](https://img.shields.io/badge/HarmonyOS-5.0-red?logo=huawei)](#platform-support)
[![HMS](https://img.shields.io/badge/HMS-Wear_Engine-ff0000?logo=huawei)](#platform-support)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](./CONTRIBUTING.md)
[![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-yellow.svg)](https://conventionalcommits.org)

</div>

---

## ✨ Why LeakWatch?

AccuBattery, BatteryGuru, BetterBatteryStats — none of them ship on **HMS-only Huawei phones** because there is no GMS, no Play Store, no Firebase. The market gap is real, and a battery monitor that itself drains the battery is a joke. LeakWatch is built on three principles:

1. **Frugal by default.** Hard-mode sampling, batched flush, listener-driven (not polled).
2. **HMS-native.** No GMS, no Firebase. HMS Wear Engine, HMS Analytics, HMS Crash.
3. **Open, on Huawei's terms.** Apache-2.0, lives on the AppGallery when it's ready.

The name is a double meaning: **leak** (battery leak) + **watch** (the wrist thing).

---

## 📊 Platform Support

| Component | Status | Notes |
|---|---|---|
| Huawei Watch 5 / GT 5 Pro (HarmonyOS NEXT) | ✅ Target | ArkTS, `@ohos.batteryInfo` |
| Huawei Watch GT 4 / Watch 4 (HarmonyOS 4) | 🟡 Planned | AOSP-compat layer; verify `batteryInfo` parity |
| Huawei phone with HMS (no GMS) | ✅ Target | Kotlin + Compose + HMS Core |
| Huawei phone with GMS (e.g. P30 global) | ❌ Out of scope | Use AccuBattery instead |
| Non-Huawei Android | ❌ Out of scope | HMS is the whole point |
| iOS | ❌ Out of scope | No HMS path |

---

## 🏗️ Architecture (Preview)

```
┌──────────────────┐    Wear Engine P2P     ┌──────────────────┐
│  Huawei Watch    │ ─────────────────────▶ │  Huawei Phone    │
│  (ArkTS)         │   batched every 10min  │  (Kotlin)        │
│                  │                        │                  │
│  BatteryMonitor  │                        │  WearEngine      │
│  + RDB history   │                        │  Receiver        │
│  + Service Ext.  │                        │  + Room          │
└──────────────────┘                        │  + Vico chart    │
                                           └──────────────────┘
```

- **Watch side** samples `batterySOC`, `chargingStatus`, `batteryTemperature`, `voltage` every **120s** (not 60s) and flushes batches every **10min** over HMS Wear Engine P2P.
- **Phone side** persists, aggregates daily, and draws the drain curve with Vico.
- **No foreground service on the watch.** `Service Extension` + work scheduling only.
- **Total target drain: < 0.5%/24h on the watch**, verified in `docs/BATTERY_BUDGET.md`.

Full architecture: [`docs/ARCHITECTURE.md`](./docs/ARCHITECTURE.md).

---

## 🚧 Status

**Alpha (v0.4.0).** Watch battery monitor + Wear Engine P2P + phone analytics landed. Next milestones:

- [x] `v0.1.0-alpha` — Watch-side `BatteryMonitor` sampling loop (ArkTS) + RDB
- [x] `v0.2.0-alpha` — Phone-side Wear Engine receiver + Room persistence
- [x] `v0.3.0-alpha` — Vico chart, daily aggregation, drain rate
- [x] `v0.4.0-alpha` — Real HMS Wear Engine P2P (v0.4.0-alpha.1 tagged)
- [ ] `v0.5.0-alpha` — Privacy policy + AppGallery submission
- [ ] `v1.0.0` — First public release

See [`CHANGELOG.md`](./CHANGELOG.md) for what landed when.

---

## 🛠️ Tech Stack

| Layer | Choice | Why |
|---|---|---|
| Watch (ArkTS) | `@ohos.batteryInfo` + `RelationalStore` | First-party, no permissions, API 6+ |
| Watch ↔ Phone | HMS Wear Engine 5.0.0.300 | P2P, idiomatic, exposes wearable battery |
| Phone (Kotlin) | 2.0 + Jetpack Compose Material 3 | Modern, Marian-familiar |
| Phone storage | Room 2.6.1 | Reliable, KSP-friendly |
| Phone charts | Vico 2.0 | Compose-native, small footprint |
| HMS analytics | HMS Analytics 6.13 + HMS Crash | Replaces Firebase |
| Build (watch) | Hvigor 5.1.0+ | DevEco Studio standard |
| Build (phone) | Gradle 8.7 + AGP 8.5 | Standard Android toolchain |

---

## 🏁 Getting Started

> ⚠️ **No app code yet.** The `watch/` and `phone/` modules land in the next milestones. This section will grow then.

### Prerequisites

- **DevEco Studio 5.1.0+** (HarmonyOS NEXT) — for the watch module
- **Android Studio Koala+** with HMS Toolkit plugin — for the phone module
- **JDK 17+**, **Node.js 20+** (for tooling)
- **A Huawei Developer account** — to apply for HMS Wear Engine Kit in AppGallery Connect

### Local setup (scaffolding only)

```bash
git clone https://github.com/halaprix/leakwatch
cd leakwatch
# Nothing to build yet. Run `git log` to see the bootstrap.
```

### Contributing

We welcome issues, PRs, and watch reports. Read [`CONTRIBUTING.md`](./CONTRIBUTING.md) first — Conventional Commits are mandatory and one-commit-per-fix is the rule.

---

## 🤝 Code of Conduct

This project follows the [Contributor Covenant v2.1](./CODE_OF_CONDUCT.md). Be excellent to each other.

---

## 🔐 Security & Privacy

See [`SECURITY.md`](./SECURITY.md) for the security policy and [`docs/PRIVACY.md`](./docs/PRIVACY.md) for the AppGallery privacy-policy skeleton. **LeakWatch does not collect personal data, does not phone home beyond HMS Analytics, and does not store anything outside the device unless you opt in.**

---

## 📜 License

[Apache License 2.0](./LICENSE) — consistent with `HMS-Wearable-Example` and friendly to HMS ecosystem contributors.

---

## 💬 Contact

- **Issues:** [github.com/halaprix/leakwatch/issues](https://github.com/halaprix/leakwatch/issues)
- **Marian (maintainer):** [github.com/halaprix](https://github.com/halaprix)
- **HMS reference patterns:** [`ferPrieto/HMS-Wearable-Example`](https://github.com/ferPrieto/HMS-Wearable-Example), [`Explore-In-HMOS-Wearable/smart-reminder`](https://github.com/Explore-In-HMOS-Wearable/smart-reminder)
