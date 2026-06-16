<!--
Thanks for opening a PR! Conventional Commits are required (see CONTRIBUTING.md).
Fill in the template below. The PR title should follow Conventional Commits
format: <type>(<scope>): <subject>
Example: feat(watch): sample batterySOC at 120s interval
-->

## Summary

<!-- One or two sentences: what does this PR do, and why? -->

Closes: <!-- #issue-number, e.g. #42 -->

## What changed

<!-- Bullet list of the user-visible changes -->

- [ ] ...
- [ ] ...

## What didn't change

<!-- Optional: explicitly call out things you intentionally did NOT touch -->

## How to test

<!-- Steps a reviewer can follow to verify. Include the build / run commands. -->

1. ...
2. ...

Expected result: ...

## Battery-budget impact

<!-- Required for any change that touches sampling, listeners, services, or sync. -->

- Watch drain estimate: ...%/24h (was: ...%/24h)
- How measured: (e.g. "smart-reminder" benchmark, manual log, or "not yet measured — will measure in follow-up")
- If unmeasured, plan to measure by: ...

## Checklist

- [ ] My commit messages follow [Conventional Commits](https://www.conventionalcommits.org/)
- [ ] One logical change per commit
- [ ] I read [`AGENTS.md`](./AGENTS.md) (if I'm a coding agent)
- [ ] I read [`CONTRIBUTING.md`](./CONTRIBUTING.md)
- [ ] No secrets, fingerprints, or `agconnect-services.json` content in the diff
- [ ] No references to private IPs, paths, or home-lab infrastructure
- [ ] Tested locally (build / lint / unit tests as applicable)
- [ ] Updated `CHANGELOG.md` (in `## [Unreleased]`)

## Reviewer notes

<!-- Anything the reviewer should pay special attention to. -->
