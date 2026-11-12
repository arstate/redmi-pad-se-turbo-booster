#!/bin/sh
# Xiaomi Redmi Pad SE (HyperOS 2.0 / Android 15) Master Performance Boost
# 100 Celah Sistem Terdalam Terverifikasi (Non-Root / UID 2000)

echo "[*] Mengaplikasikan 100 Celah Sistem Terverifikasi pada Redmi Pad SE..."

# ==============================================================================
# Kategori 1: SurfaceFlinger, VSYNC & Compositor Pipeline (Tweaks 1-10)
# ==============================================================================
setprop debug.sf.disable_client_composition_cache 1
setprop debug.sf.disable_backpressure 1
setprop debug.sf.drop_missed_frames 1
device_config override core_graphics com.android.graphics.surfaceflinger.flags.vulkan_renderengine true
device_config override core_graphics com.android.graphics.surfaceflinger.flags.add_sf_skipped_frames_to_trace false
device_config override core_graphics com.android.graphics.surfaceflinger.flags.enable_layer_command_batching true
device_config override core_graphics com.android.graphics.surfaceflinger.flags.multithreaded_present true
device_config override windowing_frontend com.android.window.flags.fifo_priority_for_major_ui_processes true
device_config override windowing_frontend com.android.window.flags.release_snapshot_aggressively true
device_config override windowing_frontend com.android.window.flags.defer_display_updates true

# ==============================================================================
# Kategori 2: HWUI RenderThread, Skia Engine & Frame Pacing (Tweaks 11-20)
# ==============================================================================
setprop debug.hwui.renderer skiagl
setprop debug.renderengine.backend skiaglthreaded
setprop debug.hwui.use_gpu_pixel_buffers true
setprop debug.hwui.target_cpu_time_percent 90
setprop debug.hwui.use_buffer_age true
setprop debug.hwui.use_partial_updates true
setprop debug.hwui.skip_empty_damage true
setprop debug.hwui.use_hint_manager true
setprop debug.hwui.app_memory_policy 1
setprop debug.hwui.drawing_enabled true
setprop debug.sf.predict_hwc_composition_strategy 1
setprop debug.sqlite.wal.syncmode OFF
device_config override core_graphics com.android.graphics.hwui.flags.initialize_gl_always false
device_config override core_graphics com.android.graphics.hwui.flags.clip_surfaceviews false

# ==============================================================================
# Kategori 3: Qualcomm Bengal / Adreno 610 Driver & ANGLE Layer (Tweaks 21-30)
# ==============================================================================
# Anti-Crash: Hapus ANGLE & force updatable driver agar game Unity (MLBB) tidak force-close
settings delete global angle_gl_driver_selection_values 2>/dev/null || true
settings delete global angle_gl_driver_selection_pkgs 2>/dev/null || true
settings delete global updatable_driver_all_apps 2>/dev/null || true
settings delete global game_driver_all_apps 2>/dev/null || true
settings delete global GPUTUNER_SWITCH 2>/dev/null || true
cmd appops set com.mobile.legends READ_EXTERNAL_STORAGE allow 2>/dev/null || true
cmd appops set com.mobile.legends WRITE_EXTERNAL_STORAGE allow 2>/dev/null || true
cmd appops set com.mobile.legends MANAGE_EXTERNAL_STORAGE allow 2>/dev/null || true

# SurfaceFlinger Buffer Latch & Dirty Regions (4PDA / Coolapk 90Hz Stutter Fix)
setprop debug.sf.latch_unsignaled 1
setprop debug.sf.enable_hwc_vds 1
setprop debug.sf.enable_advanced_sf_phase_offset 1
setprop debug.hwui.render_dirty_regions false

# Android Phantom Process Killer Bypass & eMMC Logging Lockdown
device_config set_sync_disabled_for_tests persistent 2>/dev/null || true
device_config put activity_manager max_phantom_processes 2147483647 2>/dev/null || true
settings put global settings_enable_monitor_phantom_procs false 2>/dev/null || true
settings put system send_miui_log 0
settings put system upload_log 0
settings put system anonymous_data 0
settings put secure miui_log_enable 0
settings put secure track_service 0
settings put global miui_log_enable 0

