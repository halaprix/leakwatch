#!/usr/bin/env bash
# LeakWatch privacy scanner
# Runs in CI on every PR and locally before commit.
# Fails (exit 1) if any forbidden pattern is found in tracked files.
#
# The allowlist covers files where forbidden patterns are intentionally
# mentioned as examples (privacy docs, CI config, templates, this script).

set -uo pipefail

# Patterns to scan for in tracked files
PATTERNS=(
  # Private filesystem paths
  '/home/[a-z]+/'
  '/Users/[a-z]+/'
  # Private/internal IPs (RFC1918 + loopback + link-local + Tailscale CGNAT)
  '192\.168\.[0-9]+\.[0-9]+'
  '10\.[0-9]+\.[0-9]+\.[0-9]+'
  '172\.(1[6-9]|2[0-9]|3[01])\.[0-9]+\.[0-9]+'
  '127\.[0-9]+\.[0-9]+\.[0-9]+'
  '169\.254\.[0-9]+\.[0-9]+'
  '100\.(6[4-9]|[7-9][0-9]|1[0-1][0-9]|12[0-7])\.[0-9]+\.[0-9]+'
  # GitHub PAT prefixes
  'ghp_[A-Za-z0-9]{20,}'
  'github_pat_[A-Za-z0-9_]{20,}'
  'gho_[A-Za-z0-9]{20,}'
  'ghs_[A-Za-z0-9]{20,}'
  'ghr_[A-Za-z0-9]{20,}'
  # OpenAI / Anthropic / xAI / Mistral
  'sk-[A-Za-z0-9]{20,}'
  'sk-ant-[A-Za-z0-9-]{20,}'
  'xai-[A-Za-z0-9]{20,}'
  # PEM private keys
  'BEGIN (RSA |EC |DSA |OPENSSH |PGP )?PRIVATE KEY'
  # Note: keystore/p12 file EXTENSIONS are checked separately, against
  # the file PATH, not the file content. Grepping a binary .jks for
  # the literal text '.jks$' never matches. See the keystore_files
  # pass below.
  # Tailscale hostnames. Matches any MagicDNS-style subdomain of
  # .ts.net (incl. ts- prefixed device names AND unprefixed names
  # like my-laptop.ts.net). The word boundary keeps it from matching
  # e.g. a partial file path.
  '\b[a-z0-9-]+\.ts\.net\b'
  # agconnect-services.json with content (the bare filename is fine)
  'agconnect-services\.json'
  '\"api_key\"[[:space:]]*:[[:space:]]*\"[A-Za-z0-9]{20,}\"'
)

# Files where some patterns are intentional examples
# (privacy docs, CI config that mentions the scan, issue/PR templates
# that warn against these patterns, this script itself)
ALLOWLIST_FILE_GLOBS=(
  '.github/workflows/ci.yml'
  'docs/privacy.md'
  'docs/PRIVACY.md'
  'docs/pr-flow.md'
  'docs/AGC_SETUP.md'
  'docs/E2E_TESTING.md'
  'AGENTS.md'
  'SECURITY.md'
  'SECURITY_HALL_OF_FAME.md'
  'scripts/privacy-scan.sh'
  'README.md'
  '.github/PULL_REQUEST_TEMPLATE.md'
  '.gitignore'
  '.resources/plan/implementation-plan.md'
)

# Issue templates directory (any file inside is allowed because they
# mention patterns as warnings to bug reporters)
ALLOWLIST_DIRS=(
  '.github/ISSUE_TEMPLATE'
)

# Build / vendored dirs to skip
SKIP_DIRS=(
  '.git'
  'node_modules'
  'build'
  '.gradle'
  'hvigor'
  'oh_modules'
  '.idea'
  'dist'
  'coverage'
  'target'
  '.beads/dolt'
  '.beads/embeddeddolt'
)

is_allowlisted() {
  local file="$1"
  for g in "${ALLOWLIST_FILE_GLOBS[@]}"; do
    if [ "$file" = "$g" ]; then return 0; fi
  done
  for d in "${ALLOWLIST_DIRS[@]}"; do
    if [[ "$file" == "$d"/* ]]; then return 0; fi
  done
  return 1
}

is_skipped() {
  local file="$1"
  for d in "${SKIP_DIRS[@]}"; do
    if [[ "$file" == "$d"/* ]]; then return 0; fi
  done
  return 1
}

# Build the list of files to check
files_to_check=()
while IFS= read -r f; do
  is_skipped "$f" && continue
  is_allowlisted "$f" && continue
  files_to_check+=("$f")
done < <(git ls-files 2>/dev/null)

if [ "${#files_to_check[@]}" -eq 0 ]; then
  echo "✅ Privacy scan: no files to check"
  exit 0
fi

fail=0
checked=0
for pat in "${PATTERNS[@]}"; do
  matches=$(printf '%s\n' "${files_to_check[@]}" | xargs -r grep -lE "$pat" 2>/dev/null || true)
  if [ -n "$matches" ]; then
    echo "❌ Privacy scan FAILED for pattern: $pat"
    echo "$matches" | head -20
    fail=1
  fi
  checked=$((checked + 1))
done

if [ "$fail" -eq 0 ]; then
  total=$(git ls-files | wc -l)
  echo "✅ Privacy scan: no forbidden patterns in ${#files_to_check[@]} checked files ($total total, $checked patterns)"
fi

# --- Keystore / p12 file PATH check ---
# Grepping a binary .jks/.keystore/.p12 for the literal extension text
# never matches. Check the file path itself instead, against all tracked
# files. This catches an accidentally-committed release.jks even if the
# binary content is opaque to grep.
KEYSTORE_EXT_REGEX='\.(jks|keystore|p12)$'
keystore_matches=$(git ls-files 2>/dev/null | grep -E "$KEYSTORE_EXT_REGEX" || true)
if [ -n "$keystore_matches" ]; then
  echo "❌ Privacy scan FAILED: keystore/p12 files in tracked paths"
  echo "$keystore_matches" | head -20
  fail=1
fi

exit "$fail"
