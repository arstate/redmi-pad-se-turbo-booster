# 1. Touch & 90Hz Display
settings put system pointer_speed 7
settings put secure long_press_timeout 100
settings put secure multi_press_timeout 120
settings put secure tap_duration_threshold 0
settings put secure touch_blocking_period 0
settings put system touch.size.calibration geometric
settings put system touch.pressure.calibration none
setprop debug.touch.latency 0

settings put system min_refresh_rate 90.0
settings put system peak_refresh_rate 90.0
settings put system user_refresh_rate 90
settings put system miui_refresh_rate 90
settings put global default_refresh_rate 90
settings put secure match_content_frame_rate 0
settings put global thermal_limit_refresh_rate 0
settings put system thermal_limit_refresh_rate 0
settings put global fps_limit 90

# 2. 90 FPS Game Interventions
GAMES=(
  "com.mobile.legends"
  "com.tencent.ig"
  "com.roblox.client"
  "com.mojang.minecraftpe"
  "com.wejoy.weplay"
  "com.vincentb.MobControl"
  "com.FullFocusStudio.FeelFPV"
  "com.valvesoftware.steamlink"
  "com.limelight"
  "com.yanstarstudio.joss.undercover"
  "com.gamestar.perfectpiano"
  "com.block.juggle"
)
for pkg in "${GAMES[@]}"; do
  cmd game set --fps 90 "$pkg" 2>/dev/null || true
done
settings put global game_driver_all_apps 1
settings put global updatable_driver_all_apps 1

# 3. Anti-Thermal Throttling & Power
cmd thermalservice override-status 0
cmd power set-fixed-performance-mode-enabled true
cmd power set-adaptive-power-saver-enabled false
dumpsys deviceidle disable
settings put global adaptive_battery_management_enabled 0
settings put global app_standby_enabled 0
settings put global app_auto_restriction_enabled 0
settings put global automatic_power_save_mode 0
settings put global dynamic_power_savings_enabled 0
settings put global power_mode high_performance
settings put system power_mode high_performance
settings put global raw_performance_mode 1
settings put secure game_mode 1
settings put secure game_overlay_mode 1
settings put global high_performance_mode 1
settings put global miui_performance_mode 1
settings put system performance_mode 1

# 4. Vulkan SkiaVK, GPU VRAM & ADPF
setprop debug.hwui.renderer skiavk
setprop debug.hwui.use_gpu_pixel_buffers true
setprop debug.hwui.render_thread_priority -20
setprop debug.hwui.target_cpu_time_percent 10
setprop debug.hwui.use_partial_build_delay true
setprop debug.composition.type gpu
setprop debug.sf.hw 1
setprop debug.egl.hw 1
setprop debug.egl.buffering 2
setprop debug.egl.swapinterval 0
setprop debug.sf.latch_unsignaled 1
setprop debug.sf.early_phase_offset_ns 1000000
setprop debug.sf.early_app_phase_offset_ns 1000000
setprop debug.sf.early_gl_phase_offset_ns 1000000
setprop debug.sf.early_gl_app_phase_offset_ns 1000000
setprop debug.sf.high_fps_early_phase_offset_ns 1000000
setprop debug.sf.high_fps_early_gl_phase_offset_ns 1000000
setprop debug.sf.high_fps_late_app_phase_offset_ns 1000000
setprop debug.sf.high_fps_late_sf_phase_offset_ns 1000000
setprop debug.sf.max_igbp_buffer_count 5
setprop debug.hwui.texture_cache_size 96
setprop debug.hwui.layer_cache_size 64
setprop debug.hwui.r_buffer_cache_size 48
setprop debug.hwui.path_cache_size 32
setprop debug.hwui.gradient_cache_size 16
setprop debug.hwui.drop_shadow_cache_size 12
setprop debug.hwui.disable_draw_defer true
setprop debug.hwui.skip_empty_damage true
setprop debug.art.cc.min_free_bytes 16777216

