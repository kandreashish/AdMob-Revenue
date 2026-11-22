#!/usr/bin/env bash
# AdDash Raspberry Pi (Linux ARM64) Downloader & Installer
# Usage:
#   curl -sSL https://raw.githubusercontent.com/kandreashish/AdMob-Revenue/main/install_pi.sh | bash
#   or from cloned repository: ./install_pi.sh
set -e

REPO="kandreashish/AdMob-Revenue"
TMP_DIR="/tmp/addash_install_$$"
mkdir -p "$TMP_DIR"
trap 'rm -rf "$TMP_DIR"' EXIT

echo "============================================="
echo "       AdDash Raspberry Pi Installer         "
echo "============================================="

# 1. Architecture Check
ARCH="$(uname -m)"
if [ "$ARCH" != "aarch64" ] && [ "$ARCH" != "arm64" ]; then
    echo "⚠️  Warning: Detected architecture is '$ARCH'."
    echo "AdDash packages are built for Linux ARM64 (aarch64 / arm64)."
    echo "Proceeding anyway..."
fi

# 2. Check and install system prerequisites (Java 17 runtime, curl)
echo "🔍 Checking system dependencies..."
NEED_UPDATE=false

if ! command -v curl >/dev/null 2>&1; then
    echo "Installing curl..."
    sudo apt-get update -y
    sudo apt-get install -y curl
    NEED_UPDATE=false
fi

if ! command -v java >/dev/null 2>&1; then
    echo "📦 Java 17+ runtime not found. Installing default-jre..."
    sudo apt-get update -y
    sudo apt-get install -y default-jre
else
    JAVA_MAJOR="$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)"
    if [ "$JAVA_MAJOR" -lt 17 ]; then
        echo "📦 Installed Java ($JAVA_MAJOR) is older than Java 17. Upgrading default-jre..."
        sudo apt-get update -y
        sudo apt-get install -y default-jre
    fi
fi

# 3. Locate or download the latest .deb package
DEB_PATH=""

# A. Was a deb file provided as an argument or in current folder?
if [ -n "$1" ] && [ -f "$1" ]; then
    DEB_PATH="$1"
    echo "📁 Using specified Debian package: $DEB_PATH"
elif ls desktopApp/build/distributions/addash_*_arm64.deb 1>/dev/null 2>&1; then
    DEB_PATH="$(ls -t desktopApp/build/distributions/addash_*_arm64.deb | head -n 1)"
    echo "📁 Using local repository build: $DEB_PATH"
elif ls "$HOME"/addash_*_arm64.deb 1>/dev/null 2>&1; then
    DEB_PATH="$(ls -t "$HOME"/addash_*_arm64.deb | head -n 1)"
    echo "📁 Found Debian package in home directory: $DEB_PATH"
elif ls addash_*_arm64.deb 1>/dev/null 2>&1; then
    DEB_PATH="$(ls -t addash_*_arm64.deb | head -n 1)"
    echo "📁 Found Debian package in current directory: $DEB_PATH"
fi

# B. Download latest release if no local package found
if [ -z "$DEB_PATH" ]; then
    echo "⬇️  Fetching latest AdDash release from GitHub Actions..."
    TARGET_DEB="$TMP_DIR/addash_latest_arm64.deb"

    DOWNLOAD_SUCCESS=false

    # Strategy 1: GitHub CLI (gh) if installed and authenticated
    if command -v gh >/dev/null 2>&1; then
        echo "Using GitHub CLI to download latest release asset..."
        if gh release download -R "$REPO" --pattern "*arm64.deb" --dir "$TMP_DIR" --clobber 2>/dev/null; then
            DEB_FOUND="$(ls -t "$TMP_DIR"/*arm64.deb 2>/dev/null | head -n 1)"
            if [ -n "$DEB_FOUND" ] && [ -f "$DEB_FOUND" ]; then
                DEB_PATH="$DEB_FOUND"
                DOWNLOAD_SUCCESS=true
            fi
        fi
    fi

    # Strategy 2: GitHub API with GITHUB_TOKEN / GH_TOKEN if available
    if [ "$DOWNLOAD_SUCCESS" = false ] && [ -n "${GITHUB_TOKEN:-$GH_TOKEN}" ]; then
        TOKEN="${GITHUB_TOKEN:-$GH_TOKEN}"
        echo "Querying GitHub API with token..."
        ASSET_ID="$(curl -s -H "Authorization: token $TOKEN" "https://api.github.com/repos/$REPO/releases/latest" | \
            grep -B 2 -A 5 '"name": "addash_.*_arm64\.deb"' | grep '"id":' | head -n 1 | awk '{print $2}' | tr -d ',')"
        if [ -n "$ASSET_ID" ]; then
            echo "Downloading release asset ID $ASSET_ID..."
            curl -sL -H "Authorization: token $TOKEN" \
                 -H "Accept: application/octet-stream" \
                 "https://api.github.com/repos/$REPO/releases/assets/$ASSET_ID" -o "$TARGET_DEB"
            if [ -s "$TARGET_DEB" ]; then
                DEB_PATH="$TARGET_DEB"
                DOWNLOAD_SUCCESS=true
            fi
        fi
    fi

    # Strategy 3: Unauthenticated public release API
    if [ "$DOWNLOAD_SUCCESS" = false ]; then
        LATEST_URL="$(curl -s "https://api.github.com/repos/$REPO/releases/latest" | \
            grep 'browser_download_url' | grep 'arm64\.deb' | head -n 1 | cut -d '"' -f 4)"
        if [ -n "$LATEST_URL" ]; then
            echo "Downloading $LATEST_URL..."
            curl -sL "$LATEST_URL" -o "$TARGET_DEB"
            if [ -s "$TARGET_DEB" ]; then
                DEB_PATH="$TARGET_DEB"
                DOWNLOAD_SUCCESS=true
            fi
        fi
    fi

    # Strategy 4: Local build fallback if in git repo
    if [ "$DOWNLOAD_SUCCESS" = false ]; then
        if [ -f "./gradlew" ]; then
            echo "Building package locally via Gradle..."
            chmod +x gradlew
            ./gradlew :desktopApp:packageRaspberryPiDeb
            DEB_PATH="desktopApp/build/distributions/addash_1.0.0_arm64.deb"
        else
            echo "❌ Could not find or download the Debian package."
            echo "Please pass the package path: ./install_pi.sh /path/to/addash_1.0.0_arm64.deb"
            exit 1
        fi
    fi
fi

# 4. Install Debian package
echo "⚙️  Installing $DEB_PATH..."
sudo dpkg -i "$DEB_PATH" || sudo apt-get --fix-broken install -y

# 5. Setup Desktop icon
if [ -d "$HOME/Desktop" ]; then
    echo "🖥️  Creating desktop launcher shortcut..."
    cp -f /usr/share/applications/addash.desktop "$HOME/Desktop/AdDash.desktop"
    chmod +x "$HOME/Desktop/AdDash.desktop"
    if command -v gio >/dev/null 2>&1; then
        gio set "$HOME/Desktop/AdDash.desktop" metadata::trusted true >/dev/null 2>&1 || true
    fi
fi

echo ""
echo "============================================="
echo "  🎉 AdDash is ready on your Raspberry Pi!   "
echo "============================================="
echo "• Desktop Shortcut:     Double-click 'AdDash' on your desktop"
echo "• Applications Menu:    Applications > Office > AdDash"
echo "• Command Line:         Type 'addash' in any terminal"
echo "============================================="
