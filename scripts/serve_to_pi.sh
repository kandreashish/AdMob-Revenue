#!/usr/bin/env bash
# Serves the AdDash Raspberry Pi bundle over local HTTP so the Pi can download it directly with wget/curl
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
DIST_DIR="$ROOT_DIR/desktopApp/build/distributions"

cd "$ROOT_DIR"

# Ensure package exists
TAR_FILE="$(ls -t "$DIST_DIR"/addash-*-raspberry-pi-arm64.tar.gz 2>/dev/null | head -n 1 || true)"
DEB_FILE="$(ls -t "$DIST_DIR"/addash_*_arm64.deb 2>/dev/null | head -n 1 || true)"

if [ -z "$TAR_FILE" ] || [ ! -f "$TAR_FILE" ]; then
    echo "📦 Building Raspberry Pi ARM64 distribution packages..."
    ./gradlew :desktopApp:packageRaspberryPiDeb :desktopApp:packageRaspberryPiTar
    TAR_FILE="$(ls -t "$DIST_DIR"/addash-*-raspberry-pi-arm64.tar.gz | head -n 1)"
    DEB_FILE="$(ls -t "$DIST_DIR"/addash_*_arm64.deb | head -n 1)"
fi

TAR_NAME="$(basename "$TAR_FILE")"
DEB_NAME="$(basename "$DEB_FILE")"

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
echo "wget http://${LOCAL_IP}:${PORT}/${TAR_NAME}"
echo "tar -xzf ${TAR_NAME}"
echo "cd addash/bin"
echo "./addash"
echo ""
echo "# OR 2. Download and install the .deb package:"
echo "wget http://${LOCAL_IP}:${PORT}/${DEB_NAME}"
echo "sudo dpkg -i ${DEB_NAME} || sudo apt --fix-broken install -y"
echo "addash"
echo "----------------------------------------------------------"
echo ""
echo "Serving files from: $DIST_DIR"
echo "Listening on http://${LOCAL_IP}:${PORT}/"
echo "Press Ctrl+C to stop the server when done."
echo ""

cd "$DIST_DIR"
python3 -m http.server $PORT
