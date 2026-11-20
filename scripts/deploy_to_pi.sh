#!/usr/bin/env bash
# Deploy and run AdDash on Raspberry Pi via SSH
set -e

PI_TARGET="${1:-pi@raspberrypi.local}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "=== AdDash Raspberry Pi Deployer ==="
echo "Target: $PI_TARGET"
echo ""

# Ensure the distribution packages are built
DIST_DIR="$ROOT_DIR/desktopApp/build/distributions"
DEB_FILE="$DIST_DIR/addash_1.0.0_arm64.deb"
TAR_FILE="$DIST_DIR/addash-1.0.0-raspberry-pi-arm64.tar.gz"

if [ ! -f "$DEB_FILE" ] || [ ! -f "$TAR_FILE" ]; then
    echo "📦 Building Raspberry Pi ARM64 distribution packages..."
    cd "$ROOT_DIR"
    ./gradlew :desktopApp:packageRaspberryPiDeb :desktopApp:packageRaspberryPiTar
fi

echo "🚀 Transferring package to $PI_TARGET..."
scp "$DEB_FILE" "$PI_TARGET:~/"

echo "🔧 Installing AdDash and verifying Java on $PI_TARGET..."
ssh -t "$PI_TARGET" bash -s << 'EOF'
    set -e
    echo "Checking Java 17+..."
    if ! command -v java >/dev/null 2>&1 || [ "$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)" -lt 17 ]; then
        echo "Installing default-jre (Java 17+)..."
        sudo apt update -y
        sudo apt install -y default-jre
    fi

    echo "Installing AdDash debian package..."
    sudo dpkg -i ~/addash_1.0.0_arm64.deb || sudo apt --fix-broken install -y

    if [ -d "$HOME/Desktop" ]; then
        cp -f /usr/share/applications/addash.desktop "$HOME/Desktop/AdDash.desktop"
        chmod +x "$HOME/Desktop/AdDash.desktop"
        command -v gio >/dev/null 2>&1 && gio set "$HOME/Desktop/AdDash.desktop" metadata::trusted true >/dev/null 2>&1 || true
    fi

    echo "✅ AdDash successfully installed on Raspberry Pi!"
    echo "Double click 'AdDash' on your desktop, select from Applications Menu, or run:"
    echo "  DISPLAY=:0 addash &"
EOF

echo ""
echo "🎉 Deployment complete! You can also find AdDash in the Raspberry Pi desktop app menu."
