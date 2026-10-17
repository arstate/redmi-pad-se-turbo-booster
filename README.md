# ⚡ Redmi Pad SE Turbo Booster

A standalone, rootless performance optimizer and system tuning app built specifically for the **Xiaomi Redmi Pad SE** (Snapdragon 680 / Adreno 610 / HyperOS 2.0 / Android 15).

Powered by the **Shizuku API (UID 2000 Shell Privileges)**, allowing users to toggle **Super Performance Mode** ON/OFF directly on the tablet without connecting to a PC after reboot.

---

## 🚀 Key Features

### ⚡ Super Performance Mode (ON)
1. **CPU Clock Lock**:
   - Locks 4x Kryo 265 Gold cores to **2.40 GHz** and 4x Silver cores to **1.90 GHz** via Android `PowerManager.setFixedPerformanceMode(true)`.
2. **Framework Thermal Bypass**:
   - Bypasses HyperOS thermal throttling and FPS drops via `cmd thermalservice override-status 0`.
3. **In-RAM SQLite Journaling (NVMe-like Latency)**:
   - Sets SQLite journal mode to `MEMORY` and sync mode to `OFF` (`debug.sqlite.journalmode MEMORY`), bypassing the slow eMMC 5.1 flash bottleneck for lightning-fast database writes (558 MB/s cached vs 143 MB/s flash).
4. **Display & SurfaceFlinger Optimization**:
   - Enforces permanent **90Hz refresh rate** (`refresh_rate_mode 2`).
   - Disables GPU-heavy Gaussian Window Blurs (`disable_window_blurs 1`).
   - Enables Realtime Binder (`debug.sf.set_binder_thread_rt 1`) and ADPF CPU Hints (`debug.sf.enable_adpf_cpu_hint 1`).
5. **RAM 4GB Guard & Cached Process Clamping**:
   - Clamps max cached background processes to 16 to eliminate ZRAM/Swap thrashing.
   - Activates `activity_manager_native_boot modern_queue_enabled` to prevent background broadcast storms.
6. **Native Window Downscaling (0.7x Resolution)**:
   - Utilizes HyperOS Window Downscaler (`cmd miui.downscale`) for games (`com.tencent.ig`, `com.mobile.legends`, `com.roblox.client`, `com.mojang.minecraftpe`, `com.lemon.lvoverseas`).
   - Reduces fragment shader load by ~51% with Qualcomm display hardware upscaling to FHD+ 90Hz.
7. **Chrome & WebView Anti-Tab-Reload**:
   - Injects Chromium command-line flags to prevent background tabs from reloading, unthrottles 60Hz limit, and downscales Chrome buffer via Hardware DPU (0.85x).

### ↺ Stock Mode (OFF)
- Restores dynamic CPU frequency scaling (down to 691 MHz / 1766 MHz).
- Resets thermal framework to dynamic status.
- Restores default window animation scales (1.0x).
- Reverts downscaling and Chrome interventions.

---

## 🛠️ How to Install & Use

1. **Prerequisite**:
   - Install and start [Shizuku](https://shizuku.rikka.app/) on your Redmi Pad SE via Wireless Debugging.
2. **Install APK**:
   - Download [`release/TurboBooster.apk`](release/TurboBooster.apk) from this repository and install it on your tablet.
3. **Usage**:
   - Open **Turbo Booster**.
   - Grant Shizuku permissions when prompted.
   - Tap **⚡ Apply Turbo** to activate Super Performance Mode.
   - Tap **↺ Reset Stock** whenever you want to return to normal battery-saver mode.
   - Use the **ADB Guide** button in the top bar for 1-tap shortcuts to Shizuku, Developer Options, or to copy the LADB start script.

---

## 💻 Building from Source

Requires Android SDK 35 (VanillaIceCream) and JDK 17+.

```bash
git clone https://github.com/arstate/redmi-pad-se-turbo-booster.git
cd redmi-pad-se-turbo-booster
./gradlew assembleRelease
```

Output APK will be generated at `app/build/outputs/apk/release/app-release.apk`.

---

## 📄 License

MIT License. Designed for personal optimization and educational purposes on Xiaomi Redmi Pad SE.
