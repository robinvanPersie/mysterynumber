#!/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP_DIR="$ROOT/desktopApp/build/compose/binaries/main/app/神秘数字.app"
DMG_DIR="$ROOT/desktopApp/build/compose/binaries/main/dmg"
DMG_PATH="$DMG_DIR/神秘数字-1.0.0.dmg"

test -d "$APP_DIR"
mkdir -p "$DMG_DIR"
rm -f "$DMG_PATH"

hdiutil create \
  -volname "神秘数字" \
  -srcfolder "$APP_DIR" \
  -ov \
  -format UDZO \
  "$DMG_PATH"

echo "Native macOS DMG written to $DMG_PATH"
