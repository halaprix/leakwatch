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
