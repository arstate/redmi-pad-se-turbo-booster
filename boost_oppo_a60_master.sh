#!/system/bin/sh
# ==============================================================================
# OPPO A60 (CPH2631 - bengal / SM6225 Snapdragon 680) Master Optimization
# ColorOS 16 / Android 16 - Full Performance, 90Hz Lock & Zero-Lag Touch Suite
# ==============================================================================

echo "=================================================================="
echo "[*] MEMULAI OPTIMASI MASTER OPPO A60 (COLOROS 16 / SNAPDRAGON 680)"
echo "=================================================================="

# 1. COLOROS 16 HIGH-PERFORMANCE GOVERNOR & CPU/GPU CLOCK UNLOCK
echo "[1/10] Mengaktifkan ColorOS Mode Performa Tinggi & Kunci CPU Turbo..."
settings put system high_performance_mode_on 1
settings put system high_performance_mode_on_when_shutdown 1
settings put system power_save_method 0
settings put global power_mode high_performance
settings put system power_mode high_performance
settings put global high_performance_mode 1
settings put secure high_performance_mode 1
# cmd power set-fixed-performance-mode-enabled removed (caps CPU below peak burst)
cmd power set-adaptive-power-saver-enabled false
cmd thermalservice override-status 0
settings put global power_manager_constants "no_cached_app_scheduling=true"

# 2. LOCK 90HZ REFRESH RATE KONSTAN PADA PANEL COLOROS
echo "[2/10] Mengunci Layar ke 90Hz Konstan (Bypass Dynamic Throttling)..."
settings put secure oplus_customize_screen_refresh_rate 3
settings put system peak_refresh_rate 90.0
settings put system min_refresh_rate 90.0
settings put global peak_refresh_rate 90.0
settings put global min_refresh_rate 90.0
settings put secure user_refresh_rate 90
settings put system user_refresh_rate 90
settings put global default_refresh_rate 90
settings put global fps_limit 90
settings put secure match_content_frame_rate 0
settings put global thermal_limit_refresh_rate 0
settings put system thermal_limit_refresh_rate 0

# 3. SURFACEFLINGER & ADRENO 610 VULKAN SKIAVK RENDERING
echo "[3/10] Optimasi GPU Adreno 610 (SkiaVK Renderer & ADPF Hints)..."
setprop debug.hwui.renderer skiavk
setprop debug.hwui.target_cpu_time_percent 100
setprop debug.hwui.render_thread_priority -20
setprop debug.hwui.use_hint_manager true
setprop debug.sf.enable_adpf_cpu_hint 1
setprop debug.sf.disable_client_composition_cache 1
setprop debug.sf.drop_missed_frames 1
setprop vendor.perf.framepacing.enable 1
settings put global disable_window_blurs 1

# 4. ZERO-DELAY SWIPE HOME & GOLDEN ANIMATION SCALE (0.35x ANTI-CRASH)
echo "[4/10] Mengatur Golden Ratio Animasi 0.35x & Impulse Touch Engine..."
settings put global window_animation_scale 0.35
settings put global transition_animation_scale 0.35
settings put global animator_duration_scale 0.35
settings put system orientation_animation 0
settings put secure long_press_timeout 250
settings put secure tap_timeout 100
settings put secure multi_press_timeout 150
settings put system pointer_speed 3
cmd device_config put input_native_boot velocitytracker_strategy impulse
settings put secure assistant_on_nav_gesture 0
settings put secure search_press_hold_nav_handle 0
setprop vendor.perf.gestureflingboost.enable true

# 5. MATIKAN NANDSWAP / VIRTUAL RAM (HEMAT I/O UFS & LEPAS BEBAN 8GB RAM)
echo "[5/10] Menonaktifkan NANDSwap (Virtual RAM Flash Storage)..."
settings put secure nandswap_ui_feature_state false
settings put global isExternalRamOn 0
settings put global ram_expand_size 0
setprop persist.sys.oplus.nandswap false
setprop persist.sys.oplus.nandswap.swapsize 0
cmd device_config put activity_manager max_cached_processes 32
cmd device_config put activity_manager prodev_max_cached_processes 32
cmd activity memory-factor set NORMAL

