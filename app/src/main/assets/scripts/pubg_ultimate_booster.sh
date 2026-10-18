#!/bin/bash
# =========================================================
# PUBG MOBILE ULTIMATE 90FPS & ANTI-THROTTLING BOOSTER
# For Redmi Pad SE (Snapdragon 680 / Adreno 610 / Android 15)
# =========================================================

echo "[+] 1. Bypassing Android & MIUI Thermal Engine..."
adb shell "cmd thermalservice override-status 0"

echo "[+] 2. Setting Android 15 Game Mode PERFORMANCE..."
adb shell "cmd device_config put game_overlay com.tencent.ig 'mode=2,fps=90,downscale=0.6:mode=3,fps=60'"
adb shell "cmd game mode 2 com.tencent.ig"

echo "[+] 3. Whitelisting PUBG from Doze & Battery Optimization..."
adb shell "dumpsys deviceidle whitelist +com.tencent.ig"
adb shell "cmd appops set com.tencent.ig RUN_IN_BACKGROUND allow"
adb shell "cmd appops set com.tencent.ig RUN_ANY_IN_BACKGROUND allow"
adb shell "am set-standby-bucket com.tencent.ig active"

echo "[+] 4. Enforcing Display 90Hz Lock..."
adb shell "settings put system miui_refresh_rate 90"
adb shell "settings put system user_refresh_rate 90"
adb shell "settings put system peak_refresh_rate 90"
adb shell "settings put system min_refresh_rate 90"

echo "[+] 5. Activating Qualcomm Driver & Speed Mode..."
adb shell "settings put system speed_mode 1"
adb shell "settings put secure speed_mode 1"
adb shell "settings put global power_mode 1"
adb shell "settings put global game_driver_all_apps 1"
adb shell "settings put global game_driver_opt_in_apps com.tencent.ig"
adb shell "settings put global updatable_driver_all_apps 1"
adb shell "settings put global updatable_driver_production_opt_in_apps com.tencent.ig"

echo "[+] 6. SurfaceFlinger & Latency Buffering Tuning..."
adb shell "setprop debug.sf.disable_backpressure 1"
adb shell "setprop debug.sf.latch_unsignaled 1"
adb shell "setprop debug.performance.tuning 1"
adb shell "setprop debug.egl.hw 1"

echo "[+] 7. Cleaning System Cache & Low Priority Tasks..."
adb shell "cmd activity kill-all"
adb shell "rm -rf /sdcard/Android/data/com.tencent.ig/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Logs/*"
adb shell "rm -rf /sdcard/Android/data/com.tencent.ig/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/PufferTmpDir/*"
adb shell "rm -rf /sdcard/Android/data/com.tencent.ig/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/pixuicache/*"

echo "[✓] PUBG MOBILE ULTRA 90FPS PROFILE APPLIED SUCCESSFULLY!"
