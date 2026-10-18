#!/usr/bin/env bash
set -euo pipefail

if gh release view "v$VERSION" >/dev/null 2>&1; then
  gh release upload "v$VERSION" "$FILE" --clobber
else
  gh release create "v$VERSION" "$FILE" --target "$GITHUB_SHA" \
    --title "AudioShelf $VERSION" --generate-notes
fi
