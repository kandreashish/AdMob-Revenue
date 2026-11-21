#!/usr/bin/env bash
# ==============================================================================
# AdDash Mobile Production Release Build Script (Android & iOS)
# ==============================================================================
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "===================================================================="
echo "🚀 Building AdDash Production Release (Android & iOS)"
echo "===================================================================="

# 1. Run Tests
echo ""
echo "🧪 Running Multiplatform Tests..."
./gradlew :composeApp:allTests

# 2. Build Android Production Release APK
echo ""
echo "📱 Building Android Production Release APK (R8 Minified & Shrunk)..."
./gradlew :androidApp:assembleRelease

# 3. Build Android Production Release App Bundle (Google Play AAB)
echo ""
echo "📦 Building Android Production App Bundle (.aab)..."
./gradlew :androidApp:bundleRelease

# 4. Build iOS Release Framework (Simulator & Device)
echo ""
echo "🍏 Building iOS Release Frameworks..."
./gradlew :composeApp:linkReleaseFrameworkIosSimulatorArm64
./gradlew :composeApp:linkReleaseFrameworkIosArm64

echo ""
echo "===================================================================="
echo "✅ PRODUCTION RELEASE ARTIFACTS GENERATED SUCCESSFULLY!"
echo "===================================================================="
echo ""
echo "📱 Android Release APK:"
ls -lh "$ROOT_DIR/androidApp/build/outputs/apk/release/androidApp-release.apk" || true
echo ""
echo "📦 Android Google Play Bundle (.aab):"
ls -lh "$ROOT_DIR/androidApp/build/outputs/bundle/release/androidApp-release.aab" || true
echo ""
echo "🍏 iOS Release Frameworks:"
echo " • Device (arm64):    $ROOT_DIR/composeApp/build/bin/iosArm64/releaseFramework/ComposeApp.framework"
echo " • Simulator (arm64): $ROOT_DIR/composeApp/build/bin/iosSimulatorArm64/releaseFramework/ComposeApp.framework"
echo "===================================================================="
