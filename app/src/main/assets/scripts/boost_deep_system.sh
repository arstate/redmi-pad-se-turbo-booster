#!/system/bin/sh
# REDMI PAD SE EXTREME PERFORMANCE (HYPEROS / NON-ROOT ADB)

echo '[1/7] Mengunci CPU Clock & Bypass Thermal Throttling...'
cmd power set-fixed-performance-mode-enabled true
cmd thermalservice override-status 0

echo '[2/7] SQLite In-RAM Caching (Bypass Bottleneck eMMC 5.1 Fsync)...'
setprop debug.sqlite.syncmode OFF
setprop debug.sqlite.wal.syncmode OFF
setprop debug.sqlite.journalmode MEMORY
setprop debug.sqlite.wal.autocheckpoint 4000
setprop debug.sqlite.wal.poolsize 8
setprop debug.sqlite.wal.truncatesize 1048576
setprop debug.sqlite.idle_connection_timeout 120000
settings put global sqlite_compatibility_wal_flags 'compatibility_wal_supported=true,wal_syncmode=OFF'

echo '[3/7] F2FS Controller Maintenance & Storage TRIM...'
cmd device_config put storage_native_boot smart_idle_maint_enabled true
sm idle-maint run 2>/dev/null || true
sm fstrim 2>/dev/null || true

echo '[4/7] Manajemen RAM 4GB (Anti-Trim & Pangkas Cached 16)...'
cmd activity memory-factor set NORMAL
cmd device_config put activity_manager max_cached_processes 16
cmd device_config put activity_manager_native_boot modern_queue_enabled true
settings put global activity_manager_constants 'max_cached_processes=16,low_swap_threshold_percent=0.8,max_phantom_processes=32'
settings put global miui_memory_extension 0
settings put global ram_expand_size 0

echo '[5/7] Xiaomi Native Window Downscaler (0.7x Resolution @ Adreno 610)...'
cmd miui.downscale disable-downscale false
cmd miui.downscale set-debug-mode true
GAMES="com.tencent.ig com.mobile.legends com.roblox.client com.mojang.minecraftpe com.lemon.lvoverseas com.lemon.ccpadpro"
for PKG in $GAMES; do
    cmd miui.downscale add-debug-app $PKG 0.7 2>/dev/null
    cmd game mode 2 $PKG 2>/dev/null
    dumpsys deviceidle whitelist +$PKG 2>/dev/null
    cmd appops set $PKG RUN_IN_BACKGROUND allow 2>/dev/null
    cmd appops set $PKG RUN_ANY_IN_BACKGROUND allow 2>/dev/null
    am set-standby-bucket $PKG active 2>/dev/null
done

echo '[6/7] Realtime Binder, ADPF & Touch Deadzone 0...'
setprop debug.sf.enable_adpf_cpu_hint 1
setprop debug.sf.set_binder_thread_rt 1
setprop debug.hwui.use_hint_manager true
setprop debug.hwui.target_cpu_time_percent 90
cmd device_config put input_native_boot touch_deadzone_size 0
settings put global game_driver_all_apps 1
settings put global updatable_driver_all_apps 1

echo '[7/7] Isolasi I/O App Latar Belakang (Cegah Bus Lock eMMC)...'
HEAVY_APPS="com.ss.android.ugc.trill com.shopee.id com.tokopedia.tkpd com.instagram.android com.whatsapp com.whatsapp.w4b org.telegram.messenger com.netflix.mediaclient"
for APP in $HEAVY_APPS; do
    cmd appops set $APP RUN_IN_BACKGROUND ignore 2>/dev/null
    cmd appops set $APP RUN_ANY_IN_BACKGROUND ignore 2>/dev/null
    am set-standby-bucket $APP restricted 2>/dev/null
done

echo '=== SELESAI: SEMUA CELAH TERDALAM SUDAH AKTIF ==='
