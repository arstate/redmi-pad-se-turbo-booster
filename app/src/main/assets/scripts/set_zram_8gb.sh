#!/bin/bash
# ==============================================================================
# REDMI PAD SE (xun) - ZRAM 8GB SUITE: DECOMMISSIONED & REPLACED BY PURE ZRAM
# ==============================================================================
# NOTICE: 8GB virtual swap extensions have been DECOMMISSIONED.
# Reason: Allocating an 8GB swap block on 4GB physical LPDDR4X starves the
# active Linux page cache and wears down the 128GB eMMC 5.1 storage.
# This script now safely enforces ZERO virtual swap (persist.miui.extm.enable 0)
# and redirects to pure in-RAM ZRAM compaction.
# ==============================================================================

if command -v adb >/dev/null 2>&1 && [ -z "$ANDROID_ROOT" ]; then
    ADB="adb shell"
else
    ADB=""
fi

echo "[*] Decommissioning eMMC virtual swap extensions..."
$ADB service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.miui.extm.enable 0" s16 "/dev/null" i32 600 > /dev/null 2>&1
$ADB service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.miui.memory_extension 0" s16 "/dev/null" i32 600 > /dev/null 2>&1
$ADB settings put system miui_virtual_mem_status 0
$ADB settings put global miui_virtual_mem_status 0
$ADB settings put global isExternalRamOn 0
$ADB settings put global ram_expand_size 0

echo "[✓] Virtual RAM eMMC extension permanently disabled (persist.miui.extm.enable 0)."
echo "[*] For optimal 4GB LPDDR4X performance, use: sh boost_pure_zram_compaction.sh"
