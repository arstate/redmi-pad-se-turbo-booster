#!/system/bin/sh
# ==============================================================================
# REDMI PAD SE: ULTIMATE CHROME & ANDROID SYSTEM WEBVIEW PERFORMANCE BOOSTER
# ==============================================================================

echo "[1/4] Mengaktifkan Akses Flag Chrome via debug_app..."
settings put global debug_app com.android.chrome

echo "[2/4] Menginjeksi Modern Flags Chrome (Anti-Reload Tab & 0.78x DIP Raster)..."
cat << 'CHROME_EOF' > /data/local/tmp/chrome-command-line
chrome --remote-debugging-port=9222 --disable-renderer-backgrounding --disable-backgrounding-occluded-windows --disable-background-timer-throttling --disable-low-end-device-mode --disable-layer-tree-host-memory-pressure --disable-composited-antialiasing --process-per-site --num-raster-threads=4 --enable-zero-copy --force-gpu-mem-discardable-limit-mb=512 --gpu-program-cache-size-kb=16384 --gpu-rasterization-msaa-sample-count=0 --force-device-scale-factor=1.25 --check-damage-early --enable-smooth-scrolling --enable-features=DisableTabDiscarding,BackForwardCache,ForceForegroundPriorityForAllTabs,TransientKeepAlivePolicy,ParallelDownloading --disable-features=BackForwardCacheMemoryControls,UrgentPageDiscarding,DiscardFrozenBrowsingInstancesWithGrowingPMF,DiscardOnCommitLimit,InfiniteTabsFreezing,InfiniteTabsFreezingOnMemoryPressure,ThrottleMainFrameTo60Hz,ThrottleMainFrameTo60HzDesktopAndroid,ThrottleMainFrameTo60HzWebView
CHROME_EOF

chmod 777 /data/local/tmp/chrome-command-line

echo "[3/4] Hardware DPU Resolution Downscaling & Prioritas OS..."
cmd game set --downscale 0.85 com.android.chrome 2>/dev/null || true
cmd game mode custom com.android.chrome 2>/dev/null || true
dumpsys deviceidle whitelist +com.android.chrome 2>/dev/null || true
cmd appops set com.android.chrome RUN_IN_BACKGROUND allow 2>/dev/null || true
cmd appops set com.android.chrome RUN_ANY_IN_BACKGROUND allow 2>/dev/null || true
am set-standby-bucket com.android.chrome active 2>/dev/null || true

echo "[4/4] Mengaktifkan Vulkan SkiaVK & ADPF Hint Manager untuk WebView..."
setprop debug.hwui.renderer skiavk
setprop debug.hwui.use_hint_manager true
setprop debug.hwui.skip_empty_damage true
setprop debug.hwui.use_partial_updates true
setprop debug.hwui.use_gpu_pixel_buffers true
setprop debug.hwui.webview_overlays_enabled true

# Restart Chrome untuk memuat seluruh konfigurasi baru
am force-stop com.android.chrome
monkey -p com.android.chrome -c android.intent.category.LAUNCHER 1 2>/dev/null || true

echo "=== SUKSES: CHROME & WEBVIEW SUDAH MAKSIMAL & ANTI-RELOAD! ==="
