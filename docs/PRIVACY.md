# Privacy Policy (Skeleton)

> **Status:** Placeholder. Final text lands with `v0.4.0-alpha` before the AppGallery submission. This skeleton exists so the file is in version control from day one and reviewers can challenge the design early.

## Summary (one sentence)

LeakWatch collects **battery telemetry from your paired Huawei watch** on the device, stores it **locally**, and **does not transmit it to any server** unless you explicitly opt in to HMS Analytics.

## Data we handle

| Data | Where it lives | Where it goes | Why |
|---|---|---|---|
| `batterySOC`, `chargingStatus`, `voltage`, `temperature` | Watch RDB (24h rolling) | Phone via HMS Wear Engine P2P (encrypted by HMS) | Core feature |
| `batterySOC` history | Phone Room DB (30d rolling default) | Nowhere (local only) | Chart drawing |
| Crash logs | HMS Crash service | Huawei HMS backend | Stability, no PII |
| Anonymous usage events (if you opt in) | HMS Analytics | Huawei HMS backend | Product decisions |
| AppGallery rating / review | AppGallery | Huawei | Your choice |

## Data we DO NOT handle

- ❌ No account, no email, no name, no phone number.
- ❌ No contacts, calendar, location, photos, microphone access.
- ❌ No advertising ID.
- ❌ No third-party analytics — only HMS Analytics (the HMS equivalent of Firebase Analytics).
- ❌ No background tracking of "where the watch is" or "what the user is doing".

## Permissions requested

- **Watch:** `ohos.permission.RUNNING_TASKS`, `ohos.permission.NOTIFICATION_CONTROLLER` (low-risk, no user data).
- **Phone:** `BLUETOOTH_SCAN` + `BLUETOOTH_CONNECT` (HMS Wear Engine transport), `POST_NOTIFICATIONS` (optional low-battery alerts).

## Your controls

- **Opt out of HMS Analytics** at any time: `Settings → Apps → LeakWatch → Analytics`.
- **Delete all stored data:** `Settings → Apps → LeakWatch → Storage → Clear`. This wipes Room + RDB.
- **Export your data:** `Settings → Apps → LeakWatch → Export to CSV`.

## Children's privacy

LeakWatch does not target children under 13. The AppGallery listing will reflect this.

## Changes to this policy

Material changes will be announced in the release notes of the version that ships them. The previous version is kept in `CHANGELOG.md` and the git history.

## Contact

- Email: `privacy@halaprix.dev`
- GitHub issue: [halaprix/leakwatch/issues](https://github.com/halaprix/leakwatch/issues) (use the `privacy` label)
- For data deletion requests, email with the subject `[LeakWatch] Data Deletion Request`.
