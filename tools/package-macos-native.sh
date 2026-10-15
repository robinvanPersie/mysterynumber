#!/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP_DIR="$ROOT/desktopApp/build/compose/binaries/main/app/神秘数字.app"
BUILD_DIR="$ROOT/desktopApp/build/native-macos"
BIN="$BUILD_DIR/神秘数字"

mkdir -p "$BUILD_DIR"
swiftc "$ROOT/macosApp/main.swift" "$ROOT/macosApp/MarketData.swift" \
  -parse-as-library -o "$BIN" -framework AppKit -framework CoreFoundation -framework SwiftUI

rm -rf "$APP_DIR"
mkdir -p "$APP_DIR/Contents/MacOS" "$APP_DIR/Contents/Resources"
cp "$BIN" "$APP_DIR/Contents/MacOS/神秘数字"
cp "$ROOT/desktopApp/src/main/resources/icons/mystery-numbers.icns" "$APP_DIR/Contents/Resources/神秘数字.icns"

cat > "$APP_DIR/Contents/Info.plist" <<'PLIST'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
    <key>CFBundleDisplayName</key><string>神秘数字</string>
    <key>CFBundleExecutable</key><string>神秘数字</string>
    <key>CFBundleIdentifier</key><string>com.mysterynumbers</string>
    <key>CFBundleName</key><string>神秘数字</string>
    <key>CFBundleIconFile</key><string>神秘数字.icns</string>
    <key>CFBundlePackageType</key><string>APPL</string>
    <key>LSUIElement</key><true/>
    <key>LSMinimumSystemVersion</key><string>13.0</string>
    <key>NSHighResolutionCapable</key><true/>
</dict></plist>
PLIST

codesign --force --deep --sign - "$APP_DIR" >/dev/null 2>&1 || true
echo "Native macOS app written to $APP_DIR"