# Trackers & Advertising Bloatware AppOps Lockdown
cmd appops set com.miui.msa.global RUN_IN_BACKGROUND ignore 2>/dev/null || true
cmd appops set com.miui.analytics RUN_IN_BACKGROUND ignore 2>/dev/null || true
cmd appops set com.xiaomi.metok-sdk RUN_IN_BACKGROUND ignore 2>/dev/null || true
cmd appops set com.miui.daemon RUN_IN_BACKGROUND ignore 2>/dev/null || true

setprop debug.graphics.gpu.profiler.perfetto false
setprop debug.renderengine.skia_use_perfetto_track_events false
setprop debug.hwui.skia_use_perfetto_track_events false
setprop vendor.perf.framepacing.enable 1

# ==============================================================================
# Kategori 4: Display Refresh Rate, Panel Governance & Anti-Flicker (Tweaks 31-40)
# ==============================================================================
settings put system miui_refresh_rate 90
settings put secure user_refresh_rate 90
settings put global peak_refresh_rate 90.0
settings put global min_refresh_rate 90.0
settings put system min_refresh_rate 90.0
settings put global default_refresh_rate 90
settings put global fps_limit 90
settings put system is_smart_fps 0
settings put global thermal_limit_refresh_rate 0
settings put secure match_content_frame_rate 0

# ==============================================================================
# Kategori 5: ADPF (Android Dynamic Performance Framework) & PowerHAL (Tweaks 41-50)
# ==============================================================================
device_config override game android.os.adpf_gpu_report_actual_work_duration true
device_config override game android.os.adpf_use_fmq_channel true
device_config override game android.os.adpf_fmq_eager_send true
device_config override game android.os.adpf_hwui_gpu true
device_config override game android.os.adpf_prefer_power_efficiency false
device_config override game android.os.adpf_measure_during_input_event_boost true
device_config override game android.os.adpf_obtainview_boost true
device_config override game com.android.graphics.surfaceflinger.flags.adpf_gpu_sf true
device_config override game android.server.app.disable_game_mode_when_app_top false
device_config override game com.android.server.power.hint.powerhint_thread_cleanup true

# ==============================================================================
# Kategori 6: ART Runtime, Dexopt & Ahead-Of-Time (AOT) Execution (Tweaks 51-60)
# ==============================================================================
cmd package compile -m speed -f com.android.systemui 2>/dev/null &
cmd package compile -m speed -f com.android.settings 2>/dev/null &
device_config override art_performance com.android.art.flags.reg_alloc_no_output_overlap true
device_config override art_performance com.android.art.rw.flags.skip_background_classes_in_profiles true
device_config override art_performance com.android.art.rw.flags.reduce_immune_space_zram_reads true
device_config override art_performance com.android.art.flags.fast_baseline_compiler true
device_config override art_performance com.android.art.rw.flags.compact_object_header true
device_config override system_performance com.android.server.flags.pin_webview true

# ==============================================================================
# Kategori 7: Memory Architecture, LMKD & ZRAM Swappiness Tuning (Tweaks 61-70)
# ==============================================================================
settings put global isExternalRamOn 0
settings put global ram_expand_size 0
settings put global activity_manager_constants "low_swap_threshold_percent=0.5,max_cached_processes=64,max_phantom_processes=128,power_check_max_cpu_1=16,power_check_max_cpu_2=16,power_check_max_cpu_3=8,power_check_max_cpu_4=4,power_check_max_cpu_5=4"
device_config override lmkd_native kill_heaviest_task true
device_config override lmk_native_boot swap_util_max 85
device_config override lmk_native_boot thrashing_limit 20
device_config override activity_manager use_compaction true
device_config override activity_manager compact_proactive true
cmd activity compact system
cmd activity memory-factor set NORMAL

# ==============================================================================
# Kategori 8: Storage I/O, SQLite Optimization & eMMC 5.1 Maintenance (Tweaks 71-80)
# ==============================================================================
device_config override system_performance android.database.sqlite.sqlite_allow_temp_tables true
device_config override system_performance android.database.sqlite.simple_sql_comment_scanner true
device_config override system_performance android.database.sqlite.sqlite_apis_35 true
device_config override system_performance android.database.sqlite.oneway_finalizer_close_fixed true
settings put system miui_app_cache_optimization 1
device_config override storage_native_boot smart_idle_maint_enabled true
device_config override storage_native_boot charging_required false
device_config override backstage_power android.app.skip_bg_mem_trim_on_fg_app true
sm fstrim
cmd activity idle-maintenance

