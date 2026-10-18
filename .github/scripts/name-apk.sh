#!/usr/bin/env bash
set -euo pipefail

VERSION=$(node -p "require('./package.json').version")
FILE="AudioShelf-$VERSION.apk"

cp android/app/build/outputs/apk/debug/app-debug.apk "$FILE"
{
  echo "version=$VERSION"
  echo "file=$FILE"
} >> "$GITHUB_OUTPUT"
