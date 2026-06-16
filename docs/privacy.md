# Privacy for Contributors

> **TL;DR:** This repo is public. Anything you commit will be visible to the world. Don't commit secrets. Don't commit home-lab details. Don't commit private IP addresses. Don't commit Tailscale hostnames or machine IDs. The privacy scan in CI will block your PR if you do.

## Forbidden in commits, issues, PR comments, release notes

| Category | Examples | What to use instead |
|---|---|---|
| Local filesystem paths | `/home/...`, `/Users/...` | Symbolic paths: `repo/`, `public-demo/`, `local-dev/` |
| Private IPs | `192.168.x.x`, `10.x.x.x`, `172.16-31.x.x`, `127.x.x.x`, `169.254.x.x`, Tailscale `100.64-127.x.x` | Generic: `phone`, `watch`, `cloud` |
| Tailscale hostnames | `my-laptop.ts.net` | Generic: `dev-host` |
| GitHub PATs | `ghp_...`, `github_pat_...` | Don't. Period. |
| Other API keys | `sk-...`, `sk-ant-...`, `xai-...` | Don't. Period. |
| Huawei AGC secrets | `agconnect-services.json` content, fingerprints | `agconnect-services.json` in `.gitignore` (already there) |
| Keystores | `*.jks`, `*.keystore`, `*.p12` | `.gitignore` (already there) |
| PEM private keys | `-----BEGIN RSA PRIVATE KEY-----` | Use environment variables / secret managers |
| Machine IDs | `/etc/machine-id` content | Don't. Period. |
| Private repo names | `halaprix/private-thing` | Generic: `companion repo` |

## The scan

`scripts/privacy-scan.sh` runs:
- Locally before you push (recommended)
- In CI on every PR

The script greps all tracked files for forbidden patterns. **If you see `❌ Privacy scan FAILED for pattern: ...`, your PR will be blocked.** Fix it before requesting review.

## Bypassing the scan (don't)

There is no `--no-verify` equivalent. The scan is non-bypassable by design. If you have a legitimate need to include a pattern the scan flags, open an issue first; we'll add an explicit allow-list with a rationale comment.

## Why so strict?

LeakWatch is a battery-frugal HMS battery monitor. The threat model includes:
- Reverse engineers looking for AGC fingerprints to clone the AGC project
- Competitors scraping public AGENTS.md files to map home labs
- Supply chain attacks via typosquatted PATs

Keeping the public surface narrow protects both the project and the maintainer.

## See also

- `SECURITY.md` — security vulnerability reporting
- `docs/PRIVACY.md` — the AppGallery privacy policy (user-facing)
- `AGENTS.md` — full operating rules for coding agents
