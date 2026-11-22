#!/usr/bin/env bash
# AdDash Launcher for Linux / Raspberry Pi

set -e

SCRIPT_DIR="$(cd "$(dirname "$(readlink -f "$0" 2>/dev/null || echo "$0")")" && pwd)"
APP_HOME="$(cd "$SCRIPT_DIR/.." && pwd)"

# Auto-detect display environment if running over SSH or background session
if [ -z "$DISPLAY" ] && [ -z "$WAYLAND_DISPLAY" ]; then
    if [ -S "/tmp/.X11-unix/X0" ]; then
        export DISPLAY=":0"
    elif [ -S "/tmp/.X11-unix/X1" ]; then
        export DISPLAY=":1"
    else
        export DISPLAY=":0"
    fi
fi

# Load optional user or system environment variables for AdDash
if [ -f "$HOME/.addash.env" ]; then
    set -a
    . "$HOME/.addash.env"
    set +a
elif [ -f "/etc/addash.env" ]; then
    set -a
    . "/etc/addash.env"
    set +a
fi

if [ -z "$XAUTHORITY" ] && [ -f "$HOME/.Xauthority" ]; then
    export XAUTHORITY="$HOME/.Xauthority"
fi

if [ -z "$XDG_RUNTIME_DIR" ] && [ -d "/run/user/$(id -u)" ]; then
    export XDG_RUNTIME_DIR="/run/user/$(id -u)"
fi

# Detect Java 17+
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_EXEC="$JAVA_HOME/bin/java"
elif command -v java >/dev/null 2>&1; then
    JAVA_EXEC="java"
else
    echo "Error: Java 17 or higher is required to run AdDash." >&2
    echo "Install it with: sudo apt install default-jre -y" >&2
    exit 1
fi

# Rendering API: on Raspberry Pi / ARM Linux, default to SOFTWARE_FAST to avoid OpenGL/Mesa driver hangs
RENDER_API="${SKIKO_RENDER_API:-SOFTWARE_FAST}"
JAVA_OPTS="-Xms64m -Xmx512m -Dskiko.renderApi=$RENDER_API"

exec "$JAVA_EXEC" $JAVA_OPTS -cp "$APP_HOME/lib/*" com.lavish.admob.desktop.MainKt "$@"
