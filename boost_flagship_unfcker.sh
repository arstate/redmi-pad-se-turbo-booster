#!/bin/sh
# Xiaomi Redmi Pad SE - Flagship Tier & HyperOSUnf*cker Master Optimization
# Unlocks Tier-3 Flagship Pipeline, MQSAS Kernel Boost & Zero-Delay Desktop Engine

echo "[*] Menginjeksi Flagship Device Level & HyperOSUnf*cker Stack..."

# 1. RESET ADVANCED VISUAL & HARDWARE SMOOTHCORNER (Fixes libhwui SIGSEGV on Adreno 610)
service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.computility.cpulevel 0" s16 "/dev/null" i32 600
service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.advanced_visual_release 0" s16 "/dev/null" i32 600
service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.support_view_smoothcorner false" s16 "/dev/null" i32 600
service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.support_window_smoothcorner false" s16 "/dev/null" i32 600
service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.mms.kill_fas_cached_idle false" s16 "/dev/null" i32 600
service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.mi_shadow_supported true" s16 "/dev/null" i32 600
service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.miui.blur.enable false" s16 "/dev/null" i32 600
service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.sf.disable_blurs 1" s16 "/dev/null" i32 600
service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.miui.sf_fps 90" s16 "/dev/null" i32 600

# Explicit Home Launcher & SystemUI Immunity
cmd activity set-bg-restriction-level --user 0 com.miui.home exempted 2>/dev/null || true
am set-standby-bucket com.miui.home active 2>/dev/null || true
cmd appops set com.miui.home RUN_IN_BACKGROUND allow 2>/dev/null || true
cmd appops set com.miui.home RUN_ANY_IN_BACKGROUND allow 2>/dev/null || true
dumpsys deviceidle whitelist +com.miui.home 2>/dev/null || true
dumpsys deviceidle whitelist +com.android.systemui 2>/dev/null || true
device_config put system_performance com.android.server.flags.skip_home_art_pins false 2>/dev/null || true
device_config put system_performance com.android.server.flags.pin_webview true 2>/dev/null || true
cmd pinner repin 2>/dev/null || true

# 3. MATIKAN WALLPAPER BLUR & DESKTOP GAUSSIAN BLUR SAAT SWIPE
settings put global enable_miui_desktop_blur 0
settings put system miui_home_blur_level 0
settings put system config_icon_animation 0

# 4. POWERKEEPER LOCKDOWN VIA APPOPS (Bebaskan Refresh Rate & Touch Governor)
cmd appops set com.miui.powerkeeper WRITE_SETTINGS ignore
cmd appops set com.miui.powerkeeper GET_USAGE_STATS ignore
cmd appops set com.miui.powerkeeper RUN_IN_BACKGROUND ignore

# 5. SINKRONISASI RECENTS & LINKAGE RENDERING
settings put secure linkage_state 1
settings put global task_stack_view_layout_style 2

# 6. LAUNCHER ANIMATION SPEED & SAFE 0.60x DURATION (Flagship Balanced)
settings put system miui_home_animation_rate 2
settings put global window_animation_scale 0.60
settings put global transition_animation_scale 0.60
settings put global animator_duration_scale 0.60
settings put secure long_press_timeout 200
settings put secure multi_press_timeout 150

# 7. AOT NATIVE ARM64 SPEED COMPILATION PADA LAUNCHER & SYSTEMUI
cmd package compile -m speed -f com.miui.home 2>/dev/null &
cmd package compile -m speed -f com.android.systemui 2>/dev/null &

echo "[✓] Sukses! Flagship Tier & Zero-Delay Pipeline Berhasil Aktif!"
