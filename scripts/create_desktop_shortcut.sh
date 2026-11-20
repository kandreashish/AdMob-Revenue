#!/usr/bin/env bash
# Creates Desktop and Application Menu shortcuts for AdDash on Raspberry Pi
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

ICON_PATH="$ROOT_DIR/desktopApp/src/jvmMain/resources/icons/icon.png"
LAUNCHER_PATH="$ROOT_DIR/scripts/run_on_pi.sh"

chmod +x "$LAUNCHER_PATH"

DESKTOP_ENTRY="[Desktop Entry]
Version=1.0
Type=Application
Name=AdDash
GenericName=AdMob Analytics
Comment=Live AdMob revenue, eCPM yield, and app performance
Exec=\"$LAUNCHER_PATH\"
Icon=$ICON_PATH
Terminal=false
Categories=Office;Finance;Network;
StartupWMClass=AdDash
"

# 1. Add to Application Menu
APP_MENU_DIR="$HOME/.local/share/applications"
mkdir -p "$APP_MENU_DIR"
echo "$DESKTOP_ENTRY" > "$APP_MENU_DIR/addash.desktop"
chmod +x "$APP_MENU_DIR/addash.desktop"

# 2. Add to Desktop if ~/Desktop directory exists
if [ -d "$HOME/Desktop" ]; then
    echo "$DESKTOP_ENTRY" > "$HOME/Desktop/AdDash.desktop"
    chmod +x "$HOME/Desktop/AdDash.desktop"
fi

if command -v update-desktop-database >/dev/null 2>&1; then
    update-desktop-database "$APP_MENU_DIR" >/dev/null 2>&1 || true
fi

echo "============================================="
echo " AdDash application shortcut installed!     "
echo "============================================="
echo "• Desktop Shortcut:     ~/Desktop/AdDash.desktop"
echo "• App Menu Shortcut:    $APP_MENU_DIR/addash.desktop"
echo ""
echo "You can now double-click 'AdDash' directly on your Raspberry Pi desktop!"
