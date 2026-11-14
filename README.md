# ⚡ Redmi Pad SE Turbo Booster & HyperOS Optimization Suite

[![Platform](https://img.shields.io/badge/Device-Xiaomi%20Redmi%20Pad%20SE%20(xun)-orange.svg)](https://www.mi.com)
[![SoC](https://img.shields.io/badge/SoC-Qualcomm%20Snapdragon%20680-blue.svg)](https://www.qualcomm.com)
[![OS](https://img.shields.io/badge/OS-HyperOS%202.0%20%7C%20Android%2015-green.svg)](https://android.com)
[![Privilege](https://img.shields.io/badge/Access-Rootless%20Shizuku%20UID%202000-purple.svg)](https://shizuku.rikka.app)
[![License](https://img.shields.io/badge/License-MIT-brightgreen.svg)](LICENSE)

A standalone, rootless performance optimizer, kernel-level tuning suite, and native Android application engineered specifically for the **Xiaomi Redmi Pad SE** (Codename: `xun` / `xun_id`, Snapdragon 680 SM6225, Adreno 610, HyperOS 2.0 / Android 15).

Powered by the **Shizuku API (UID 2000 Shell Privileges)** and proprietary Xiaomi **MQSAS Backdoor Exploits**, allowing users to toggle **Super Performance Mode** ON/OFF directly on the tablet without requiring PC tethering or Magisk/KernelSU root.

---

## 📱 Hardware & Benchmark Baseline

| Parameter | Factory Stock Specification | Turbo Booster Target (v2.0) |
|---|---|---|
| **SoC** | Qualcomm Snapdragon 680 SM6225 | Dynamic Burst to 2.40 GHz Kryo Gold + 1.90 GHz Silver (No fixed cap) |
| **GPU** | Adreno 610 (Budget GPU) | Flagship Animation Physics (`c:3`) without GPU Blur Choke (`g:1`) |
| **RAM** | 4GB LPDDR4X (Slow flash swap) | Pure in-RAM ZRAM Compaction + Cgroup v2 Freezer (Zero eMMC thrashing) |
| **Storage** | 128GB eMMC 5.1 (~20MB/s random write) | Safe In-Memory WAL SQLite (`debug.sqlite.journalmode WAL`, `syncmode NORMAL`) |
| **Display** | 11.0" IPS LCD, 1200x1920 @ 90Hz | Locked 90Hz (`refresh_rate_mode 2`, `peak_refresh_rate 90.0`) |
| **Density** | Variable (often drops to phone UI) | Locked **`wm density 205`** (True Android tablet desktop layout) |
| **Navigation** | 3-button or delayed gesture bar | **`force_fsg_nav_bar=1`** + `hide_gesture_line=1` (Instant 0ms swipe) |

---

## 🚀 Key Features & Architectural Modules (v2.0 Zero-Placebo Arsenal)

### 1. ⚡ Flagship Balanced Mode (Default Profile)
* **SM6225 Gold Core Affinity**: Directs SurfaceFlinger and HyperOS animation scheduling to Gold Kryo cores 4-7 (`persist.sys.miui.sf_cores 4-7`, `persist.vendor.display.miui.composer_boost 4-7`).
* **Android 15 Cgroup v2 Freezer & In-RAM Compaction**: Locks 16 cached processes sweet spot for 4GB RAM (`device_config put activity_manager max_cached_processes 16`).
* **Safe SQLite WAL Acceleration**: Safe WAL mode (`journalmode WAL` & `syncmode NORMAL`) eliminating corruption risk while speeding up app database writes.
* **Pure ZRAM Compaction**: Decommissions destructive 8GB virtual swap extensions (`persist.miui.extm.enable 0`) to preserve eMMC 5.1 life and keep Linux page cache alive.
* **Flagship Animation Physics without Blur Choke**: Enforces `deviceLevelList "v:1,c:3,g:1,m:2"` (unlocks smooth flagship animations `c:3` while disabling GPU blur choking on Adreno 610 `g:1`).
* **Hybrid Rust Daemon (`libturbodaemon.so`)**: Persistent ~1.5 MB RSS UID-2000 native daemon with direct `rsbinder` IPC for MQSAS transaction 21, automated 90Hz guard, and screen-off gentle compaction.
* **Surgical Debloat & Undead Killer**: Hardcoded 13 core packages in Red-List protection; uninstalls green-list bloat and immediately crashes undead `-800 PERSISTENT` daemons from memory.

### 2. 🎮 Extreme Gaming & Render Mode
* Thermal limit override (`cmd thermalservice override-status 0`).
* Freezes Joyose game throttling daemon.
* Ultra snappy 0.25x animation scale and 0.8x native resolution downscaling for heavy 3D titles.
* Automated game session isolation: suspends background hogs when target games/CapCut enter foreground, restores cleanly on exit.

### 3. ↺ Stock Mode
* Restores factory stock settings, Blurs, and Tier 1 device level (`v:1,c:1,g:1,m:1`).
* Resets thermal governance to hardware sensors.
* Restores default 1.0x animation scales and auto refresh rate.
* Stops native daemon and cleans session state.

### 4. 🔄 Debloat Restore
* 1-tap reinstallation of all debloated packages for user 0 via `cmd package install-existing`.

---

## 🖥️ Display & Navigation Guidelines

### Native Tablet Density (`wm density 205` @ 1200x1920)
To avoid oversized phone icons and cramped docks, the tablet requires:
```bash
adb shell wm size 1200x1920
adb shell wm density 205
```
* At 205 dpi, the logical screen width reaches ~936dp in portrait and ~1498dp in landscape, triggering full tablet/desktop multi-column interfaces in HyperOS and all third-party apps.

### Zero-Delay Full-Screen Gesture Navigation
```bash
# Force Full Screen Gestures (FSG)
adb shell settings put global force_fsg_nav_bar 1
adb shell settings put system force_fsg_nav_bar 1

# Remove gesture indicator bar to eliminate 200ms swipe discrimination buffer
adb shell settings put global hide_gesture_line 1

# Ultra-responsive launcher physics and touch speeds
adb shell settings put system miui_home_animation_rate 2
adb shell settings put global window_animation_scale 0.35
adb shell settings put system touch_response_speed 1
adb shell settings put system touch_gesture_speed 2
adb shell settings put secure assistant_on_nav_gesture 0
adb shell settings put secure search_press_hold_nav_handle 0
```

### CapCut & Window Resizing Compatibility
To prevent EGL surface errors and crashes during video timeline editing or window resizing:
* **Never apply resolution downscaling to CapCut** (`com.lemon.lvoverseas` / `com.lemon.ccpadpro`).
* Ensure media AppOps are fully allowed:
  ```bash
  for PKG in com.lemon.lvoverseas com.lemon.ccpadpro; do
    adb shell cmd game reset $PKG
    adb shell cmd appops set $PKG READ_EXTERNAL_STORAGE allow
    adb shell cmd appops set $PKG WRITE_EXTERNAL_STORAGE allow
    adb shell cmd appops set $PKG MANAGE_EXTERNAL_STORAGE allow
    adb shell cmd appops set $PKG READ_MEDIA_IMAGES allow
    adb shell cmd appops set $PKG READ_MEDIA_VIDEO allow
    adb shell cmd appops set $PKG READ_MEDIA_AUDIO allow
    adb shell cmd appops set $PKG NO_ISOLATED_STORAGE allow
  done
  ```
* Enable freeform multi-window support:
  ```bash
  adb shell settings put global enable_freeform_support 1
  adb shell settings put global force_resizable_activities 1
  adb shell settings put global enable_non_resizable_multi_window 1
  ```

---

## 📂 Repository File Structure

```
├── AGENTS.md                          # Comprehensive master architecture & agent handbook
├── README.md                          # This user & developer guide
├── app/                               # Native Android application source code
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml        # Includes ShizukuProvider & TileService
│       ├── assets/
│       │   ├── audio/                 # Audio payload assets
│       │   └── scripts/               # 12 Bundled performance & gaming shell scripts
│       └── java/com/arya/turbobooster/
│           ├── AudioPlayerService.kt  # Audio feedback service (embedded asset & custom path)
│           ├── MainActivity.kt        # Telemetry UI, script picker, tablet display & gesture toggles
│           ├── booster/               # ShellExecutor, TurboBoosterManager, AudioBoosterManager
│           └── receiver/              # BootReceiver & TurboBoosterTileService
├── boost_100_celah_master.sh          # 100 deep system tweaks across 10 categories
├── boost_pure_zram_compaction.sh      # Pure in-RAM zRAM compaction engine (anti-eMMC swap)
├── fix_swipe_home_delay.sh            # Zero-delay swipe home and instant launcher transitions
├── set_zram_8gb.sh                    # Injects 8192 MB zRAM parameters via MQSAS Call 21
├── boost_flagship_unfcker.sh          # Tier-3 flagship spoofing & computility booster
├── boost_oppo_a60_master.sh           # Companion Snapdragon 680 tuner for OPPO A60
├── boost_redmi_note_10_pro.sh         # Companion Snapdragon 732G tuner for Redmi Note 10 Pro
├── stress_test_5min.py                # Python ADB automated stress test & telemetry reader
└── release/
    └── TurboBooster.apk               # Pre-built release APK
```

---

## 🛠️ Installation & Quick Start

### Method 1: Using the Android App (Recommended)
1. **Prerequisite**: Install and activate [Shizuku](https://shizuku.rikka.app/) on your Redmi Pad SE via Wireless Debugging.
2. **Install APK**: Download [`release/TurboBooster.apk`](release/TurboBooster.apk) and install it on your tablet:
   ```bash
   adb install -r release/TurboBooster.apk
   ```
3. **Grant Shizuku Permission**: Open **Turbo Booster** and grant permission when prompted.
4. **Activate Turbo**:
   - Tap **⚡ 1-KLIK AKTIFKAN SEMUA** to automatically run all system performance optimizations (Extreme 90 FPS lock, Rust daemon IPC, surgical debloat with smart skip, incremental AOT speed compilation, and auto-apply on boot).
   - Audio Quad-Speaker Studio remains cleanly isolated in its own card.
   - Live hardware telemetry monitors RAM, ZRAM, CPU temperature, and daemon RSS memory in real time!
   - Or add the **Turbo Booster** tile to your Quick Settings / Control Center for instant 1-tap toggling!

### Method 2: Direct Execution via ADB Shell
Run individual optimization scripts from your computer or terminal:
```bash
# 1. Apply zero-delay swipe navigation
adb shell < fix_swipe_home_delay.sh

# 2. Inject 100 deep system tweaks
adb shell < boost_100_celah_master.sh

# 3. Enable pure zRAM in-memory compaction
bash boost_pure_zram_compaction.sh

# 4. Expand zRAM buffer to 8GB
bash set_zram_8gb.sh 8192
```

---

## 💻 Building from Source

Requires **JDK 17+** and **Android SDK 35/36**.

```bash
git clone https://github.com/arstate/redmi-pad-se-turbo-booster.git
cd redmi-pad-se-turbo-booster

# Build release APK
./gradlew assembleRelease

# The generated APK is at:
# app/build/outputs/apk/release/app-release.apk
```

---

## 📑 Next Agent Guide

For detailed explanations of kernel parameters, MQSAS backdoor transaction numbers, Shizuku reflection internals, and troubleshooting procedures, please consult [`AGENTS.md`](AGENTS.md).

---

## 📄 License

MIT License. Engineered for personal optimization, performance research, and educational purposes on Xiaomi Redmi Pad SE.
