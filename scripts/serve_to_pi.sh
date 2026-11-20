#!/usr/bin/env bash
# Serves the AdDash Raspberry Pi bundle over local HTTP so the Pi can download it directly with wget/curl
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
DIST_DIR="$ROOT_DIR/desktopApp/build/distributions"

cd "$ROOT_DIR"

# Ensure package exists
if [ ! -f "$DIST_DIR/addash-1.0.0-raspberry-pi-arm64.tar.gz" ]; then
    echo "📦 Building Raspberry Pi ARM64 distribution packages..."
    ./gradlew :desktopApp:packageRaspberryPiDeb :desktopApp:packageRaspberryPiTar
fi

# Detect Local IP on Mac / Linux
LOCAL_IP=$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null || hostname -I 2>/dev/null | awk '{print $1}' || echo "YOUR_MAC_IP")
PORT=8080

echo "=========================================================="
echo " AdDash Raspberry Pi Transfer Helper"
echo "=========================================================="
echo ""
echo "Run these commands ON YOUR RASPBERRY PI terminal:"
echo ""
echo "----------------------------------------------------------"
echo "# 1. Download and extract the portable bundle:"
echo "wget http://${LOCAL_IP}:${PORT}/addash-1.0.0-raspberry-pi-arm64.tar.gz"
echo "tar -xzf addash-1.0.0-raspberry-pi-arm64.tar.gz"
echo "cd addash/bin"
echo "./addash"
echo ""
echo "# OR 2. Download and install the .deb package:"
echo "wget http://${LOCAL_IP}:${PORT}/addash_1.0.0_arm64.deb"
echo "sudo dpkg -i addash_1.0.0_arm64.deb || sudo apt --fix-broken install -y"
echo "addash"
echo "----------------------------------------------------------"
echo ""
echo "Serving files from: $DIST_DIR"
echo "Listening on http://${LOCAL_IP}:${PORT}/"
echo "Press Ctrl+C to stop the server when done."
echo ""

cd "$DIST_DIR"
python3 -m http.server $PORT
