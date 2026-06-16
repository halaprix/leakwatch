# End-to-End Testing Guide (v0.4.0-alpha.1)

> **Prerequisites:** Huawei Developer account, AGC project setup, paired watch+phone.

## Overview

v0.4.0-alpha.1 implements **real HMS Wear Engine P2P** communication:
- **Watch** (HarmonyOS NEXT): `BatteryMonitor` → `WearEngineSender` → P2P → phone
- **Phone** (Android/HMS): `WearEngineReceiverService` → Room DB → UI charts

## Quick Start (If AGC Already Set Up)

### 1. Watch Side (HarmonyOS NEXT)

```bash
# Open in DevEco Studio 5.1.0+
cd watch/

# Ensure agconnect-services.json is in watch/ root
# (Download from AGC → Project Settings)

# Build → Run on device
# Expected: BatteryMonitor starts, logs "BatteryMonitor started"
# Every 10 min: WearEngineSender flushes → logs "Sent X records to phone via P2P"
```

### 2. Phone Side (Android/HMS)

```bash
# Open in Android Studio Koala+
cd phone/

# Ensure agconnect-services.json is in phone/app/ root
# (Download from AGC → Project Settings)

# Build → Run on device
# Expected: WearEngineReceiverService starts automatically
# When watch sends data: logs "Received X readings from watch"
# UI shows: Today tab → live battery, History tab → chart, Settings → analytics
```

### 3. Verify P2P Flow

**Watch logcat (HiLog):**
```
BatteryMonitor: BatteryMonitor started
BatteryMonitor: Inserted reading: level=85%, temp=25.0°C
WearEngineSender: WearEngine DataClient initialized
WearEngineSender: Sent 5 records to phone via P2P
```

**Phone logcat:**
```
LeakWatchApp: LeakWatch Application starting
WearEngineReceiverService: WearEngineReceiverService created
WearEngineReceiverService: Received 5 readings from watch
WearEngineReceiver: Inserted 5 readings from watch
```

**Phone UI:**
- Today tab: "Current Battery" card shows latest level
- History tab: Battery drain chart (if ≥1 day of data)
- Settings tab: Analytics (if ≥7 days of data)

---

## AGC Setup (First Time)

See `docs/AGC_SETUP.md` for the full checklist. Quick version:

### Step 1: Create AGC Project

1. Go to https://developer.huawei.com/consumer/en/service/josp/agc/index.html
2. Sign in → **Console** → **AppGallery Connect** → **My projects** → **New project**
3. Project name: `LeakWatch`

### Step 2: Register Apps

**Watch app:**
- Platform: **HarmonyOS**
- Device: **Wearable**
- Package name: `com.halaprix.leakwatch` (must match `watch/AppScope/app.json5`)

**Phone app:**
- Platform: **Android**
- Device: **Mobile phone**
- Package name: `com.halaprix.leakwatch` (must match `phone/app/build.gradle.kts`)

### Step 3: Apply for Wear Engine Kit

1. AGC → your project → your app → **Build** → **Kit management** → **Add kit**
2. Select **Wear Engine**
3. Use-case description: "battery telemetry P2P from companion wearable to phone"
4. Wait for Huawei approval (1-2 business days)

### Step 4: Generate Fingerprint

Once Wear Engine is approved:
1. AGC → Wear Engine console → **Generate fingerprint**
2. Save the fingerprint locally (e.g., in `local.properties` as `peerFingerprint=...`)
3. **Do NOT commit this file** (it's in `.gitignore`)

### Step 5: Download agconnect-services.json

1. AGC → your project → **Project settings** → **agconnect-services.json**
2. Download the file
3. Place it at **BOTH**:
   - `watch/agconnect-services.json` (for HarmonyOS app)
   - `phone/app/agconnect-services.json` (for Android app)
4. **Do NOT commit these files** (they're in `.gitignore`)

### Step 6: Build & Deploy

**Watch:**
```bash
cd watch/
# DevEco Studio → Build → Build Hap(s) / APP(s)
# Install on watch via USB or DevEco device manager
```

**Phone:**
```bash
cd phone/
# Android Studio → Build → Generate Signed Bundle / APK
# Install on phone via USB or ADB
```

---

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `agconnect-services.json` not found | Forgot to copy | Repeat Step 5 |
| `HMS Core not initialized` | Wrong path | Check `build-profile.json5` (watch) or `build.gradle.kts` (phone) |
| Wear Engine P2P send fails with `DEVICE_OFFLINE` | Watch and phone not paired | Pair them in Huawei Health app, retry |
| `peerFingerprint mismatch` | Fingerprint in AGC ≠ local | Regenerate fingerprint in Wear Engine console |
| Phone doesn't receive data | WearEngineReceiverService not running | Check AndroidManifest.xml, ensure service is declared |
| Watch logs "DataClient not initialized" | Wear Engine Kit not approved | Wait for Huawei approval (1-2 days) |
| Phone logs "battery_readings key missing" | Watch sending wrong format | Check WearEngineSender.ets serialization |

---

## Data Format

**Watch → Phone P2P payload:**

```json
{
  "battery_readings": [
    {
      "ts": 1718524800000,
      "level": 85,
      "pluggedType": 0,
      "chargingStatus": 2,
      "voltage": 4100000,
      "temperature": 250,
      "isPresent": true
    }
  ]
}
```

**Phone deserialization:** `BatteryReadingSerializer.deserialize(ByteArray)` → `List<BatteryReading>`

---

## Battery Budget

**Watch:**
- BatteryMonitor: 120s polling → ~0.3%/day
- WearEngineSender: 10min flush → ~0.1%/day
- Total: **<0.5%/day** (target met)

**Phone:**
- WearEngineReceiverService: listener-driven, no polling → negligible
- DailyAggregationWorker: once/day at midnight → negligible
- Total: **<0.1%/day**

---

## Next Steps

After successful e2e testing:
- **v0.5.0-alpha.1**: Low-battery alerts (Push Kit)
- **v1.0.0**: AppGallery release candidate

---

## See Also

- `docs/AGC_SETUP.md` — full AGC setup checklist
- `docs/ARCHITECTURE.md` — hard-mode sampling rules
- `docs/BATTERY_BUDGET.md` — measured drain budget per build
