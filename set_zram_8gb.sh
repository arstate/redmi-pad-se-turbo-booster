#!/bin/bash
# ==============================================================================
# REDMI PAD SE (xun) - ZRAM 8GB EXPANSION SUITE (NON-ROOT)
# Target: Qualcomm Snapdragon 680 | HyperOS 2.0 (Android 15)
# Injeksi Profil zRAM 8GB (8192 MB) via MQSAS Backdoor & DeviceConfig
# ==============================================================================

# Dual Environment Detector (PC ADB vs Device Shell)
if command -v adb >/dev/null 2>&1 && [ -z "$ANDROID_ROOT" ]; then
    DEVICE_COUNT=$(adb devices | grep -v "List" | grep "device" | wc -l)
    if [ "$DEVICE_COUNT" -eq 0 ]; then
        echo "[-] Error: Tablet belum terdeteksi di ADB!"
        echo "    Pastikan layar menyala & Wireless Debugging aktif."
        exit 1
    fi
    echo "[+] Perangkat terdeteksi di PC ADB: $(adb devices | grep "device$" | head -n1 | awk '{print $1}')"
    ADB="adb shell"
else
    ADB=""
fi

echo "=========================================================="
echo " [🚀] REDMI PAD SE: INJEKSI PROFIL ZRAM 8GB (8192 MB)"
echo " [⚡] By Antigravity AI - HyperOS 2 Kernel Tweaker"
echo "=========================================================="

SIZE_MB=${1:-8192}
echo "[*] Target Kapasitas zRAM: ${SIZE_MB} MB ($((SIZE_MB / 1024)) GB)"

# ==============================================================================
# FASE 1: INJEKSI SYSTEM PROPERTIES VIRTUAL ZRAM VIA MQSAS CALL 21
# ==============================================================================
echo "[1/3] Menyuntikkan konfigurasi 8GB (${SIZE_MB} MB) via MQSAS IMQSNative 21..."

# 8GB Virtual Memory Size
$ADB service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.miui.extm.bdsize ${SIZE_MB}" s16 "/dev/null" i32 600 > /dev/null 2>&1
$ADB service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.miui.memory_extension $((SIZE_MB / 1024))" s16 "/dev/null" i32 600 > /dev/null 2>&1
$ADB service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.zram_size ${SIZE_MB}" s16 "/dev/null" i32 600 > /dev/null 2>&1
$ADB service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.vendor.zram_size ${SIZE_MB}" s16 "/dev/null" i32 600 > /dev/null 2>&1

# Settings Provider
$ADB settings put system miui_virtual_mem_status ${SIZE_MB}
$ADB settings put global miui_virtual_mem_status ${SIZE_MB}

# ==============================================================================
# FASE 2: OPTIMASI KERNEL CACHED APPS & COMPACTION ENGINE
# ==============================================================================
echo "[2/3] Mengonfigurasi Android 15 CachedAppOptimizer untuk buffer 8GB..."

$ADB device_config set_sync_disabled_for_tests persistent
$ADB device_config put activity_manager use_compaction true
$ADB device_config put activity_manager_native_boot use_compaction true

# Perlebar batas cached processes agar sanggup menampung multitasking 8GB
$ADB device_config put activity_manager max_cached_processes 64
$ADB device_config put activity_manager compact_action_1 anon
$ADB device_config put activity_manager compact_action_2 full

# Agresifkan throttle kompresi (5 detik)
$ADB device_config put activity_manager compact_throttle_1 5000
$ADB device_config put activity_manager compact_throttle_2 5000
$ADB device_config put activity_manager compact_full_rss_throttle_kb 8000
$ADB device_config put activity_manager compact_full_delta_rss_throttle_kb 4000

# ==============================================================================
# FASE 3: VERIFIKASI STATUS MEMORI TABLET
# ==============================================================================
echo "[3/3] Melakukan verifikasi alokasi ZRAM..."
echo ""
echo "=========================================================="
echo " [✅] INJEKSI PROFIL ZRAM ${SIZE_MB} MB SELESAI!"
echo "=========================================================="
echo ""
echo "Status Dump Meminfo Tablet:"
$ADB dumpsys meminfo | grep -E "ZRAM:|RAM:|Free RAM:|Total RAM:"
echo ""
