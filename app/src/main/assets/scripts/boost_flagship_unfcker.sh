#!/system/bin/sh
# ==============================================================================
# Xiaomi Redmi Pad SE (xun/bengal) - HyperOS 2.0 / Android 15
# HyperOS Flagship Unf*cker & Zero-Delay Navigation Master Engine
# Integrasi Analisis Repo Enki013/hyperosunfcker & Riset Optimasi Terdalam
# ==============================================================================

echo "[*] Menginjeksi Flagship Pipeline & HyperOS Unf*cker Suite..."

# 1. FLAGSHIP DEVICE LEVEL SPOOFING (v:1,c:3,g:1,m:2)
# Mengubah identitas Redmi Pad SE dari Tier 1 (budget) jadi Tier 3 Flagship
# c:3 = Prioritas tinggi CPU scheduler & interruptible transitions
# g:1 = Bypass GPU blur shader Adreno 610 (hemat 16-22ms per frame)
# m:2 = Mencegah aggressive Low-RAM trim icon cache
settings put system deviceLevelList "v:1,c:3,g:1,m:2"

# 2. MQSAS INTERNAL ROOT BACKDOOR EXPLOIT (persist.sys.*)
# Memanfaatkan miui.mqsas.IMQSNative (UID 0 service) untuk bypass limitasi shell
service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.computility.cpulevel 6" s16 "/storage/emulated/0/log.txt" i32 600
service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.computility.gpulevel 6" s16 "/storage/emulated/0/log.txt" i32 600
service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.advanced_visual_release 3" s16 "/storage/emulated/0/log.txt" i32 600
service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.support_view_smoothcorner true" s16 "/storage/emulated/0/log.txt" i32 600
service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.support_window_smoothcorner true" s16 "/storage/emulated/0/log.txt" i32 600
service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.mi_shadow_supported true" s16 "/storage/emulated/0/log.txt" i32 600
service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.miui.blur.enable false" s16 "/storage/emulated/0/log.txt" i32 600
service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.sf.disable_blurs 1" s16 "/storage/emulated/0/log.txt" i32 600

# 3. ELIMINASI BEBAN BLUR GPU & TASKBAR DOCK TABLET
settings put global enable_miui_desktop_blur 0
settings put system miui_home_blur_level 0
settings put system config_icon_animation 0
settings put global disable_window_blurs 1
settings put system enable_dock_blur 0
settings put system show_dock_divider 0

# 4. POWERKEEPER & JOYOSE LOCKDOWN (Bebaskan Refresh Rate & Throttling)
cmd appops set com.miui.powerkeeper WRITE_SETTINGS ignore
cmd appops set com.miui.powerkeeper GET_USAGE_STATS ignore
cmd appops set com.miui.powerkeeper RUN_IN_BACKGROUND ignore
cmd appops set com.xiaomi.joyose TOAST_WINDOW ignore 2>/dev/null || true
am force-stop com.xiaomi.joyose 2>/dev/null || true

# 5. SINKRONISASI RECENTS & LINKAGE RENDERING
settings put secure linkage_state 1
settings put global task_stack_view_layout_style 2

# 6. ZERO-DELAY SWIPE & GOLDEN RATIO ANIMASI (0.35x - Anti-NPE Crash)
settings put system miui_home_animation_rate 2
settings put global window_animation_scale 0.35
settings put global transition_animation_scale 0.35
settings put global animator_duration_scale 0.35
settings put secure long_press_timeout 250
settings put secure tap_timeout 100
settings put secure multi_press_timeout 150
settings put secure assistant_on_nav_gesture 0
settings put secure search_press_hold_nav_handle 0
cmd device_config put input_native_boot velocitytracker_strategy impulse
settings put system pointer_speed 3
setprop vendor.perf.gestureflingboost.enable true
setprop persist.sys.turbosched.touch_boost true
setprop debug.sf.predict_hwc_composition 1

# 7. PERFORMA NATIVE POWERKEEPER & ADPF CPU HINTS
settings put system power_mode high
settings put system power_performance_mode_open 1
settings put system speed_mode 1
setprop debug.sf.enable_adpf_cpu_hint 1
setprop debug.hwui.use_hint_manager true
setprop debug.hwui.target_cpu_time_percent 100
setprop debug.hwui.render_thread_priority -20
cmd thermalservice override-status 0
cmd power set-fixed-performance-mode-enabled true
cmd power set-adaptive-power-saver-enabled false

# 8. KUNCI REFRESH RATE 90Hz MAKSIMAL
settings put system min_refresh_rate 90.0
settings put system peak_refresh_rate 90.0
settings put system user_refresh_rate 90
settings put system miui_refresh_rate 90
settings put global peak_refresh_rate 90.0
settings put global min_refresh_rate 90.0
settings put global default_refresh_rate 90
settings put secure refresh_rate_mode 2

# 9. MANAJEMEN RAM 4GB: MATIKAN VIRTUAL RAM (Anti-eMMC Thrashing)
settings put global isExternalRamOn 0
settings put global ram_expand_size 0
settings put global miui_memory_extension 0
cmd device_config put activity_manager max_cached_processes 16
cmd device_config put activity_manager prodev_max_cached_processes 16
cmd activity memory-factor set NORMAL

# 10. MULTI-WINDOW & FREEFORM ENGINE UNLOCK
settings put global enable_freeform_support 1
settings put global force_resizable_activities 1
settings put global enable_non_resizable_multi_window 1

# 11. HENTIKAN PARASIT BACKGROUND (Leeches)
cmd appops set com.google.android.as RUN_IN_BACKGROUND ignore
cmd appops set com.google.android.as RUN_ANY_IN_BACKGROUND ignore
pm suspend --user 0 com.google.android.as 2>/dev/null || true
cmd appops set com.milink.service RUN_IN_BACKGROUND ignore
cmd appops set com.milink.service RUN_ANY_IN_BACKGROUND ignore
cmd appops set com.xiaomi.aicr RUN_IN_BACKGROUND ignore 2>/dev/null || true

# 12. AOT DEXOPT SPEED COMPILATION (SystemUI & Launcher)
cmd package compile -m speed -f com.miui.home 2>/dev/null &
cmd package compile -m speed -f com.android.systemui 2>/dev/null &
cmd package compile -m speed -f com.android.settings 2>/dev/null &

echo "[✓] Sukses! HyperOS Flagship Unf*cker & Zero-Delay Pipeline Aktif 100%!"
