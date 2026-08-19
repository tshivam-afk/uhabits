#!/usr/bin/env bash
# Installs the CI workflow into the path GitHub Actions actually reads.
set -euo pipefail
cd "$(dirname "$0")/../.."
mkdir -p .github/workflows
cp tools/ci/android.yml .github/workflows/android.yml
echo "Installed .github/workflows/android.yml"