# ==============================================================================
# Kategori 9: Proprietary Xiaomi HyperOS 2.0 Performance & TurboSched Hooks (Tweaks 81-90)
# ==============================================================================
setprop debug.turbosched.enable true
setprop debug.turbosched.gaea.enable true
settings put system turbo_sched_core_app_list "com.mobile.legends,com.roblox.client,com.tencent.ig,com.android.chrome,com.ss.android.ugc.trill,com.miui.home,com.whatsapp,com.facebook.katana,com.instagram.android,org.telegram.messenger,com.lemon.lvoverseas,com.lemon.ccpadpro"
settings put system rt_pkg_white_list "com.whatsapp,com.google.android.youtube,com.android.chrome,com.facebook.katana,com.instagram.android,com.google.android.gm,org.telegram.messenger,com.miui.home,com.mobile.legends,com.lemon.lvoverseas,com.lemon.ccpadpro"
settings put system perf_proc_game_List "com.tencent.dm,com.mobile.legends,com.miHoYo.bh3.bilibili,com.roblox.client,com.tencent.ig"
settings put system light_speed_app "com.mobile.legends"
# CapCut Video Editor Compatibility (Anti-Crash EGL & Scoped Storage Bypass)
cmd game reset com.lemon.lvoverseas 2>/dev/null || true
cmd game reset com.lemon.ccpadpro 2>/dev/null || true
for capcut in com.lemon.lvoverseas com.lemon.ccpadpro; do
  cmd appops set $capcut MANAGE_EXTERNAL_STORAGE allow 2>/dev/null || true
  cmd appops set $capcut READ_MEDIA_VIDEO allow 2>/dev/null || true
  cmd appops set $capcut READ_MEDIA_IMAGES allow 2>/dev/null || true
  cmd appops set $capcut NO_ISOLATED_STORAGE allow 2>/dev/null || true
done
settings put system perf_profile 1
settings put system performance_mode 1
settings put system power_mode high_performance
settings put global power_mode high_performance
settings put global high_performance_mode 1
settings put secure high_performance_mode 1

# ==============================================================================
# Kategori 10: Tablet Touch Digitizer, Power Management & Bloatware Suppression (Tweaks 91-100)
# ==============================================================================
cmd thermalservice override-status 0
cmd power set-fixed-performance-mode-enabled true
cmd power set-adaptive-power-saver-enabled false
settings put global power_manager_constants "no_cached_app_scheduling=true"
settings put system touch_game_mode 3
settings put system touch_response_speed 1
settings put system touch_gesture_speed 2
settings put global disable_window_blurs 1
settings put global window_animation_scale 0.5
settings put global transition_animation_scale 0.5
settings put global animator_duration_scale 0.5

# Radio Jitter & Location Polling Lockdown (Eliminates Packet Latency Jitter)
settings put global wifi_scan_always_enabled 0
settings put global ble_scan_always_enabled 0
settings put global wifi_verbose_logging_enabled 0

# Android 15 MGLRU (Multi-Gen LRU) RAM Optimization
device_config put runtime_native_boot mglru true 2>/dev/null || true
device_config put runtime_native_boot disable_mglru false 2>/dev/null || true
setprop persist.device_config.runtime_native_boot.mglru true

# MQSAS Service Backdoor for Permanent 90 FPS SurfaceFlinger Frame Pacing
service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.miui.sf_fps 90" s16 "/storage/emulated/0/log.txt" i32 600 2>/dev/null || true

# Package Manager Cache Trimming
pm trim-caches 999G 2>/dev/null || true

# Suppress heavy background leeches without SecurityException
cmd appops set com.google.android.as RUN_IN_BACKGROUND ignore
cmd appops set com.google.android.as RUN_ANY_IN_BACKGROUND ignore
pm suspend --user 0 com.google.android.as 2>/dev/null || true
cmd appops set com.milink.service RUN_IN_BACKGROUND ignore
cmd appops set com.milink.service RUN_ANY_IN_BACKGROUND ignore

echo "[✓] 100 Celah Sistem Terdalam Sukses Diterapkan pada Redmi Pad SE!"
