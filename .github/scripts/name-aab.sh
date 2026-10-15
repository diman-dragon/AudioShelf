#!/usr/bin/env bash
# Копирует подписанный AAB под именем AudioShelf-<версия>.aab (версия из package.json)
# и отдаёт версию и имя файла следующим шагам workflow.
set -euo pipefail

VERSION=$(node -p "require('./package.json').version")
FILE="AudioShelf-$VERSION.aab"

cp android/app/build/outputs/bundle/release/app-release.aab "$FILE"
{
  echo "version=$VERSION"
  echo "file=$FILE"
} >> "$GITHUB_OUTPUT"
