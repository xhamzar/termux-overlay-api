#!/data/data/com.termux/files/usr/bin/bash
# ==============================================================================
# Interactive Demo for Termux Overlay API
# Shows live CPU/uptime monitoring, button prompts, and updates.
# ==============================================================================

if ! command -v overlay >/dev/null 2>&1; then
    echo "Overlay command not found in PATH, using local script ./overlay"
    CMD="./overlay"
else
    CMD="overlay"
fi

echo "1. Displaying Welcome overlay..."
$CMD show "Termux Overlay API Initialized\nConnected via Binder IPC"
sleep 3

echo "2. Streaming live progress updates..."
for i in 10 25 50 75 100; do
    BAR=$(printf "%-${i}s" "#" | tr ' ' '#')
    $CMD update "Compiling project...\n[$i%] $BAR"
    sleep 1
done

echo "3. Showing action button..."
$CMD button "DEPLOY SUCCESS"
sleep 4

echo "4. Adjusting transparency & position..."
$CMD alpha 0.8
$CMD pos 100 300
sleep 2

echo "5. Hiding overlay..."
$CMD hide
echo "Demo completed!"
