# Security Policy

## Supported Versions

| Version | Supported |
|---|---|
| `v0.x.x` (alpha/beta/rc) | ✅ Best-effort |
| Latest release on `main` | ✅ |
| Older releases | ❌ No backports |

This project is pre-1.0. Security fixes land on `main` and ship in the next alpha. We do not maintain multiple release branches yet.

---

## Reporting a Vulnerability

**Do not file a public GitHub issue for security bugs.**

Report privately via one of:

1. **GitHub Security Advisories** (preferred): [privately report a vulnerability](https://github.com/halaprix/leakwatch/security/advisories/new)
2. **Email:** `security@halaprix.dev` (PGP key on request)

Please include:

- Watch / phone model and OS version
- App version (`vX.Y.Z`) or commit SHA
- HMS Core version
- Reproduction steps
- A short proof-of-concept (code, log snippet, screenshot — sanitise personal data!)
- Impact assessment: what data is at risk, who can exploit it

You should hear back within **48 hours**. We will:

- Acknowledge receipt
- Triage and assign a severity (Critical / High / Medium / Low)
- Propose a fix timeline
- Credit you in the fix commit (unless you ask to remain anonymous)

---

## Scope

### In scope

- The `watch/` and `phone/` app code (when it lands)
- HMS integration code (Wear Engine client, Room persistence, P2P message handlers)
- The build / CI pipeline (GitHub Actions workflows)
- The `docs/`, `tools/scripts/` content
- Dependency CVEs that affect this project

### Out of scope

- Huawei AppGallery Connect platform vulnerabilities — report to Huawei
- HMS SDK vulnerabilities — report to Huawei via the [Developer Console support](https://developer.huawei.com/consumer/en/support/feedback/)
- Third-party libraries (Vico, Room, etc.) — report upstream
- Vulnerabilities requiring physical access to a rooted watch

---

## Privacy

LeakWatch is a battery monitor. Our privacy commitments are in [`docs/PRIVACY.md`](./docs/PRIVACY.md). In short:

- **No personal data leaves the device** unless you opt in to HMS Analytics.
- **No account, no email, no name** — LeakWatch does not ask who you are.
- **No network calls** beyond HMS Core (Wear Engine P2P, Analytics, Crash).
- **No third-party analytics** — only HMS Analytics (the HMS equivalent of Firebase Analytics).

If you find a privacy bug (e.g. data leaking to a server we did not document), that is a security bug. Report it via the channels above.

---

## Security Best Practices for Contributors

- **Never commit secrets.** No `agconnect-services.json`, no `.jks`, no `.keystore`, no `local.properties` content. CI uses GitHub Secrets.
- **Never log P2P message payloads in production builds.** Wrap logging in `BuildConfig.DEBUG` checks.
- **Sanitise HMS errors** before writing them to Room — they can include device identifiers.
- **Validate all P2P messages** on the receiving side. The watch and phone are paired, but be defensive.
- **Pin HMS SDK versions** in `build.gradle.kts` — no `+` or `latest.release`.

---

## Acknowledgements

Thanks to the open-source security community. We will publish a `SECURITY_HALL_OF_FAME.md` once we have reports to credit (only with reporter consent).