device_config put performance_hint_service adpf_cpu_headroom_enabled true
device_config put performance_hint_service adpf_gpu_headroom_enabled true
device_config put performance_hint_service adpf_use_fmq_channel true
device_config put performance_hint_service adpf_enable_target_duration_rate true
device_config put surface_flinger late_sf_phase_offset_ns 1000000
device_config put surface_flinger late_app_phase_offset_ns 1000000
device_config put surface_flinger early_sf_phase_offset_ns 1000000
device_config put surface_flinger early_app_phase_offset_ns 1000000

device_config put lmk_native_boot kill_timeout_ms 100
device_config put lmk_native_boot thrashing_limit 80
device_config put lmk_native_boot swap_util_max 95
device_config put activity_manager max_cached_processes 64
device_config put activity_manager prodev_max_cached_processes 64
device_config put runtime_native_boot use_app_image_startup true
device_config put activity_manager max_phantom_processes 2147483647
settings put global cached_apps_freezer enabled


# 5. Unlimited Freeform & AOSP Desktop Windowing
settings put global enable_freeform_support 1
settings put global force_resizable_activities 1
settings put global enable_non_resizable_multi_window 1
settings put global force_desktop_mode_on_external_displays 1
settings put system enable_freeform_support 1
settings put secure enable_freeform_support 1
device_config put window_manager_native_boot com.android.window.flags.enable_desktop_windowing_mode true
device_config put window_manager_native_boot com.android.window.flags.show_desktop_windowing_dev_option true
device_config put window_manager_native_boot com.android.window.flags.enable_cascading_windows true
device_config put window_manager_native_boot com.android.window.flags.enable_windowing_edge_drag_resize true
device_config put window_manager com.android.window.flags.enable_desktop_windowing_mode true
device_config put window_manager com.android.window.flags.show_desktop_windowing_dev_option true

# 6. HyperOS Turbo Sched, GPU Driver, Game OOM Shield & SurfaceFlinger Anti-Jank
settings put global updatable_driver_production_opt_out_apps ""
settings put global updatable_driver_production_opt_in_apps "com.mobile.legends,com.tencent.ig,com.roblox.client,com.mojang.minecraftpe,com.wejoy.weplay,com.android.chrome"
settings put global updatable_driver_prerelease_opt_in_apps "com.mobile.legends,com.tencent.ig,com.roblox.client,com.mojang.minecraftpe"
settings put global game_driver_all_apps 1
settings put global updatable_driver_all_apps 1

GAMES="com.mobile.legends,com.tencent.ig,com.roblox.client,com.mojang.minecraftpe,com.wejoy.weplay,com.vincentb.MobControl,com.FullFocusStudio.FeelFPV,com.valvesoftware.steamlink,com.limelight,com.android.chrome"
CUR_TURBO=$(settings get system turbo_sched_core_app_list)
CUR_RT=$(settings get system rt_pkg_white_list)
settings put system turbo_sched_core_app_list "${CUR_TURBO},${GAMES}"
settings put system rt_pkg_white_list "${CUR_RT},${GAMES}"
settings put system perf_proc_game_List "$GAMES"
settings put system perf_game_oom_enable 1
settings put secure gb_boosting 1
settings put system screen_game_mode 1
settings put secure pref_open_game_booster 1

for pkg in com.mobile.legends com.tencent.ig com.roblox.client com.mojang.minecraftpe com.wejoy.weplay com.android.chrome; do
  dumpsys deviceidle whitelist +$pkg 2>/dev/null || true
done

