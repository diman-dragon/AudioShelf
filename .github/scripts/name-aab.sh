#!/usr/bin/env bash
set -euo pipefail

VERSION=$(node -p "require('./package.json').version")
FILE="AudioShelf-$VERSION.aab"

cp android/app/build/outputs/bundle/release/app-release.aab "$FILE"
{
  echo "version=$VERSION"
  echo "file=$FILE"
} >> "$GITHUB_OUTPUT"
