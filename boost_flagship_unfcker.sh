#!/bin/sh
# Xiaomi Redmi Pad SE - Flagship Tier & HyperOSUnf*cker Master Optimization
# Unlocks Tier-3 Flagship Pipeline, MQSAS Kernel Boost & Zero-Delay Desktop Engine

echo "[*] Menginjeksi Flagship Device Level & HyperOSUnf*cker Stack..."

# 1. FLAGSHIP DEVICE LEVEL SPOOFING (v:1,c:3,g:3)
# Mengubah identitas Redmi Pad SE dari Tier 1 (budget c:1,g:1) menjadi Tier 3 Flagship (setara Xiaomi Pad 6 Pro)
settings put system deviceLevelList "v:1,c:3,g:3"

# 2. MQSAS INTERNAL ROOT BACKDOOR SERVICE EXPLOIT (persist.sys.*)
# Memanfaatkan miui.mqsas.IMQSNative (UID 0 root service) untuk mengunci CPU & GPU ke level 6 (Max Perf)
service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.computility.cpulevel 6" s16 "/storage/emulated/0/log.txt" i32 600
service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.computility.gpulevel 6" s16 "/storage/emulated/0/log.txt" i32 600
service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.advanced_visual_release 3" s16 "/storage/emulated/0/log.txt" i32 600
service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.support_view_smoothcorner true" s16 "/storage/emulated/0/log.txt" i32 600
service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.support_window_smoothcorner true" s16 "/storage/emulated/0/log.txt" i32 600
service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.mi_shadow_supported true" s16 "/storage/emulated/0/log.txt" i32 600
service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.miui.blur.enable false" s16 "/storage/emulated/0/log.txt" i32 600
service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.sf.disable_blurs 1" s16 "/storage/emulated/0/log.txt" i32 600

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

# 6. LAUNCHER ANIMATION SPEED & SAFE 0.5x DURATION (Anti-NPE Crash)
settings put system miui_home_animation_rate 2
settings put global window_animation_scale 0.5
settings put global transition_animation_scale 0.5
settings put global animator_duration_scale 0.5
settings put secure long_press_timeout 250
settings put secure multi_press_timeout 200

# 7. AOT NATIVE ARM64 SPEED COMPILATION PADA LAUNCHER & SYSTEMUI
cmd package compile -m speed -f com.miui.home 2>/dev/null &
cmd package compile -m speed -f com.android.systemui 2>/dev/null &

echo "[✓] Sukses! Flagship Tier & Zero-Delay Pipeline Berhasil Aktif!"
