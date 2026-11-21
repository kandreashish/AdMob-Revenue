#!/usr/bin/env bash
# ==============================================================================
# Helper to generate an Android Production Release Keystore
# ==============================================================================
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KEYSTORE_PATH="${1:-"$ROOT_DIR/release.keystore"}"
KEY_ALIAS="${2:-"addash_key"}"

echo "===================================================================="
echo "🔑 Generating Android Production Release Keystore"
echo "Keystore Path: $KEYSTORE_PATH"
echo "Key Alias:     $KEY_ALIAS"
echo "===================================================================="

if [ -f "$KEYSTORE_PATH" ]; then
    echo "⚠️  Keystore file already exists at $KEYSTORE_PATH. Aborting to avoid overwrite."
    exit 1
fi

keytool -genkeypair -v \
    -keystore "$KEYSTORE_PATH" \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000

echo ""
echo "✅ Keystore created at $KEYSTORE_PATH"
echo ""
echo "To configure release signing, add the following to local.properties or your CI environment variables:"
echo "--------------------------------------------------------------------"
echo "release.keystore.path=$KEYSTORE_PATH"
echo "release.keystore.password=<your_keystore_password>"
echo "release.key.alias=$KEY_ALIAS"
echo "release.key.password=<your_key_password>"
echo "--------------------------------------------------------------------"
