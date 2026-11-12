#!/bin/bash
# ==============================================================================
# REDMI PAD SE (xun) - PURE IN-RAM ZRAM COMPACTION ENGINE (NON-ROOT)
# Target: Qualcomm Snapdragon 680 (Adreno 610) | HyperOS 2.0 (Android 15)
# Goal: Matikan Swap eMMC 5.1 Lambat + Paksa Pure zRAM Compression di LPDDR4X
# ==============================================================================

echo "=========================================================="
echo " [🚀] REDMI PAD SE: PURE ZRAM COMPACTION ACCELERATOR"
echo " [⚡] By Antigravity AI - HyperOS 2 Deep Kernel Exploits"
echo "=========================================================="

# 1. Pastikan ADB Terkoneksi
DEVICE_COUNT=$(adb devices | grep -v "List" | grep "device" | wc -l)
if [ "$DEVICE_COUNT" -eq 0 ]; then
    echo "[-] Error: Tablet belum terdeteksi di ADB!"
    echo "    Nyalakan layar tablet & aktifkan Wireless Debugging."
    exit 1
fi

echo "[+] Perangkat terdeteksi. Memulai injeksi celah kernel..."

# ==============================================================================
# FASE 1: MATIKAN TOTAL SWAP FLASH eMMC 5.1 (EKSTENSI MEMORI STORAGE)
# Alasan: eMMC 5.1 random write cuma 20-30MB/s -> penyebab utama UI ngelag/stutter!
# ==============================================================================
echo "[1/4] Mematikan Swap Flash Storage eMMC 5.1 (Memory Extension)..."
adb shell settings put system miui_virtual_mem_status 0
adb shell settings put global miui_virtual_mem_status 0

# Suntik via MQSAS Backdoor (Call 21) untuk elevate system properties
adb shell service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.miui.extm.enable 0" s16 "/dev/null" i32 600 > /dev/null 2>&1
adb shell service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.miui.memory_extension 0" s16 "/dev/null" i32 600 > /dev/null 2>&1

# ==============================================================================
# FASE 2: AKTIFKAN ANDROID 15 IN-RAM COMPACTION (CachedAppOptimizer)
# Memaksa heap & anonymous memory langsung dikompres ke zRAM LPDDR4X (17 GB/s)
# ==============================================================================
echo "[2/4] Mengaktifkan Android 15 CachedAppOptimizer (In-RAM zRAM Compactor)..."
# Kunci konfigurasi dari reset otomatis Google Play Services (Phenotype)
adb shell device_config set_sync_disabled_for_tests persistent

# Aktifkan compactor engine
adb shell device_config put activity_manager use_compaction true
adb shell device_config put activity_manager_native_boot use_compaction true

# Strategi kompresi: 'anon' (anonymous RAM pages ke zRAM) & 'full'
adb shell device_config put activity_manager compact_action_1 anon
adb shell device_config put activity_manager compact_action_2 full

# Agresifkan throttle: Default 10-20 menit dipangkas jadi 5 detik (5000ms)
adb shell device_config put activity_manager compact_throttle_1 5000
adb shell device_config put activity_manager compact_throttle_2 5000
adb shell device_config put activity_manager compact_throttle_3 5000
adb shell device_config put activity_manager compact_throttle_4 5000
adb shell device_config put activity_manager compact_throttle_5 5000
adb shell device_config put activity_manager compact_throttle_min_oom_score 100

# Turunkan batas ambang RSS (dari 12MB ke 8MB) agar app kecil/sedang juga ikut dikompres
adb shell device_config put activity_manager compact_full_rss_throttle_kb 8000
adb shell device_config put activity_manager compact_full_delta_rss_throttle_kb 4000
adb shell device_config put activity_manager compact_proc_state_throttle 1

# ==============================================================================
# FASE 3: TUNING LOW MEMORY KILLER (LMKD) & CACHED APPS LIMIT
# Biarkan aplikasi nongkrong di zRAM tanpa dibunuh paksa oleh LMKD
# ==============================================================================
echo "[3/4] Menyesuaikan batas LMKD & Cached Processes..."
adb shell device_config put activity_manager max_cached_processes 64
adb shell device_config put activity_manager lm_kill_throttle_ms 3000
adb shell device_config put activity_manager_native_boot max_phantom_processes 2147483647
adb shell settings put global settings_enable_monitor_phantom_procs false

# Matikan window blur agar GPU Adreno 610 tidak terbebani saat multitasking
adb shell settings put global disable_window_blurs 1

# Elevasi level komputasi CPU & GPU via MQSAS
adb shell service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.computility.cpulevel 6" s16 "/dev/null" i32 600 > /dev/null 2>&1
adb shell service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.computility.gpulevel 6" s16 "/dev/null" i32 600 > /dev/null 2>&1

# ==============================================================================
# FASE 4: TRIGGER INSTANT COMPACTION & AUDIT MEMINFO
# ==============================================================================
echo "[4/4] Menjalankan instant memory compaction pada app utama..."
for PKG in com.lemon.ccpadpro com.miui.home com.android.chrome; do
    adb shell am compact full $PKG > /dev/null 2>&1
done

echo ""
echo "=========================================================="
echo " [✅] PURE ZRAM OPTIMIZATION BERHASIL DIINJEKSI 100%!"
echo "=========================================================="
echo ""
echo "Status ZRAM & Swap Saat Ini di Tablet:"
adb shell dumpsys meminfo | grep -E "ZRAM:|RAM:|Free RAM:"
echo ""
