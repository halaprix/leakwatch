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
