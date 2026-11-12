# ⚡ AGENTS.md — Redmi Pad SE Turbo Booster Master System Architecture & Agent Handbook

> **Target Device**: Xiaomi Redmi Pad SE (Codename: `xun` / `xun_id`)  
> **Platform**: Qualcomm Snapdragon 680 4G (SM6225) | Adreno 610 GPU  
> **Operating System**: Xiaomi HyperOS 2.0 (Android 15 — Vanilla Ice Cream)  
> **Author**: Antigravity AI & arstate  
> **Repository**: [https://github.com/arstate/redmi-pad-se-turbo-booster](https://github.com/arstate/redmi-pad-se-turbo-booster)

---

## 1. Executive Summary & Architecture Overview

The **Redmi Pad SE Turbo Booster** is a rootless system tuning engine and standalone Android application built exclusively for the Xiaomi Redmi Pad SE (`xun`). The tablet features an 11.0-inch 90Hz IPS LCD (1200x1920) powered by Qualcomm Snapdragon 680 (4x Kryo 265 Gold @ 2.40 GHz + 4x Kryo 265 Silver @ 1.90 GHz) paired with 4GB LPDDR4X RAM and 128GB eMMC 5.1 storage.

### The Bottlenecks & Exploits Solved
1. **Slow eMMC 5.1 Storage Bottleneck**: eMMC 5.1 random write speeds hover around 20–30 MB/s. HyperOS's default "Memory Extension" (virtual RAM swap file written to flash storage) creates catastrophic UI micro-stutters and freeze frames. We disable flash swap entirely and reroute memory compaction directly into physical LPDDR4X zRAM (17 GB/s bandwidth).
2. **HyperOS Tier-1 Budget Throttling**: HyperOS internally classifies Redmi Pad SE as a budget device (`deviceLevelList "v:1,c:1,g:1"`), aggressively capping background processes, locking SurfaceFlinger frame pacing, and degrading UI transitions. We spoof Tier-3 Flagship status (`v:1,c:3,g:3` or `v:1,c:3,g:1,m:2`) and elevate CPU/GPU computility levels to Level 6 using the internal Xiaomi MQSAS backdoor.
3. **GPU Gaussian Blur Overhead**: Adreno 610 struggles with multi-layer window blurs on a 1200x1920 canvas. We disable window, desktop, dock, and notification blurs, reclaiming ~100% shader fillrate during gesture navigation.
4. **Gesture Navigation Lag**: The home gesture line incurs a 150–200ms discrimination buffer to distinguish horizontal multitasking swipes from vertical return swipes. Hiding the gesture line and reducing touch timeouts achieves true instant (0ms) swipe-to-home responsiveness.
5. **CapCut & Video Editor Resizing Crash**: Custom window downscaling engines crash EGL contexts in video editing apps (CapCut). We enforce native resolution exemption for video editing and grant full storage AppOps.

---

## 2. Display & Gesture Settings (Iron Rules)

### 2.1 Native Display Density (`wm density 205` @ 1200x1920)
- **Target Resolution**: Native `1200x1920` (WUXGA, 16:10 aspect ratio).
- **Target Density**: `wm density 205`.
  - At 205 dpi, the logical screen width in landscape is $1920 / (205 / 160) \approx 1498\text{ dp}$, and in portrait is $1200 / (205 / 160) \approx 936\text{ dp}$.
  - Any density higher than 260 dpi collapses HyperOS into phone UI mode (giant icons, cramped taskbar, phone layouts in apps).
  - `wm density 205` preserves pure Android tablet UI: spacious multi-column layout, permanent desktop dock/taskbar, true tablet navigation, and maximum canvas real estate for editing, coding, and multitasking.
- **Commands**:
  ```bash
  # Enforce native density
  adb shell wm size 1200x1920
  adb shell wm density 205

  # To reset back to factory default if needed:
  adb shell wm size reset
  adb shell wm density reset
  ```

### 2.2 Full-Screen Gesture Navigation (`force_fsg_nav_bar=1`) & Zero-Delay Swipe
To eliminate gesture delay and remove the 3-button navigation bar:
```bash
# Force Full Screen Gesture (FSG) navigation bar
adb shell settings put global force_fsg_nav_bar 1
adb shell settings put system force_fsg_nav_bar 1

# Eliminate the 150-200ms gesture discrimination delay
adb shell settings put global hide_gesture_line 1

# Set HyperOS Launcher animation curve to Fast / Elegant
adb shell settings put system miui_home_animation_rate 2

# Golden-ratio animation scales (0.35x or 0.0x for instant response)
adb shell settings put global window_animation_scale 0.35
adb shell settings put global transition_animation_scale 0.35
adb shell settings put global animator_duration_scale 0.35

# Cut touch hold latency and disable corner assistant hooks
adb shell settings put secure long_press_timeout 200
adb shell settings put secure multi_press_timeout 150
adb shell settings put system touch_response_speed 1
adb shell settings put system touch_gesture_speed 2
adb shell settings put secure assistant_on_nav_gesture 0
adb shell settings put secure search_press_hold_nav_handle 0
adb shell setprop vendor.perf.gestureflingboost.enable true
adb shell setprop persist.sys.turbosched.touch_boost true
```

### 2.3 CapCut & App Window Resizing Fix
When using downscalers or custom window sizes, CapCut (`com.lemon.lvoverseas` and `com.lemon.ccpadpro`) can experience EGL surface initialization errors, black video previews, or frozen resize handles.
- **Exempt from Downscaler**: Never apply `cmd miui.downscale add-debug-app` to CapCut packages!
- **Reset Game Mode**:
  ```bash
  adb shell cmd game reset com.lemon.lvoverseas
  adb shell cmd game reset com.lemon.ccpadpro
  ```
- **Grant Full Media Storage AppOps (Bypass Scoped Storage Latency)**:
  ```bash
  for PKG in com.lemon.lvoverseas com.lemon.ccpadpro; do
    adb shell cmd appops set $PKG READ_EXTERNAL_STORAGE allow
    adb shell cmd appops set $PKG WRITE_EXTERNAL_STORAGE allow
    adb shell cmd appops set $PKG MANAGE_EXTERNAL_STORAGE allow
    adb shell cmd appops set $PKG READ_MEDIA_IMAGES allow
    adb shell cmd appops set $PKG READ_MEDIA_VIDEO allow
    adb shell cmd appops set $PKG READ_MEDIA_AUDIO allow
    adb shell cmd appops set $PKG NO_ISOLATED_STORAGE allow
  done
  ```
- **Enable Freeform Multi-Window & Force Resizable**:
  ```bash
  adb shell settings put global enable_freeform_support 1
  adb shell settings put global force_resizable_activities 1
  adb shell settings put global enable_non_resizable_multi_window 1
  adb shell setprop persist.sys.freeform_window true
  ```

---

## 3. System Tweaks & Shell Scripts Architecture

The repository contains standalone shell scripts engineered for direct execution via ADB shell or Shizuku shell (`UID 2000`):

| Script | Primary Responsibility | Key Mechanism |
|---|---|---|
| `boost_100_celah_master.sh` | 100 deep system tweaks across 10 categories | SurfaceFlinger, HWUI SkiaGL threaded, ADPF, ART compilation, SQLite RAM journaling |
| `boost_pure_zram_compaction.sh` | Pure in-RAM zRAM compaction engine | Disables eMMC swap, sets Android 15 `CachedAppOptimizer` throttle to 5000ms |
| `fix_swipe_home_delay.sh` | Zero-delay swipe home & app opening | `hide_gesture_line 1`, `animation_scale 0.0`, `touch_response_speed 1` |
| `set_zram_8gb.sh` | zRAM 8GB memory expansion profile | MQSAS backdoor Call 21 injects `persist.sys.zram_size 8192` & `persist.miui.extm.bdsize 8192` |
| `boost_flagship_unfcker.sh` | Tier-3 Flagship spoofing & MQSAS boost | `deviceLevelList "v:1,c:3,g:3"`, MQSAS computility 6, disables desktop/dock blurs |
| `boost_oppo_a60_master.sh` | Snapdragon 680 companion profile (CPH2631) | 90Hz lock, Touch sampling tuning, Adreno 610 driver stabilization |
| `boost_redmi_note_10_pro.sh` | Snapdragon 732G companion profile (sweet) | MIUI 14 thermal bypass, 120Hz lock, Joyose suppression |
| `stress_test_5min.py` | 5-Minute automated ADB telemetry & stress tester | Evaluates CPU frequency locks, RAM compaction rates, and SurfaceFlinger frame drops |

### 3.1 Breakdown of the 100 System Tweaks (`boost_100_celah_master.sh`)
1. **Category 1: SurfaceFlinger & Compositor Pipeline (1–10)**:
   - `debug.sf.disable_client_composition_cache 1`, `debug.sf.disable_backpressure 1`, `debug.sf.drop_missed_frames 1`.
   - `vulkan_renderengine true`, `multithreaded_present true`, `fifo_priority_for_major_ui_processes true`.
2. **Category 2: HWUI RenderThread & Skia Engine (11–20)**:
   - `debug.hwui.renderer skiagl`, `debug.renderengine.backend skiaglthreaded`.
   - `debug.hwui.use_gpu_pixel_buffers true`, `debug.hwui.target_cpu_time_percent 90`.
   - `debug.sqlite.wal.syncmode OFF`.
3. **Category 3: Qualcomm Bengal / Adreno 610 Driver Stabilization (21–30)**:
   - Delete buggy ANGLE driver overrides that cause Mobile Legends / Unity crashes.
   - SurfaceFlinger buffer latching (`debug.sf.latch_unsignaled 1`, `debug.sf.enable_hwc_vds 1`).
   - Android Phantom Process Killer bypass (`max_phantom_processes 2147483647`).
   - Suppress tracking daemons (`com.miui.analytics`, `com.miui.msa.global`, `com.xiaomi.metok-sdk`, `com.miui.daemon`).
4. **Category 4: Display Refresh Rate & Panel Governance (31–40)**:
   - Lock permanent 90Hz (`peak_refresh_rate 90.0`, `min_refresh_rate 90.0`, `fps_limit 90`, `thermal_limit_refresh_rate 0`).
5. **Category 5: ADPF (Android Dynamic Performance Framework) & PowerHAL (41–50)**:
   - GPU actual work duration reporting, FMQ eager send, HWUI GPU hints.
6. **Category 6: ART Runtime, Dexopt & Ahead-Of-Time (AOT) Compilation (51–60)**:
   - `cmd package compile -m speed -f com.android.systemui` & `com.android.settings`.
   - `reg_alloc_no_output_overlap true`, `compact_object_header true`.
7. **Category 7: Memory Architecture & LMKD Tuning (61–70)**:
   - Virtual RAM disabled (`isExternalRamOn 0`, `ram_expand_size 0`).
   - `activity_manager use_compaction true`, `compact_proactive true`, `memory-factor set NORMAL`.
8. **Category 8: Storage I/O & SQLite In-Memory Caching (71–80)**:
   - `sqlite_allow_temp_tables true`, `smart_idle_maint_enabled true`, `fstrim`.
9. **Category 9: Proprietary Xiaomi HyperOS 2.0 TurboSched (81–90)**:
   - `debug.turbosched.enable true`, `turbo_sched_core_app_list` for high-priority apps.
   - Force system `high_performance` power mode.
10. **Category 10: Tablet Touch Digitizer & Power Management (91–100)**:
    - `cmd thermalservice override-status 0` (Bypass thermal downclocking).
    - `cmd power set-fixed-performance-mode-enabled true` (Lock 2.4GHz / 1.9GHz).
    - Radio jitter elimination (`wifi_scan_always_enabled 0`, `ble_scan_always_enabled 0`).
    - Android 15 MGLRU activation (`runtime_native_boot mglru true`).
    - MQSAS Call 21 permanent SurfaceFlinger 90 FPS lock.

---

## 4. TurboBooster Android Application Architecture

The `app/` directory houses a native Kotlin Android Studio project:
- **Build System**: Android Gradle Plugin 8.4.2, Kotlin 1.9.24, Gradle 8.7.
- **SDK Targets**: `minSdk = 26`, `targetSdk = 35` (Android 15), `compileSdk = 36`.
- **Package Name**: `com.arya.turbobooster`.

### 4.1 Key Code Structure & Components
```
app/src/main/
├── AndroidManifest.xml                  # Permissions, Services, Receivers, Shizuku Provider
├── assets/
│   ├── audio/kuntilanak_seram.mp3       # Audio payload for audio testing service
│   └── scripts/                         # 12 Bundled performance & gaming shell scripts
│       ├── boost_100.sh
│       ├── boost_chrome_webview.sh
│       ├── boost_deep_system.sh
│       ├── boost_extreme.sh
│       ├── boost_flagship_unfcker.sh
│       ├── boost_pure_zram_compaction.sh
│       ├── boost_quad_audio.sh
│       ├── boost.sh
│       ├── fix_swipe_home_delay.sh
│       ├── launch_freeform.sh
│       ├── pubg_ultimate_booster.sh
│       └── set_zram_8gb.sh
└── java/com/arya/turbobooster/
    ├── AudioPlayerService.kt            # Background service playing audio feedback at max volume (supports embedded asset & custom path)
    ├── MainActivity.kt                  # Real-time dashboard, Shizuku listener, script & display quick-action dialog
    ├── booster/
    │   ├── AudioBoosterManager.kt       # Quad-speaker tuning, Dolby Atmos & Harman curve unlock
    │   ├── DeviceMetrics.kt             # Real-time CPU, RAM, thermal, and refresh rate reader
    │   ├── ScriptInstaller.kt           # Asset extractor to app private files / /sdcard/TurboBoosterScripts
    │   ├── ShellExecutor.kt             # Shizuku IPC bridge via reflection (Shizuku.newProcess)
    │   └── TurboBoosterManager.kt       # 21-step Super Performance Mode & Stock Mode orchestrator
    └── receiver/
        ├── BootReceiver.kt              # On boot: auto-activates Turbo Mode; delegates PLAY_AUDIO intent to AudioPlayerService
        └── TurboBoosterTileService.kt   # Android Quick Settings Tile for 1-tap Turbo toggle
```

### 4.2 Shizuku Provider Integration (`rikka.shizuku.ShizukuProvider`)
In `AndroidManifest.xml`:
```xml
<provider
    android:name="rikka.shizuku.ShizukuProvider"
    android:authorities="com.arya.turbobooster.shizuku"
    android:multiprocess="false"
    android:enabled="true"
    android:exported="true"
    android:permission="android.permission.INTERACT_ACROSS_USERS_FULL" />
```
- The app binds to the Shizuku server binder without requiring root.
- `ShellExecutor.kt` interacts with `Shizuku.newProcess()` using reflection, providing standard `stdin`, `stdout`, and `stderr` streams under `UID 2000` (shell privileges).

### 4.3 Quick Settings Tile (`TurboBoosterTileService`)
- Declared with `android.permission.BIND_QUICK_SETTINGS_TILE`.
- When added to the HyperOS Control Center:
  - Displays state: `Turbo: ON` (Active / Highlighted) or `Turbo: OFF` (Inactive).
  - Tapping toggles `applySuperPerformanceMode` or `applyStockMode` asynchronously on `Dispatchers.IO`.

### 4.4 Audio Testing Service (`AudioPlayerService`)
- Manages audio stream volume elevation (`STREAM_MUSIC` to maximum).
- Plays embedded asset audio (`audio/kuntilanak_seram.mp3`) with automatic completion cleanup (or accepts optional custom path via `Intent.putExtra("path", ...)`).
- Can be triggered externally via broadcast:
  ```bash
  # Play default embedded asset:
  adb shell am broadcast -a com.arya.turbobooster.PLAY_AUDIO

  # Or play specific file path:
  adb shell am broadcast -a com.arya.turbobooster.PLAY_AUDIO --es path "/sdcard/sound.mp3"
  ```

### 4.5 Release Artifacts
- Binary output: `app/build/outputs/apk/release/app-release.apk`
- Tracked git binary: `release/TurboBooster.apk` (Signed with debug signing key for seamless test installs).

---

## 5. Master Workflows for Next AI Agents

When instructed to work on this repository, subsequent agents must follow these standardized protocols:

### Workflow 1: Building the Android App
```bash
cd "/mnt/HDD-2/BACKUP LINUX/FILE DATA/REDMI PAD SE BOOST"

# 1. Compile and package the release APK
./gradlew assembleRelease

# 2. Sync built APK to the release folder
cp app/build/outputs/apk/release/app-release.apk release/TurboBooster.apk

# 3. Verify APK size
ls -la release/TurboBooster.apk
```

### Workflow 2: Wireless ADB Pairing & Deployment
```bash
# 1. Pair via Wireless Debugging (check tablet screen for 6-digit PIN and Port)
adb pair 192.168.x.x:<PAIRING_PORT> <PIN>

# 2. Connect to device
adb connect 192.168.x.x:<CONNECT_PORT>

# 3. Install TurboBooster APK
adb install -r release/TurboBooster.apk

# 4. Verify Shizuku service is running
adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh
```

### Workflow 3: Injecting Master Shell Scripts
Execute scripts directly over ADB:
```bash
# 1. Zero-delay swipe home
adb shell < fix_swipe_home_delay.sh

# 2. 100 deep system tweaks
adb shell < boost_100_celah_master.sh

# 3. Pure zRAM compaction
bash boost_pure_zram_compaction.sh

# 4. Configure zRAM 8GB
bash set_zram_8gb.sh 8192
```

### Workflow 4: The MQSAS Backdoor Exploit Protocol
HyperOS protects several `persist.sys.*` properties from regular shell modification. However, the proprietary `miui.mqsas` service (`IMQSNative`) runs as UID 0 (root) and exposes an unauthenticated transaction `21`:
```bash
# Syntax:
# service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "<PROPERTY_NAME> <VALUE>" s16 "/dev/null" i32 600

# Example: Overriding SurfaceFlinger FPS to 90
adb shell service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.miui.sf_fps 90" s16 "/dev/null" i32 600

# Example: Disabling system blurs
adb shell service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "persist.sys.miui.blur.enable false" s16 "/dev/null" i32 600
```

### Workflow 5: Restoring Debloated Packages
If debloated packages need to be restored:
```bash
RESTORE_LIST="com.google.android.apps.messaging com.milink.service com.xiaomi.aicr com.miui.aod com.android.updater com.google.android.as"
for PKG in $RESTORE_LIST; do
  adb shell cmd package install-existing $PKG
  adb shell cmd appops set $PKG RUN_IN_BACKGROUND allow
done
```

---

## 6. Known HyperOS Quirks & Troubleshooting Matrix

| Issue | Root Cause | Solution |
|---|---|---|
| Refresh rate drops to 60Hz when playing games or video | HyperOS `PowerKeeper` or `Joyose` dynamically enforces 60Hz limits based on thermal state | Re-run: `cmd appops set com.miui.powerkeeper WRITE_SETTINGS ignore` and `cmd thermalservice override-status 0`. |
| Shizuku says "Not Running" after tablet reboot | Android kills ADB wireless debugging daemons on reboot | Re-open Shizuku and reconnect via Wireless Debugging, or start via PC ADB: `adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh`. |
| CapCut crashes during export or timeline scrubbing | Resolution downscaler was accidentally applied to CapCut | Run `cmd game reset com.lemon.lvoverseas` and verify storage AppOps are set to `allow`. |
| UI icons appear gigantic / taskbar disappears | Screen density was set above 260 dpi | Run `adb shell wm density 205` to force full tablet UI. |
| Home gesture has delay when swiping up | Navigation bar gesture line is enabled | Run `adb shell settings put global hide_gesture_line 1`. |

---
*Maintained by the Antigravity Autonomous Agent Swarm for Redmi Pad SE optimization.*
