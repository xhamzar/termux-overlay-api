#!/data/data/com.termux/files/usr/bin/bash
# ==============================================================================
# Installer for Termux Overlay CLI
# Installs executable into $PREFIX/bin/overlay
# ==============================================================================

set -e

echo "=== Installing Termux Overlay API CLI ==="

TARGET_DIR="${PREFIX:-/data/data/com.termux/files/usr}/bin"
TARGET_BIN="${TARGET_DIR}/overlay"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

if [ ! -d "$TARGET_DIR" ]; then
    echo "Creating target bin directory: $TARGET_DIR"
    mkdir -p "$TARGET_DIR"
fi

if [ -f "$SCRIPT_DIR/overlay" ]; then
    cp "$SCRIPT_DIR/overlay" "$TARGET_BIN"
else
    echo "Error: overlay CLI script not found in $SCRIPT_DIR"
    exit 1
fi

chmod +x "$TARGET_BIN"

echo "[✓] Successfully installed to $TARGET_BIN"
echo ""
echo "Quick Test:"
echo "  overlay show \"Hello from Termux Overlay!\""
echo "  overlay hide"
echo ""
echo "Run 'overlay help' to see all available commands."
