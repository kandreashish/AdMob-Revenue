#!/usr/bin/env bash
# Run or build AdDash directly on Raspberry Pi (ARM64)
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$ROOT_DIR"

echo "=== AdDash Raspberry Pi Runner ==="

# Check Java 17+
if ! command -v java >/dev/null 2>&1; then
    echo "Java not found. Installing Java 17 (default-jre)..."
    sudo apt update
    sudo apt install default-jre -y
fi

DIST_TAR="$ROOT_DIR/desktopApp/build/distributions/addash-1.0.0-raspberry-pi-arm64.tar.gz"

if [ ! -f "$DIST_TAR" ]; then
    echo "Building standalone bundle..."
    chmod +x gradlew
    ./gradlew :desktopApp:packageRaspberryPiTar
fi

echo "Extracting bundle..."
mkdir -p "$ROOT_DIR/run_pi"
tar -xzf "$DIST_TAR" -C "$ROOT_DIR/run_pi"

echo "Launching AdDash..."
exec "$ROOT_DIR/run_pi/addash/bin/addash" "$@"
