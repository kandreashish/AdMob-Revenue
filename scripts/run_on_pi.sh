#!/usr/bin/env bash
# Run or build AdDash directly on Raspberry Pi (ARM64)
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$ROOT_DIR"

echo "=== AdDash Raspberry Pi Runner ==="

# Parse command-line flags
REBUILD_FLAG=false
for arg in "$@"; do
    case "$arg" in
        --rebuild)
            REBUILD_FLAG=true
            ;;
        :*)
            export DISPLAY="$arg"
            ;;
        --screen=*)
            export ADDASH_SCREEN="${arg#*=}"
            ;;
        --screen)
            # Will be handled with next arg if passed
            ;;
    esac
done

if [ "$1" = "--screen" ] && [ -n "$2" ]; then
    export ADDASH_SCREEN="$2"
fi

# Auto-detect display environment if running over SSH
if [ -z "$DISPLAY" ] && [ -z "$WAYLAND_DISPLAY" ]; then
    if [ -S "/tmp/.X11-unix/X0" ]; then
        export DISPLAY=":0"
    elif [ -S "/tmp/.X11-unix/X1" ]; then
        export DISPLAY=":1"
    else
        export DISPLAY=":0"
    fi
fi

if [ -z "$XAUTHORITY" ] && [ -f "$HOME/.Xauthority" ]; then
    export XAUTHORITY="$HOME/.Xauthority"
fi

if [ -z "$XDG_RUNTIME_DIR" ] && [ -d "/run/user/$(id -u)" ]; then
    export XDG_RUNTIME_DIR="/run/user/$(id -u)"
fi

echo "Using DISPLAY=$DISPLAY"
if [ -n "$ADDASH_SCREEN" ]; then
    echo "Target Screen Index: $ADDASH_SCREEN"
fi

# Check Java 17+
if ! command -v java >/dev/null 2>&1; then
    echo "Java not found. Installing Java 17 (default-jre)..."
    sudo apt update
    sudo apt install default-jre -y
fi

DIST_TAR="$ROOT_DIR/desktopApp/build/distributions/addash-1.0.0-raspberry-pi-arm64.tar.gz"

# Force rebuild if requested, if tar is missing, or if source files are newer than the tar
SHOULD_REBUILD=false
if [ "$REBUILD_FLAG" = true ] || [ "$1" = "--rebuild" ] || [ ! -f "$DIST_TAR" ]; then
    SHOULD_REBUILD=true
elif [ -d "$ROOT_DIR/desktopApp/src" ] && [ "$ROOT_DIR/desktopApp/src" -nt "$DIST_TAR" ]; then
    SHOULD_REBUILD=true
fi

if [ "$SHOULD_REBUILD" = true ]; then
    echo "Building fresh standalone bundle..."
    rm -rf "$ROOT_DIR/run_pi"
    chmod +x gradlew
    ./gradlew :desktopApp:packageRaspberryPiTar
fi

echo "Extracting bundle..."
mkdir -p "$ROOT_DIR/run_pi"
tar -xzf "$DIST_TAR" -C "$ROOT_DIR/run_pi"

echo "Launching AdDash (Software Fast rendering)..."
export SKIKO_RENDER_API="${SKIKO_RENDER_API:-SOFTWARE_FAST}"
exec "$ROOT_DIR/run_pi/addash/bin/addash" "$@"
