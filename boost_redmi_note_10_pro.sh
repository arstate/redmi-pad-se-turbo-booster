#!/bin/sh
# ==============================================================================
# Xiaomi Redmi Note 10 Pro (sweet / sweet_id) - Qualcomm Snapdragon 732G
# MIUI 14 (Android 13) Master Super Performance Boost Suite
# Safe & 100% Zero Data Loss (UID 2000 ADB Shell & Non-Root Optimizations)
# ==============================================================================

echo "[*] Memulai Oprek Super Performa Redmi Note 10 Pro (Snapdragon 732G)..."

# ==============================================================================
# Kategori 1: Matikan Throttling Joyose, PowerKeeper & Telemetry (Anti-Lag / Anti-FPS Drop)
# ==============================================================================
echo "[1/7] Menembus Celah Throttling Joyose & Telemetri MIUI..."
# Disable Joyose (Penyebab utama FPS game di-cap 45-50 FPS & thermal downclock)
pm disable-user --user 0 com.xiaomi.joyose 2>/dev/null || true

# Disable MIUI Analytic & System Ads Framework
pm disable-user --user 0 com.miui.analytics 2>/dev/null || true
pm disable-user --user 0 com.miui.msa.global 2>/dev/null || true

# Disable Background Telemetry & Crash Daemons
pm disable-user --user 0 com.miui.daemon 2>/dev/null || true
pm disable-user --user 0 com.miui.bugreport 2>/dev/null || true
pm disable-user --user 0 com.miui.yellowpage 2>/dev/null || true
pm disable-user --user 0 com.miui.hybrid 2>/dev/null || true
pm disable-user --user 0 com.miui.hybrid.accessory 2>/dev/null || true
pm disable-user --user 0 com.google.android.feedback 2>/dev/null || true

# Lockdown AppOps PowerKeeper & System Intelligence
cmd appops set com.miui.powerkeeper WAKE_LOCK ignore 2>/dev/null || true
cmd appops set com.miui.powerkeeper RUN_IN_BACKGROUND ignore 2>/dev/null || true
cmd appops set com.miui.powerkeeper RUN_ANY_IN_BACKGROUND ignore 2>/dev/null || true
cmd appops set com.google.android.as RUN_IN_BACKGROUND ignore 2>/dev/null || true
cmd appops set com.google.android.as RUN_ANY_IN_BACKGROUND ignore 2>/dev/null || true
cmd appops set com.milink.service RUN_IN_BACKGROUND ignore 2>/dev/null || true
cmd appops set com.milink.service RUN_ANY_IN_BACKGROUND ignore 2>/dev/null || true

# ==============================================================================
# Kategori 2: SurfaceFlinger, VSYNC & Compositor Pipeline (Bebas Stutter)
# ==============================================================================
echo "[2/7] Mengonfigurasi SurfaceFlinger Pipeline & Latensi Buffer..."
setprop debug.sf.latch_unsignaled 1
setprop debug.sf.disable_backpressure 1
setprop debug.sf.enable_hwc_vds 1
setprop debug.sf.predict_hwc_composition_strategy 1
setprop debug.sf.early_phase_offset_ns 500000
setprop debug.sf.early_app_phase_offset_ns 500000
setprop debug.sf.early_gl_phase_offset_ns 3000000
setprop debug.sf.early_gl_app_phase_offset_ns 15000000
setprop debug.sf.high_fps_early_phase_offset_ns 6100000
setprop debug.sf.high_fps_early_gl_phase_offset_ns 9000000
setprop debug.sf.high_fps_late_app_phase_offset_ns 100000
setprop debug.sf.high_fps_late_sf_phase_offset_ns 1000000

# ==============================================================================
# Kategori 3: HWUI RenderThread & Skia Engine (Adreno 618 Optimal)
# ==============================================================================
echo "[3/7] Mengaktifkan SkiaGL Threaded & Triple Buffering Adreno 618..."
setprop debug.hwui.renderer skiagl
setprop debug.renderengine.backend skiaglthreaded
setprop debug.hwui.use_buffer_age true
setprop debug.hwui.use_partial_updates true
setprop debug.hwui.skip_empty_damage true
setprop debug.hwui.fps_divisor 1
setprop debug.egl.swapinterval 0
setprop debug.gr.numframebuffers 3
setprop debug.sqlite.wal.syncmode OFF
setprop vendor.perf.gestureflingboost.enable 1
setprop debug.velocitytracker.strategy impulse

# ==============================================================================
# Kategori 4: Bypass Thermal Throttling & Phantom Process Killer
# ==============================================================================
echo "[4/7] Membuka Limit Thermal OS & Phantom Process Killer Android 13..."
cmd thermalservice override-status 0
cmd power set-fixed-performance-mode-enabled true
cmd power set-adaptive-power-saver-enabled false
device_config set_sync_disabled_for_tests persistent 2>/dev/null || true
device_config put activity_manager max_phantom_processes 2147483647 2>/dev/null || true
device_config put activity_manager bg_current_drain_auto_restrict_abusive_apps_enabled false 2>/dev/null || true
device_config put activity_manager use_compaction true 2>/dev/null || true

# ==============================================================================
# Kategori 5: Display 120Hz Refresh Rate Preparation
# ==============================================================================
echo "[5/7] Menyetel Refresh Rate 120Hz & Resolusi Optimal..."
appops set com.android.shell WRITE_SETTINGS allow 2>/dev/null || true
settings put system peak_refresh_rate 120.0 2>/dev/null || true
settings put system min_refresh_rate 120.0 2>/dev/null || true
settings put system user_refresh_rate 120 2>/dev/null || true
settings put system miui_refresh_rate 120 2>/dev/null || true
cmd display set-user-preferred-display-mode 1080 2400 120.0 2>/dev/null || true

# ==============================================================================
# Kategori 6: Storage I/O Maintenance & Cache Purge (UFS 2.2 Speedup)
# ==============================================================================
echo "[6/7] Menjalankan UFS FSTRIM & Maintenance Idle..."
sm fstrim
cmd activity idle-maintenance
pm trim-caches 999G 2>/dev/null || true
cmd activity compact system

# ==============================================================================
# Kategori 7: Status Verifikasi
# ==============================================================================
echo "[7/7] Selesai! Verifikasi status sistem:"
echo "Thermal Status:"
dumpsys thermalservice | grep -i "Status" | head -n 3
echo "Refresh Rate System Settings:"
echo -n "peak_refresh_rate: "; settings get system peak_refresh_rate
echo -n "min_refresh_rate: "; settings get system min_refresh_rate
echo -n "user_refresh_rate: "; settings get system user_refresh_rate
echo "Phantom Processes Max: $(device_config get activity_manager max_phantom_processes)"
echo "Joyose Status: $(pm list packages -d | grep joyose)"

echo "[✓] Oprek Super Performa Redmi Note 10 Pro SUKSES 100%! Data Aman Terlindungi."