# 6. STORAGE I/O MAINTENANCE & SQLITE TUNING
echo "[6/10] Maintenance Storage I/O & SQLite DB Tuning..."
device_config override system_performance android.database.sqlite.sqlite_allow_temp_tables true
device_config override system_performance android.database.sqlite.simple_sql_comment_scanner true
device_config override system_performance android.database.sqlite.oneway_finalizer_close_fixed true
device_config override storage_native_boot smart_idle_maint_enabled true
device_config override storage_native_boot charging_required false
sm fstrim
cmd activity idle-maintenance

# 7. ANTI-CRASH GAME DRIVERS (SAFETY UNTUK MLBB, FREE FIRE, ROBLOX)
echo "[7/10] Konfigurasi Game Driver Native & Target 90 FPS..."
settings delete global angle_gl_driver_selection_values
settings delete global angle_gl_driver_selection_pkgs
settings delete global updatable_driver_all_apps
settings delete global game_driver_all_apps
settings delete global GPUTUNER_SWITCH

cmd game set --fps 90 com.mobile.legends 2>/dev/null || true
cmd game set --fps 90 com.dts.freefiremax 2>/dev/null || true
cmd game set --fps 90 com.roblox.client 2>/dev/null || true

cmd appops set com.mobile.legends READ_EXTERNAL_STORAGE allow 2>/dev/null || true
cmd appops set com.mobile.legends WRITE_EXTERNAL_STORAGE allow 2>/dev/null || true
cmd appops set com.mobile.legends MANAGE_EXTERNAL_STORAGE allow 2>/dev/null || true
cmd appops set com.dts.freefiremax READ_EXTERNAL_STORAGE allow 2>/dev/null || true
cmd appops set com.dts.freefiremax WRITE_EXTERNAL_STORAGE allow 2>/dev/null || true
cmd appops set com.dts.freefiremax MANAGE_EXTERNAL_STORAGE allow 2>/dev/null || true

# 8. MULTI-WINDOW & FREEFORM FLEXIBLE WINDOWS UNLOCK
echo "[8/10] Mengaktifkan AOSP Freeform & Multi-Window..."
settings put global enable_freeform_support 1
settings put global force_resizable_activities 1
settings put global enable_non_resizable_multi_window 1

# 9. SUPPRESS BLOATWARE & BACKGROUND LEECHES
echo "[9/10] Menekan Konsumsi RAM Bloatware Latar Belakang..."
cmd appops set com.google.android.as RUN_IN_BACKGROUND ignore
cmd appops set com.google.android.as RUN_ANY_IN_BACKGROUND ignore
pm suspend --user 0 com.google.android.as 2>/dev/null || true

for pkg in com.heytap.wallet com.heytap.mall com.nearme.gamecenter com.amazon.appmanager com.amazon.avod.thirdpartyclient com.oplus.member com.oplus.riderMode; do
  cmd appops set $pkg RUN_IN_BACKGROUND ignore 2>/dev/null || true
  cmd appops set $pkg RUN_ANY_IN_BACKGROUND ignore 2>/dev/null || true
done

# 10. AHEAD-OF-TIME (AOT) NATIVE DEX COMPILATION
echo "[10/10] Melakukan AOT Speed Compilation pada SystemUI & Launcher..."
cmd package compile -m speed -f com.android.systemui 2>/dev/null &
cmd package compile -m speed -f com.android.launcher 2>/dev/null &
cmd package compile -m speed -f com.android.settings 2>/dev/null &

echo "=================================================================="
echo "[✓] SUKSES! MASTER OPTIMASI OPPO A60 BERHASIL DITERAPKAN 100%!"
echo "=================================================================="
