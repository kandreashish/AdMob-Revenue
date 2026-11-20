#!/usr/bin/env bash
# SolarAd Launcher for Linux / Raspberry Pi

set -e

SCRIPT_DIR="$(cd "$(dirname "$(readlink -f "$0" 2>/dev/null || echo "$0")")" && pwd)"
APP_HOME="$(cd "$SCRIPT_DIR/.." && pwd)"

# Detect Java 17+
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_EXEC="$JAVA_HOME/bin/java"
elif command -v java >/dev/null 2>&1; then
    JAVA_EXEC="java"
else
    echo "Error: Java 17 or higher is required to run SolarAd." >&2
    echo "Install it with: sudo apt install default-jre -y" >&2
    exit 1
fi

# Optimization flags for Raspberry Pi / ARM Linux
JAVA_OPTS="-Xms64m -Xmx512m -Dskiko.renderApi=OPENGL"

exec "$JAVA_EXEC" $JAVA_OPTS -cp "$APP_HOME/lib/*" com.solarad.app.desktop.MainKt "$@"