setprop debug.hwui.render_ahead 1
setprop debug.sf.enable_gl_backpressure 0
setprop debug.qc.hardware true
setprop debug.qctwa.preservebuf 1
setprop debug.turbosched.enable true
setprop debug.turbosched.gaea.enable true
device_config put runtime_native_boot iorap_readahead_enable true
settings put global wifi_scan_always_enabled 0
settings put global wifi_sleep_policy 2
am stop-app com.google.android.googlequicksearchbox 2>/dev/null || true
cmd appops set com.google.android.googlequicksearchbox RUN_IN_BACKGROUND ignore 2>/dev/null || true
am force-stop com.android.camera 2>/dev/null || true
sm fstrim 2>/dev/null || true

# 7. Linux Kernel MGLRU, SurfaceFlinger Layer Caching & LMKD Kill Heaviest Task
device_config put mglru_native lru_gen_config 7
device_config put surface_flinger_native_boot com.android.graphics.surfaceflinger.flags.enable_layer_caching true
device_config put lmkd_native kill_heaviest_task true
device_config put lmk_native_boot kill_heaviest_task true
device_config put activity_manager_native_boot use_freezer true

# 8. Zero-Jank Drop Shadow & AppOps Freeze (Free 2.18GB RAM)
setprop debug.hwui.drop_shadow_cache_size 0
settings put system miui_desktop_icon_animation_enable 0
settings put system miui_home_app_icon_anim 0
settings put system miui_home_animation_rate 1
setprop debug.smartpower.display.enable false
setprop debug.smartpower.window.enable false
cmd appops set com.ss.android.ugc.trill RUN_IN_BACKGROUND ignore 2>/dev/null || true
cmd appops set com.ss.android.ugc.trill RUN_ANY_IN_BACKGROUND ignore 2>/dev/null || true
cmd appops set com.android.camera RUN_IN_BACKGROUND ignore 2>/dev/null || true
cmd appops set com.google.android.googlequicksearchbox RUN_IN_BACKGROUND ignore 2>/dev/null || true
cmd appops set com.google.android.googlequicksearchbox RUN_ANY_IN_BACKGROUND ignore 2>/dev/null || true

# 9. App Pinning (DeviceIdle Whitelist for Instant Warm Start < 50ms) & Launcher Anti-Freeze
for pkg in com.lemon.lvoverseas com.lemon.ccpadpro com.adobe.lrmobile com.mojang.minecraftpe com.roblox.client com.instagram.android org.telegram.messenger com.spotify.music com.whatsapp.w4b com.whatsapp com.tokopedia.tkpd com.shopee.id com.valvesoftware.steamlink ph.spacedesk.beta; do
  cmd deviceidle whitelist +$pkg 2>/dev/null || true
  cmd appops set $pkg RUN_IN_BACKGROUND allow 2>/dev/null || true
done
device_config put activity_manager use_compaction false
device_config put gpu android.provider.flags.angle_dynamic_denylist false
cmd power set-adaptive-power-saver-enabled false
cmd power set-mode 0
cmd thermalservice override-status 0
setprop debug.sf.disable_backpressure 1
setprop debug.egl.swapinterval 0
setprop debug.hwui.drop_shadow_cache_size 0
setprop debug.hwui.renderer skiavk
setprop debug.renderengine.backend skiavk

# 10. Hardware Composer (HWC) Direct Scanout & Flash Storage Trim
setprop debug.sf.predict_hwc_composition_strategy 1
setprop debug.sf.enable_hwc_vds 1
device_config put surface_flinger_native_boot com.android.graphics.surfaceflinger.flags.enable_layer_caching true
pm trim-caches 999G 2>/dev/null || true
sm fstrim 2>/dev/null || true

# 11. Logd Memory Trimming & Background Diagnostics Freeze
logcat -G 64K 2>/dev/null || true
cmd appops set com.android.devicediagnostics RUN_IN_BACKGROUND ignore 2>/dev/null || true
cmd appops set com.android.devicediagnostics RUN_ANY_IN_BACKGROUND ignore 2>/dev/null || true

# 12. WebView, Keyboard Zero-Latency & Wi-Fi Scan Throttling
settings put global wifi_scan_throttle_enabled 1 2>/dev/null || true
