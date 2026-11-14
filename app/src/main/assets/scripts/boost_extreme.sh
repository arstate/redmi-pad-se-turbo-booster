#!/system/bin/sh
echo "=== [1/6] SURFACEFLINGER & GPU BLUR REMOVAL ==="
# Matikan window blurs berat yang membebani Adreno 610
settings put global disable_window_blurs 1
setprop persist.sys.sf.disable_blurs 1
setprop ro.surface_flinger.supports_background_blur 0
setprop debug.sf.disable_client_composition_cache 1
device_config put surface_flinger_native_boot com.android.graphics.surfaceflinger.flags.use_adpf_cpu_hint true
device_config put surface_flinger use_adpf_cpu_hint true
device_config put surface_flinger_native_boot com.android.graphics.surfaceflinger.flags.multithreaded_present true

echo "=== [2/6] HYPEROS SMARTPOWER & THERMAL FPS LOCK BYPASS ==="
# Matikan throttling display dan auto-downclock FPS HyperOS
setprop persist.sys.smartpower.display.enable false
setprop persist.sys.smartpower.window.enable false
setprop persist.sys.turbosched.thermal_break.enable false
setprop ro.vendor.fps.switch.thermal false
setprop persist.vendor.powerhal.rendering 1
setprop vendor.perf.gestureflingboost.enable true
cmd thermalservice override-status 0
dumpsys deviceidle disable
settings put secure refresh_rate_mode 2
settings put system min_refresh_rate 90.0
settings put system peak_refresh_rate 90.0
settings put global thermal_limit_refresh_rate 0
settings put system thermal_limit_refresh_rate 0

echo "=== [3/6] RAM 4GB RE-TUNING (ANTI-LAUNCHER-KILL) ==="
device_config put activity_manager max_cached_processes 24
device_config put activity_manager prodev_max_cached_processes 24
device_config put activity_manager use_compaction true
device_config put lmk_native_boot thrashing_limit 30
device_config put lmk_native_boot swap_util_max 80
device_config put lmk_native_boot kill_timeout_ms 50
device_config override lmkd_native kill_heaviest_task false
device_config delete lmkd_native kill_heaviest_task 2>/dev/null || true

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

echo "=== [4/6] CRIPPLE THROTTLER & BACKGROUND LEECHES ==="
# Blokir Powerkeeper & Android System Intelligence latar belakang
cmd appops set com.miui.powerkeeper RUN_IN_BACKGROUND ignore 2>/dev/null || true
cmd appops set com.miui.powerkeeper RUN_ANY_IN_BACKGROUND ignore 2>/dev/null || true
cmd appops set com.google.android.as RUN_IN_BACKGROUND ignore 2>/dev/null || true
cmd appops set com.google.android.as RUN_ANY_IN_BACKGROUND ignore 2>/dev/null || true
am force-stop com.google.android.as 2>/dev/null || true
am force-stop com.xiaomi.cameramind 2>/dev/null || true
am force-stop com.xiaomi.aicr 2>/dev/null || true

echo "=== [5/6] QUALCOMM ADRENO HARDWARE ACCELERATION ==="
setprop debug.qc.hardware true
setprop debug.qctwa.preservebuf 1
setprop debug.qctwa.forcegpu 1
setprop debug.hwui.renderer skiagl
setprop debug.renderengine.backend skiaglthreaded
setprop debug.sf.enable_gl_backpressure 0

echo "=== [6/6] SYSTEM ANIMATION INSTANT RESPONSE ==="
settings put global window_animation_scale 0.25
settings put global transition_animation_scale 0.25
settings put global animator_duration_scale 0.25

echo "=== [7/6] SPEED-AOT COMPILATION (ROBLOX, PUBG, MINECRAFT) ==="
for pkg in com.roblox.client com.tencent.ig com.mojang.minecraftpe ph.spacedesk.beta; do
  cmd package compile -m speed -f "$pkg" 2>/dev/null &
done
for capcut_pkg in com.lemon.lvoverseas com.lemon.ccpadpro; do
  cmd package compile -m speed-profile "$capcut_pkg" 2>/dev/null &
done

echo "=== BOOST EXTREME APPLIED! ==="
