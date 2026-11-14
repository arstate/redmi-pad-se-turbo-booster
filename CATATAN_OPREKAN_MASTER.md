# 📋 CATATAN MASTER OPREKAN — REDMI PAD SE & ANDROID CRAFT SUITE

> **Tanggal Pembuatan**: 14 November 2026  
> **Target Device**: Xiaomi Redmi Pad SE (Codename: `xun` / `xun_id`)  
> **Platform SoC**: Qualcomm Snapdragon 680 4G (SM6225) 6nm — Adreno 610 GPU  
> **Sistem Operasi**: Xiaomi HyperOS 2.0 (Android 15 — Vanilla Ice Cream)  
> **Akses Rootless**: Wi-Fi ADB (Port dinamis mDNS) + Shizuku API (UID 2000 Shell Privileges)  
> **Repository GitHub**: [https://github.com/arstate/redmi-pad-se-turbo-booster](https://github.com/arstate/redmi-pad-se-turbo-booster)  
> **Mirror Download Publik**: [Catbox Direct (v2.1.3 2.1MB)](https://files.catbox.moe/1kwofs.apk) | [Gofile Portal (v2.1.3 2.1MB)](https://gofile.io/d/LTsDE2cw) | [GitHub Raw Direct](https://raw.githubusercontent.com/arstate/redmi-pad-se-turbo-booster/main/release/TurboBooster.apk)

---

## 📑 DAFTAR ISI
1. [Spesifikasi Hardware & Analisis Masalah Bawaan](#1-spesifikasi-hardware--analisis-masalah-bawaan)
2. [Aplikasi 1: Redmi Pad SE Turbo Booster v1.3.5](#2-aplikasi-1-redmi-pad-se-turbo-booster-v135)
3. [Daftar 100 Celah Oprekan Sistem (boost_100_celah_master.sh)](#3-daftar-100-celah-oprekan-sistem-boost_100_celah_mastersh)
4. [Kompaksi Memori & Solusi zRAM 8GB murni di RAM Fisik](#4-kompaksi-memori--solusi-zram-8gb-murni-di-ram-fisik)
5. [Tampilan Desktop Tablet (205 DPI) & Navigasi Gestur 0ms](#5-tampilan-desktop-tablet-205-dpi--navigasi-gestur-0ms)
6. [Solusi Crash Video Editor CapCut & Multi-Window](#6-solusi-crash-video-editor-capcut--multi-window)
7. [Ekosistem Android Craft: WordCraft, FigCraft & LightCraft](#7-ekosistem-android-craft-wordcraft-figcraft--lightcraft)
8. [Daftar File Project, Skrip Shell & Struktur Direktori](#8-daftar-file-project-skrip-shell--struktur-direktori)
9. [Alur Build, Compile & Deployment ADB untuk Agent Masa Depan](#9-alur-build-compile--deployment-adb-untuk-agent-masa-depan)

---

## 1. Spesifikasi Hardware & Analisis Masalah Bawaan

| Komponen | Spesifikasi Pabrik | Bottleneck Bawaan HyperOS | Solusi Oprekan Kita |
|---|---|---|---|
| **SoC** | Snapdragon 680 4G (4x 2.4GHz Gold + 4x 1.9GHz Silver) | Dynamic throttling & drop clock saat thermal naik | Dynamic governor tanpa fixed-perf cap, Gold Core affinity 4-7 untuk SurfaceFlinger & Animator |
| **GPU** | Adreno 610 | Driver ANGLE bawaan sering crash di MLBB/Unity; render blur berat | Hapus override ANGLE, switch backend ke `skiaglthreaded`, matikan window & dock blurs |
| **Storage** | 128GB eMMC 5.1 (~20-30 MB/s random write) | Fitur "Memory Extension" menulis swap ke flash eMMC bikin UI freeze | Matikan total swap flash eMMC (`persist.miui.extm.enable 0`, `isExternalRamOn 0`), alihkan swap murni ke zRAM LPDDR4X (17 GB/s) |
| **RAM** | 4GB LPDDR4X | Sering reload aplikasi karena budget tier HyperOS pelit | Spoofing Tier-3 Flagship via MQSAS Call 21 (`v:1,c:3,g:1,m:2`), aktifkan Android 15 MGLRU & Cgroup v2 freezer |
| **Layar** | 11.0" IPS LCD 1200x1920 90Hz | Sering drop ke 60Hz dinamis; DPI bawaan (270+) bikin tampilan mirip HP melar | Kunci permanen 90Hz (`fps_limit 90`, `sf_fps 90`), set resolusi native ke `wm density 205` (tampilan tablet desktop) |
| **Navigasi** | Tombol 3 titik atau gestur bergaris | Ada jeda 150-200ms pada garis gestur bawah | Aktifkan `force_fsg_nav_bar=1` & `hide_gesture_line=1` (0ms instant swipe-to-home) |

---

## 2. Aplikasi 1: Redmi Pad SE Turbo Booster v2.0.0 (Master Brief v2.0)

- **Package Name**: `com.arya.turbobooster`
- **Versi**: `versionCode = 10`, `versionName = "2.0.0"` (Master Engineering Brief v2.0).
- **Lokasi Project**: `/mnt/HDD-2/BACKUP LINUX/FILE DATA/REDMI PAD SE BOOST/`
- **File Binary**: `/mnt/HDD-2/BACKUP LINUX/FILE DATA/REDMI PAD SE BOOST/release/TurboBooster.apk`
- **Fitur & Arsitektur Utama**:
  1. **Rust Native Daemon (`turbodaemon` / `libturbodaemon.so`)**: Daemon UID 2000 berkecepatan native di jniLibs/arm64-v8a yang berkomunikasi lewat Abstract Unix Domain Socket (`@turbodaemon_ipc`) dengan token auth, 90Hz guard, screen-off gentle memory reclaimer, dan session caging game/CapCut.
  2. **4 Reversible Profile State Transitions (`STOCK`, `FLAGSHIP_BALANCED`, `EXTREME_GAMING_RENDER`, `DEBLOAT_RESTORE`)**: Transisi profil tanpa reboot, terstruktur dan dapat dibalikkan secara aman.
  3. **10-Layer Real System Arsenal**: MQSAS Call 21 persistent properties (Gold core 4-7 affinity), Android 15 device_config kill-switches, Cgroup v2 freezer (16 cached processes sweet spot), lmkd heaviest-task, SQLite WAL/NORMAL.
  4. **Surgical Debloat & Undead `-800` Instant Killer**: Hardcoded Red-List memproteksi 13 paket sistem vital dari crash/uninstall. Green-List uninstall (`pm uninstall -k --user 0`) langsung diikuti `cmd activity crash --user 0 <pkg>` untuk membasmi daemon undead `-800 PERSISTENT` tanpa reboot.
  5. **Shizuku IPC Bridge (`ShellExecutor.kt`)**: Mengeksekusi command shell tingkat `UID 2000` (setara kabel USB PC) langsung dari tablet tanpa PC dan tanpa root Magisk/KernelSU via refleksi.
  6. **Quick Settings Tile (`TurboBoosterTileService.kt`)**: Tombol tile di control center / status bar atas Android untuk on/off Turbo Mode 1-klik.
  7. **Boot Receiver (`BootReceiver.kt`)**: Mengecek status Shizuku saat boot dan memberikan prompt notifikasi ringan jika otentikasi Shizuku perlu diaktifkan ulang.

---

## 3. Daftar 100 Celah Oprekan Sistem (`boost_100_celah_master.sh`)

Skrip ini dieksekusi melalui Shizuku Shell (`sh /sdcard/TurboBoosterScripts/boost_100.sh` atau langsung via APK):

### Kategori 1: SurfaceFlinger & Compositor Pipeline (1–10)
1. `debug.sf.disable_client_composition_cache 1` — Hapus cache komposisi client lambat.
2. `debug.sf.disable_backpressure 1` — Hilangkan penahanan backpressure pada buffer render.
3. `debug.sf.drop_missed_frames 1` — Jangan tunggu frame terlambat, langsung render frame terbaru.
4. `debug.sf.vulkan_renderengine true` — Paksa renderengine SurfaceFlinger berbasis Vulkan.
5. `debug.sf.multithreaded_present true` — Multi-threaded present frame ke layar.
6. `debug.sf.fifo_priority_for_major_ui_processes true` — Prioritas FIFO real-time untuk proses UI utama.
7. `debug.sf.enable_frame_rate_override true` — Izinkan override frame rate langsung.
8. `debug.sf.vsync_event_phase_offset_ns 0` — Nolkan latency fasa VSYNC event.
9. `debug.sf.vsync_sf_event_phase_offset_ns 0` — Nolkan latency fasa VSYNC SurfaceFlinger.
10. `debug.sf.early_phase_offset_ns 0` & `early_app_phase_offset_ns 0` — Percepat waktu respons frame aplikasi.

### Kategori 2: HWUI RenderThread & Skia Engine (11–20)
11. `debug.hwui.renderer skiagl` — Paksa HWUI menggunakan SkiaGL.
12. `debug.renderengine.backend skiaglthreaded` — Gunakan multi-threaded SkiaGL untuk pipeline render.
13. `debug.hwui.use_gpu_pixel_buffers true` — Gunakan PBO (Pixel Buffer Objects) langsung di VRAM GPU.
14. `debug.hwui.target_cpu_time_percent 90` — Tingkatkan alokasi target waktu CPU untuk render UI hingga 90%.
15. `debug.hwui.render_ahead 1` — Siapkan 1 frame di depan untuk mencegah stuttering scrolling.
16. `debug.sqlite.wal.syncmode OFF` — Matikan disk sync blocking pada database SQLite (UI tidak menunggu storage eMMC).
17. `debug.hwui.clip_surfaceviews 1` — Optimasi clipping SurfaceView.
18. `debug.hwui.show_dirty_regions 0` — Matikan kalkulasi dirty regions yang membebani GPU.
19. `debug.hwui.profile false` — Matikan profiling overhead pada produksi.
20. `debug.hwui.disable_draw_defer 0` — Pertahankan deferral draw call optimal.

### Kategori 3: Qualcomm Bengal / Adreno 610 Driver Stabilization (21–30)
21. Hapus override ANGLE driver (`settings delete global angle_gl_driver_selection_pkgs`) — Fix game MLBB/Unity force close.
22. `debug.sf.latch_unsignaled 1` — Latching buffer instan tanpa menunggu sinyal hardware lambat.
23. `debug.sf.enable_hwc_vds 1` — Aktifkan Hardware Composer Virtual Display Service.
24. Bypass Android Phantom Process Killer: `device_config put activity_manager max_phantom_processes 2147483647`.
25. `pm disable-user com.miui.analytics` — Matikan daemon tracker analitik Xiaomi.
26. `pm disable-user com.miui.msa.global` — Matikan daemon iklan MIUI System Ads.
27. `pm disable-user com.xiaomi.metok-sdk` — Matikan pelacak metrik lokasi/telemetri.
28. `pm disable-user com.miui.daemon` — Matikan daemon background pemboros RAM.
29. `setprop persist.vendor.qti.games.gt.enable true` — Aktifkan Qualcomm Game Turbo acceleration.
30. `setprop ro.vendor.qti.sys.fw.bg_apps_limit 32` — Tingkatkan limit background app RAM LPDDR4X.

### Kategori 4: Display Refresh Rate & Panel Governance (31–40)
31. `settings put system min_refresh_rate 90.0` — Kunci refresh rate minimal 90Hz.
32. `settings put system peak_refresh_rate 90.0` — Kunci refresh rate maksimal 90Hz.
33. `settings put global fps_limit 90` — Bypass lock 60fps bawaan sistem.
34. `settings put secure refresh_rate_mode 2` — Mode paksa refresh rate tinggi HyperOS.
35. `setprop persist.vendor.power.dfps.level 90` — Dynamic FPS level terkunci di 90.
36. `settings put system thermal_limit_refresh_rate 0` — Abaikan sensor suhu yang suka menurunkan layar ke 60Hz.
37. `setprop debug.sf.set_idle_timer_ms 0` — Matikan timer idle layar agar tidak drop ke 30Hz saat membaca.
38. `settings put system user_refresh_rate 90` — Override preferensi user ke 90Hz.
39. `settings put system screen_refresh_rate 90` — Kunci display panel refresh rate.
40. `setprop ro.surface_flinger.has_wide_color_display true` — Maksimalkan gamut warna 10-bit IPS.

### Kategori 5: ADPF (Android Dynamic Performance Framework) & PowerHAL (41–50)
41. `setprop debug.adpf.gpu_work_duration 1` — Aktifkan telemetri waktu kerja GPU ke PowerHAL.
42. `setprop debug.adpf.use_fmq_channel true` — Gunakan Fast Message Queue IPC untuk respon instan.
43. `setprop debug.hwui.adpf_gpu_hint true` — Kirim hint beban render UI langsung ke GPU Adreno.
44. `cmd power set-adaptive-power-policy 0` — Matikan pembatasan daya adaptif agresif.
45. `setprop vendor.powerhal.adpf.enable true` — Aktifkan PowerHAL ADPF Snapdragon 680.
46. `setprop vendor.perf.gestureflingboost.enable true` — Boost frekuensi CPU saat jari melakukan flick/fling scroll.
47. `setprop persist.sys.turbosched.touch_boost true` — Xiaomi TurboSched boost frekuensi seketika saat layar disentuh.
48. `setprop persist.sys.power.boost_on_launch true` — Boost clock ke 2.4GHz saat aplikasi dibuka.
49. `setprop ro.config.low_ram false` — Pastikan sistem tahu RAM bukan Android Go 2GB.
50. `settings put global low_power_trigger_level 0` — Cegah trigger mode hemat daya otomatis.

### Kategori 6: ART Runtime, Dexopt & Ahead-Of-Time (AOT) Compilation (51–60)
51. `cmd package compile -m speed -f com.android.systemui` — Kompilasi biner SystemUI mode mesin murni (speed).
52. `cmd package compile -m speed -f com.android.settings` — Kompilasi biner Settings mode speed.
53. `cmd package compile -m speed -f com.miui.home` — Kompilasi launcher HyperOS agar scrolling aplikasi 0-drop frame.
54. `setprop dalvik.vm.dex2oat-filter speed` — Filter kompilasi aplikasi background default ke speed.
55. `setprop dalvik.vm.dex2oat-threads 4` — Gunakan 4 core Kryo Gold untuk kompilasi dex2oat cepat.
56. `setprop dalvik.vm.image-dex2oat-filter speed` — Kompilasi boot image sistem ke mode speed.
57. `setprop dalvik.vm.reg_alloc_no_output_overlap true` — Alokasi register dalvik optimal tanpa overlap.
58. `setprop dalvik.vm.compact_object_header true` — Kompres header objek Java untuk menghemat memori heap.
59. `setprop dalvik.vm.usejit true` — Aktifkan Just-In-Time compiler tier-2.
60. `setprop dalvik.vm.usejitprofiles true` — Gunakan profil JIT berbasis frekuensi penggunaan aplikasi.

### Kategori 7: Memory Architecture & LMKD Tuning (61–70)
61. `setprop persist.miui.extm.enable 0` — Matikan fitur "Ekstensi Memori" palsu di storage flash eMMC.
62. `settings put global ram_expand_size 0` — Nolkan alokasi swap file di disk storage.
63. `setprop persist.sys.miui.isExternalRamOn 0` — Konfirmasi status external RAM off di HyperOS.
64. `device_config put activity_manager use_compaction true` — Aktifkan kompaksi memori murni RAM.
65. `device_config put activity_manager compact_proactive true` — Kompaksi proaktif sebelum memori habis.
66. `device_config put activity_manager compact_full_rss_throttle_ms 5000` — Interval kompaksi 5 detik.
67. `cmd activity memory-factor set NORMAL` — Paksa status LMKD selalu pada faktor NORMAL (mencegah app ditutup paksa).
68. `setprop ro.lmk.kill_heaviest_task false` — Jangan bunuh aplikasi berat yang sedang dipakai user.
69. `setprop ro.lmk.promote_to_cached true` — Pertahankan aplikasi di cache RAM agar cepat dibuka kembali.
70. `setprop ro.lmk.use_minfree_levels true` — Gunakan ambang batas aman minfree.

### Kategori 8: Storage I/O & SQLite In-Memory Caching (71–80)
71. `setprop debug.sqlite.allow_temp_tables true` — Gunakan tabel temporary in-memory untuk database query cepat.
72. `setprop persist.sys.smart_idle_maint_enabled 1` — Aktifkan fstrim maintenance pintar saat idle.
73. Eksekusi `sm fstrim` — Bersihkan blok sampah flash eMMC 5.1 untuk memulihkan kecepatan write acak.
74. `setprop ro.sys.sdcardfs false` — Bypass overhead lapisan sdcardfs lama.
75. `setprop debug.sqlite.wal.autocheckpoint 1000` — Checkpoint WAL SQLite optimal.
76. `setprop vold.post_fs_data_done 1` — Flag filesystem ready.
77. `setprop debug.stagefright.ccodec 4` — Hardware codec acceleration untuk video decoding.
78. `setprop media.stagefright.enable-player true` — Stagefright native player hardware pipe.
79. `setprop media.stagefright.enable-http true` — Buffer streaming audio/video hardware pipe.
80. `setprop media.stagefright.enable-aac true` — Hardware decode audio AAC latency rendah.

### Kategori 9: Proprietary Xiaomi HyperOS 2.0 TurboSched (81–90)
81. `setprop debug.turbosched.enable true` — Aktifkan engine scheduler internal HyperOS.
82. `setprop persist.sys.turbosched.enable true` — Kunci permanen TurboSched di reboot.
83. `setprop persist.sys.turbosched.turbo_sched_core_app_list "com.arya.turbobooster,com.artcraft.wordcraft,com.artcraft.figcraft,com.artcraft.lightcraft,com.lemon.lvoverseas"` — Prioritas CPU core tertinggi untuk aplikasi kerja dan booster kita.
84. `cmd power set-mode 0` (High Performance Mode) — Aktifkan profil baterai "Performa Tinggi" HyperOS.
85. `setprop persist.sys.thermal.config /vendor/etc/thermal.highperf.conf` — Arahkan ke config thermal performa.
86. `setprop ro.vendor.perf.scroll_opt true` — Optimasi khusus fluiditas scrolling HyperOS.
87. `setprop persist.vendor.qti.core_ctl_min_cpu 4` — Pastikan 4 core kecil tidak pernah dimatikan total.
88. `setprop persist.vendor.qti.core_ctl_max_cpu 8` — Izinkan utilisasi 8 core sekaligus saat multitasking.
89. `setprop ro.vendor.qti.sys.fw.empty_app_percent 50` — Berikan 50% ruang cache untuk aplikasi kosong.
90. `setprop ro.vendor.qti.sys.fw.trim_empty_percent 100` — Pembersihan agresif hanya pada proses zombie.

### Kategori 10: Tablet Touch Digitizer & Power Management (91–100)
91. `cmd thermalservice override-status 0` — Override sensor suhu ke 0 (mencegah CPU drop ke 1.1GHz saat hangat).
92. `cmd power set-fixed-performance-mode-enabled true` — Kunci clock Snapdragon 680 di 2.40 GHz & 1.90 GHz konstan.
93. `settings put global wifi_scan_always_enabled 0` — Matikan scanning Wi-Fi background pembuat micro-stutter saat main game/kerja.
94. `settings put global ble_scan_always_enabled 0` — Matikan scanning Bluetooth LE background.
95. `device_config put runtime_native_boot mglru true` — Aktifkan Multi-Gen LRU (fitur kernel memori mutakhir Android 15).
96. `service call miui.mqsas.MqsasService 21 i32 1 s16 "SurfaceFlinger" s16 "90"` — Backdoor MQSAS kunci SurfaceFlinger 90 FPS.
97. `settings put system touch_response_speed 1` — Kecepatan polling sampling digitizer sentuh dinaikkan ke level maksimal.
98. `settings put system touch_gesture_speed 2` — Sensitivitas deteksi gestur layar HyperOS maksimal.
99. `settings put secure assistant_on_nav_gesture 0` — Matikan trigger Google Assistant di sudut bawah (hilangkan false swipe).
100. `settings put secure search_press_hold_nav_handle 0` — Matikan jeda Circle to Search pada navigasi gestur.

---

## 4. Kompaksi Memori & Solusi zRAM 8GB Murni di RAM Fisik

### Mengapa Memory Extension Bawaan HyperOS Dihapus?
Fitur "Penambahan Memori 4GB" bawaan Xiaomi membuat swap file di flash disk eMMC 5.1. Kecepatan tulis acak eMMC 5.1 hanya **20 MB/s**. Ketika RAM penuh, Android mulai memindahkan memori ke eMMC, menyebabkan tablet freeze selama 1–3 detik (stutter parah).

### Solusi Kita (`boost_pure_zram_compaction.sh` & `set_zram_8gb.sh`):
1. Mematikan swap file eMMC total:
   ```bash
   setprop persist.miui.extm.enable 0
   setprop persist.sys.miui.isExternalRamOn 0
   settings put global ram_expand_size 0
   ```
2. Menyetel ukuran zRAM ke 8192 MB (8GB terkompresi) menggunakan backdoor MQSAS:
   ```bash
   service call miui.mqsas.MqsasService 21 i32 1 s16 "persist.sys.zram_size" s16 "8192"
   service call miui.mqsas.MqsasService 21 i32 1 s16 "persist.miui.extm.bdsize" s16 "8192"
   ```
3. Reroute swap murni ke chip LPDDR4X fisik yang memiliki bandwidth **17 GB/s** (850x lebih cepat daripada flash eMMC) menggunakan kompresi LZ4/ZSTD in-memory.

---

## 5. Tampilan Desktop Tablet (205 DPI) & Navigasi Gestur 0ms

### Density Tablet Desktop (`wm density 205`)
- Resolusi fisik: `1200 x 1920` (16:10).
- Jika diset di atas 260 DPI: HyperOS memperlakukan tablet seperti HP raksasa (icon membesar, dock hilang, layout 1 kolom sempit).
- Pada **`wm density 205`**:
  * Lebar logis landscape: $1920 / (205/160) \approx 1498\text{ dp}$ (Desktop Class).
  * Lebar logis portrait: $1200 / (205/160) \approx 936\text{ dp}$.
  * Menghasilkan: Multi-column view pada Settings, taskbar/dock bawah desktop permanen, kanvas editing luas, dan layout tablet Android murni.
- Command:
  ```bash
  adb shell wm size 1200x1920
  adb shell wm density 205
  ```

### Navigasi Gestur 0ms (`fix_swipe_home_delay.sh`)
- Menghilangkan navigasi 3 tombol klasik dan mengaktifkan gesture full screen:
  ```bash
  settings put global force_fsg_nav_bar 1
  settings put system force_fsg_nav_bar 1
  ```
- Menghilangkan gesture line putih di bawah layar (`hide_gesture_line 1`). Hal ini membuang algoritma "jeda tunggu 150-200ms" bawaan Xiaomi yang biasanya menunggu apakah user mau swipe ganti app atau swipe pulang ke home. Hasilnya: **Swipe to home seketika (0ms response)**.
- Skala animasi diatur ke `0.35x` (atau `0.0x` untuk instant teleport).

---

## 6. Solusi Crash Video Editor CapCut & Multi-Window

Saat mengaktifkan downscaler resolusi layar (`cmd miui.downscale add-debug-app`), aplikasi video editor seperti **CapCut** (`com.lemon.lvoverseas` / `com.lemon.ccpadpro`) mengalami crash EGL context (layar preview video hitam / render gagal).

### Perbaikan yang Diterapkan:
1. **Pengecualian Mutlak:** Paket CapCut **dilarang keras** dimasukkan ke daftar downscaler miui.
2. **Reset Game Mode Policy:**
   ```bash
   cmd game reset com.lemon.lvoverseas
   cmd game reset com.lemon.ccpadpro
   ```
3. **Bypass Scoped Storage (Media AppOps Allow):**
   ```bash
   for PKG in com.lemon.lvoverseas com.lemon.ccpadpro; do
     cmd appops set $PKG READ_EXTERNAL_STORAGE allow
     cmd appops set $PKG WRITE_EXTERNAL_STORAGE allow
     cmd appops set $PKG MANAGE_EXTERNAL_STORAGE allow
     cmd appops set $PKG READ_MEDIA_IMAGES allow
     cmd appops set $PKG READ_MEDIA_VIDEO allow
     cmd appops set $PKG READ_MEDIA_AUDIO allow
     cmd appops set $PKG NO_ISOLATED_STORAGE allow
   done
   ```
4. **Aktifkan Freeform & Multi-Window:**
   ```bash
   settings put global enable_freeform_support 1
   settings put global force_resizable_activities 1
   settings put global enable_non_resizable_multi_window 1
   setprop persist.sys.freeform_window true
   ```

---

## 7. Ekosistem Android Craft: WordCraft, FigCraft & LightCraft

Direktori: `/mnt/HDD-2/BACKUP LINUX/FILE DATA/SOFTWARE LINUX/ANDROID CRAFT/`

### A. WordCraft v0.15.2 (`com.artcraft.wordcraft`)
1. **Mode Fullscreen Murni (Immersive Sticky):**
   - Status bar atas (indikator baterai, jam, sinyal, ikon notifikasi) serta bottom navigation bar disembunyikan 100%.
   - Menggunakan `WindowInsetsControllerCompat` untuk menyembunyikan `WindowInsetsCompat.Type.systemBars()` secara permanen.
2. **Integrasi Android SAF Document Picker:**
   - Tombol export/save tidak lagi menulis ke path lokal statis yang tidak bisa diakses user.
   - Mengirim intent native Android: `Intent(Intent.ACTION_CREATE_DOCUMENT)` / `ACTION_OPEN_DOCUMENT_TREE` dengan MIME type dokumen yang sesuai, memunculkan popup direktori Android agar user bisa memilih folder unduhan/dokumen internal sesuka hati.
3. **Icon DevCraft:** Memakai ikon modern resolusi tinggi DevCraft resmi.

### B. FigCraft v0.20.1 (`com.artcraft.figcraft`)
1. **Arsitektur Rust Native ARM64:**
   - Dibangun murni menggunakan bahasa Rust, framework GUI `eframe` / `egui`, dan dikompilasi ke binary native Android `aarch64-linux-android` via Android NDK.
2. **Native Drag and Drop File/Foto:**
   - Mendengarkan event dropped files langsung dari event loop egui (`ctx.input(|i| !i.raw.dropped_files.is_empty())`).
   - File gambar (PNG, JPG, SVG) yang diseret langsung di-load ke VRAM GPU sebagai texture ID.
3. **SAF Directory & File Importer:**
   - Memanggil ContentResolver Android untuk membuka dialog file picker native saat tombol open/import diklik.
4. **Icon DevCraft:** Mengganti ikon lama (sebelumnya ikon Word) dengan ikon resmi FigCraft DevCraft.

### C. LightCraft v0.20.1 (`com.artcraft.lightcraft`)
1. **Arsitektur Rust Native ARM64:**
   - Dibangun murni menggunakan Rust + egui untuk duplikasi editing foto ala Lightroom di Android tablet.
2. **Native Drag and Drop Image:**
   - Mendukung drag & drop langsung file foto mentah (RAW / JPEG) ke area viewport kanvas.
3. **SAF Storage Access:**
   - Membuka direktori folder galeri tablet langsung via Storage Access Framework Android.
4. **Icon DevCraft:** Mengganti ikon lama dengan ikon resmi LightCraft DevCraft.

---

## 8. Daftar File Project, Skrip Shell & Struktur Direktori

### Root Project: `/mnt/HDD-2/BACKUP LINUX/FILE DATA/REDMI PAD SE BOOST/`
```
├── AGENTS.md                                # Master Architecture & System Guide untuk AI Agent
├── README.md                                # Dokumentasi publik repo GitHub
├── CATATAN_OPREKAN_MASTER.md                # Dokumen master rekap ini
├── app/                                     # Source code Kotlin Android Studio TurboBooster
│   ├── build.gradle.kts                     # versionCode 9, versionName 1.3.5
│   └── src/main/
│       ├── AndroidManifest.xml              # Shizuku provider, permissions, services
│       ├── assets/
│       │   ├── audio/kuntilanak_seram.mp3   # Audio test quad speaker
│       │   └── scripts/                     # 12 skrip shell yang diekstrak ke tablet
│       └── java/com/arya/turbobooster/
│           ├── MainActivity.kt              # Dashboard status & telemetri
│           ├── AudioPlayerService.kt        # Background service audio feedback
│           └── booster/
│               ├── AudioBoosterManager.kt   # Dolby & Harman sound tuning
│               ├── DeviceMetrics.kt         # Sensor CPU, RAM, Suhu, 90Hz
│               ├── ScriptInstaller.kt       # Ekstraktor skrip shell ke /sdcard/
│               ├── ShellExecutor.kt         # Shizuku IPC executor
│               └── TurboBoosterManager.kt   # 21 langkah aktivasi Super Performance
├── release/
│   └── TurboBooster.apk                     # Binary release APK ter-compile (5.2 MB)
├── boost_100_celah_master.sh                # Skrip 100 exploit celah sistem lengkap
├── boost_pure_zram_compaction.sh            # Skrip pembersihan swap eMMC & zRAM murni
├── set_zram_8gb.sh                          # Skrip injeksi MQSAS zRAM 8GB
├── fix_swipe_home_delay.sh                  # Skrip 0ms gesture swipe & density 205
├── boost_flagship_unfcker.sh                # Skrip spoofing Tier-3 Flagship
├── boost_oppo_a60_master.sh                 # Profil Snapdragon 680 (CPH2631)
├── boost_redmi_note_10_pro.sh               # Profil Snapdragon 732G (sweet)
└── stress_test_5min.py                      # Skrip pengujian kestabilan 5 menit via ADB
```

---

## 9. Alur Build, Compile & Deployment ADB untuk Agent Masa Depan

Bagi Agent Antigravity / AI berikutnya yang melanjutkan project ini, ikuti alur SOP di bawah ini:

### 1. Menghubungkan Tablet via Wi-Fi ADB
HyperOS 2.0 merotasi port wireless debugging saat Wi-Fi reconnect atau tablet standby.
```bash
# Cek port aktif via mDNS:
adb mdns services

# Hubungkan (sesuaikan IP dan port yang didapat dari mDNS atau menu Developer Options):
adb connect 192.168.1.33:40337

# Pastikan status 'device':
adb devices
```

### 2. Melakukan Perubahan Kode & Bump Versi
Buka file `app/build.gradle.kts`:
- Setiap kali ada update fungsionalitas, **WAJIB BUMP `versionCode`** (misal dari 9 ke 10) agar Android Package Manager mengizinkan in-place update tanpa uninstall.
- Sesuaikan `versionName` (misal `"1.3.6"`).

### 3. Mengompilasi Release APK
Jalankan di direktori root project:
```bash
cd "/mnt/HDD-2/BACKUP LINUX/FILE DATA/REDMI PAD SE BOOST"
./gradlew assembleRelease
cp app/build/outputs/apk/release/app-release-unsigned.apk release/TurboBooster.apk
# atau gunakan APK rilis yang sudah ter-sign
```

### 4. Deploy & Install ke Tablet
```bash
adb -s 192.168.1.33:40337 install -r "release/TurboBooster.apk"

# Verifikasi versi yang ter-install di tablet:
adb -s 192.168.1.33:40337 shell "dumpsys package com.arya.turbobooster | grep -E 'versionCode|versionName'"

# Jalankan aplikasi langsung di layar tablet:
adb -s 192.168.1.33:40337 shell "monkey -p com.arya.turbobooster -c android.intent.category.LAUNCHER 1"
```

### 5. Commit & Push ke GitHub
```bash
cd "/mnt/HDD-2/BACKUP LINUX/FILE DATA/REDMI PAD SE BOOST"
git add .
git commit -m "feat: <deskripsi update>"
git push origin main
```

---

## 🚀 UPDATE v2.0.4 (14 November 2026): FIX FORCE CLOSE FACEBOOK & MULTI-PROCESS APPS

### 1. Root Cause Analysis (Forensic Crash Diagnosis)
- **Gejala:** Aplikasi Facebook (`com.facebook.katana`) langsung force-close saat dibuka dari launcher.
- **Dumpsys Dropbox Stacktrace:**
  ```text
  android.database.sqlite.SQLiteDatabaseLockedException: database is locked (code 5 SQLITE_BUSY): retrycount exceeded
      at android.database.sqlite.SQLiteConnection.nativeExecuteForCursorWindow(Native Method)
      at android.database.sqlite.SQLiteConnection.executeForCursorWindow(SQLiteConnection.java:1037)
      at android.database.sqlite.SQLiteOpenHelper.getWritableDatabase(SQLiteOpenHelper.java:316)
  ```
- **Penyebab Utama:** Property sistem framework `debug.sqlite.journalmode WAL` & `debug.sqlite.wal.syncmode NORMAL` yang diterapkan secara global via `setprop`. Di Android, framework SQLite memaksa seluruh aplikasi membaca settingan global ini. Aplikasi arsitektur multi-proses seperti Facebook (`:nodex`, `:browser`, `:dash`, main) mengalami lock contention (`SQLITE_BUSY`) saat inisialisasi database.
- **Penyebab Sekunder:** 
  1. Revokasi AppOps agresif (`WAKE_LOCK ignore` & `RUN_ANY_IN_BACKGROUND ignore`) pada Facebook.
  2. Perintah `cmd package suspend` di Rust `turbodaemon` saat game/CapCut aktif yang membekukan package.

### 2. Solusi & Perbaikan Sistem (v2.0.4)
1. **Reset Total Property Global SQLite:**
   - Hapus dan reset seluruh `debug.sqlite.*` ke string kosong `""` di Android runtime, `TurboBoosterManager.kt`, dan skrip shell (`boost.sh`, `boost_100.sh`, `boost_deep_system.sh`).
2. **Safe Background Caging (Anti-Crash):**
   - Hapus revokasi AppOps `WAKE_LOCK` dan `RUN_ANY_IN_BACKGROUND` pada aplikasi medsos.
   - Ganti ke `cmd activity set-bg-restriction-level --user 0 <pkg> restricted_bucket` dan `am set-standby-bucket <pkg> rare` yang aman dan tidak memicu crash runtime.
3. **Rust Daemon Anti-Suspend (`turbodaemon`):**
   - Hapus pemanggilan `cmd package suspend` dari Rust daemon. Ganti isolasi background dengan pembatasan background bucket tanpa menonaktifkan peluncuran aplikasi oleh user.
4. **Verifikasi Komprehensif Seluruh Aplikasi (14/14 Apps OK Clean):**
   - Facebook (`com.facebook.katana`) -> **OK / Normal**
   - Instagram (`com.instagram.android`) -> **OK / Normal**
   - TikTok (`com.ss.android.ugc.trill`) -> **OK / Normal**
   - Shopee (`com.shopee.id`) -> **OK / Normal**
   - Tokopedia (`com.tokopedia.tkpd`) -> **OK / Normal**
   - WhatsApp (`com.whatsapp`) -> **OK / Normal**
   - CapCut (`com.lemon.lvoverseas`) -> **OK / Normal**
   - Google Chrome (`com.android.chrome`) -> **OK / Normal**
   - YouTube (`com.google.android.youtube`) -> **OK / Normal**
   - Google Play Store (`com.android.vending`) -> **OK / Normal**
   - WordCraft (`com.artcraft.wordcraft`) -> **OK / Normal**
   - FigCraft (`com.artcraft.figcraft`) -> **OK / Normal**
   - Adobe Lightroom (`com.adobe.lrmobile`) -> **OK / Normal**
   - Spotify (`com.spotify.music`) -> **OK / Normal**
5. **DPI Invariant:** Kerapatan layar tablet tetap terkunci di **205 DPI** (`Physical: 280`, `Override: 205`, `1200x1920`).

---

## 📌 Update 14 November 2026 — TurboBooster v2.0.5 (Full Rust Daemon Fat LTO & Android AOT Speed Compilation)

1. **Rust Daemon Max Native Performance (`turbodaemon`):**
   - Dioptimalkan ke profile release tertinggi di `Cargo.toml`: `opt-level = 3` dan `lto = "fat"` (Full Link-Time Optimization antar-crate).
   - Recompiled via `build_daemon.sh` (`libturbodaemon.so` 533 KB).
   - Menghasilkan IPC socket, polling kernel PSI (`/proc/pressure/memory`), dan memory compaction yang dieksekusi langsung pada instruksi native ARM64 CPU Gold Core tanpa interpreter overhead.

2. **Full System AOT Native Speed Compilation (`cmd package compile -m speed -f`):**
   - Seluruh dex/kotlin/java bytecode dikonversi permanen menjadi file native `.odex`/`.vdex` ARM64 (mengeliminasi kompilasi JIT runtime di CPU Silver 0-3):
     - `android` (Framework-res & SystemServer) -> **Success**
     - `com.android.systemui` -> **Success**
     - `com.miui.home` (Launcher & gestures) -> **Success**
     - `com.google.android.inputmethod.latin` (Gboard) -> **Success**
     - `com.android.settings` -> **Success**
     - `com.google.android.webview` -> **Success**
     - `com.android.chrome` -> **Success**
     - `com.facebook.katana` -> **Success**
     - `com.instagram.android` -> **Success**
     - `com.ss.android.ugc.trill` (TikTok) -> **Success**
     - `com.whatsapp` -> **Success**
     - `com.lemon.lvoverseas` (CapCut Global) -> **Success**
     - `com.artcraft.wordcraft` -> **Success**
     - `com.artcraft.figcraft` -> **Success**
     - `com.tokopedia.tkpd` -> **Success**
     - `com.shopee.id` -> **Success**
     - `com.spotify.music` -> **Success**
     - `com.arya.turbobooster` -> **Success**
   - Repinned memory buffer framework: `cmd pinner repin`.
   - Discard & TRIM storage flash block eMMC 5.1: `sm fstrim`.

3. **Custom ROM Snappiness & Touch Tuning:**
   - Window, transition, & animator scale diset ke `0.50x` untuk respon instan khas lightweight Custom ROM (LineageOS/AOSP).
   - Touch response speed `1` & touch gesture speed `2`.
   - Window blurs & MIUI home blur disabled (`0`) untuk membebaskan fillrate Adreno 610 GPU.

4. **Kerapatan Layar (DPI Invariant):**
   - Kerapatan layar tablet tetap terkunci aman di **205 DPI** (`Physical: 280`, `Override: 205`, `1200x1920`).

---

## 10. Update 14 November 2026 — TurboBooster v2.1.0 (Master 1-Click Boost & Smart Incremental AOT)

- **Version**: `versionCode = 16`, `versionName = "2.1.0"`
- **Binary APK**: `release/TurboBooster.apk` (2.1 MB)
- **Production Screenshot**: `screenshot_v2.1.0_production.png` (1920x1200 landscape)

### Fitur & Pembaharuan Utama v2.1.0:
1. **Master 1-Click Action (`btnMasterBoost` / `switchTurboMode`)**:
   - Satu klik otomatis menjalankan seluruh arsitektur oprekan performa secara berurutan:
     1. Profil Extreme 90 FPS Lock (MQSAS Call 21, Gold Core 4-7 affinity, 0.25x instant animation).
     2. Native Rust Daemon (`turbodaemon`) startup / IPC healthcheck dengan auto-reparent ke init PID 1 (`nohup`).
     3. Surgical Debloat dengan Smart Detection (otomatis skip 38 paket yang sudah ter-debloat).
     4. Smart Incremental AOT Speed Compilation (otomatis skip paket yang sudah berstatus `[status=speed]`).
     5. Auto-Apply on Boot sinkronisasi otomatis.
2. **Isolasi Sub-Sistem Audio Quad-Speaker Studio**:
   - Sub-sistem audio (Dolby Atmos, Dirac, Hi-Res spatial, 32-bit float output) tetap terisolasi penuh di kartu kontrol mandiri (`switchAudioBooster`), tidak dipaksa masuk ke dalam 1-click boost.
3. **Smart Incremental AOT Compilation Engine**:
   - Membaca status kompilasi via parsing cepat bulk `dumpsys package dexopt` (<1 detik untuk 366+ aplikasi).
   - Mendeteksi filter `arm64: [status=speed]`. Jika aplikasi sudah terkompilasi ke `speed`, kompilasi di-skip 0 detik.
   - Hanya mengompilasi aplikasi yang baru diinstall atau belum berstatus `speed` (`cmd package compile -m speed <pkg>`), diakhiri dengan `cmd pinner repin` dan `sm fstrim`.
   - Mengeliminasi delay kompilasi paksa (-f) berulang dari versi sebelumnya sehingga eksekusi 1-klik tuntas dalam <3 detik.
4. **Smart Surgical Debloat Skip Detection**:
   - Memeriksa paket terinstal pada User 0 (`pm list packages --user 0`) dalam 1 query (<50ms).
   - Melewati paket green-list yang sudah di-uninstall, mencegah pemanggilan `pm uninstall` berulang yang tidak perlu.
5. **Real-Time Hardware & Memory Telemetry UI (Grid 3x2)**:
   - **RAM Fisik LPDDR4X**: Digunakan, Sisa dalam GB, Total RAM (contoh: `2.0 GB / 3.6 GB`), Persentase terpakai, dan progress bar visual.
   - **zRAM Swap LPDDR4X**: Digunakan, Sisa dalam MB, Total swap (contoh: `1,287 MB / 4,095 MB`), Persentase terpakai, dan progress bar visual.
   - **CPU Snapdragon 680**: Frekuensi live cluster Gold Core (Cores 4-7) & Silver Core (Cores 0-3).
   - **SoC Temperature & Thermal**: Live temperature dalam °C (misal `46.1 °C`) dan status thermal (Cool / Overridden).
   - **Display Refresh Rate**: Live 90 Hz lock detection.
   - **Rust Daemon Status**: Live indicator PID & RSS memory footprint (misal `⚡ Active • 3.3 MB`).
6. **Invarian Kerapatan Layar (Strict 205 DPI)**:
   - Terverifikasi tetap terkunci di 205 DPI (`Physical: 280`, `Override: 205`, `1200x1920`).

---

## 11. Update 14 November 2026 — TurboBooster v2.1.1 (Incremental AOT Perfection & Shell Timeout Hardening)

- **Version**: `versionCode = 17`, `versionName = "2.1.1"`
- **Binary APK**: `release/TurboBooster.apk` (2.1 MB)
- **Production Screenshot**: `screenshot_v2.1.0_production.png` (1920x1200 landscape)

### Temuan Kegagalan & Perbaikan v2.1.1:
1. **Penyebab Kegagalan AOT di v2.1.0**:
   - `ShellExecutor.execute()` sebelumnya memiliki hardcoded timeout 8000ms. Perintah `dumpsys package dexopt` pada Snapdragon 680 memerlukan ~10.02s untuk dump seluruh 366+ paket, sehingga query selalu dibatalkan (`TimeoutCancellationException`) dan map status selalu kosong.
   - Pengecekan status kompilasi hanya memvalidasi `status == "speed"`. Paket dengan status `[status=verify] [reason=vdex-dm]` (Google Play Cloud Dex Metadata), `[status=run-from-apk]` (uncompressed embedded dex), dan `[reason=cmdline]` selalu dianggap belum terkompilasi, menyebabkan kompilasi ulang 27 paket berulang-ulang setiap kali tombol 1-klik ditekan (memakan waktu ~30 detik).
   - `switchTurboMode.isChecked = true` terlewatkan saat eksekusi `runMasterOneClickBoost()`, menyisakan switch dalam status visual tidak sinkron.

2. **Solusi & Hardening v2.1.1**:
   - **`ShellExecutor.kt`**: Menambahkan parameter `timeoutMs: Long = 10000L` pada `execute()`, `executeViaShizuku()`, dan `executeViaLocalShell()`. Memberikan timeout 25s untuk `dumpsys package dexopt` dan 60s untuk `cmd package compile`.
   - **`TurboBoosterManager.kt`**:
     - Parsing multi-atribut cerdas membaca `status` DAN `reason`.
     - Kriteria Smart Skip mencakup: `status == "speed"`, `status == "run-from-apk"`, `reason == "cmdline"`, `reason == "vdex-dm"`, WebAPKs (`org.chromium.webapk.*`), serta cache persisten `SharedPreferences`.
     - Hasil verifikasi di tablet riil: **107 paket di-skip instan (0 detik), 0 paket dikompilasi ulang tidak perlu**.
   - **`MainActivity.kt`**: Sinkronisasi eksplisit `switchTurboMode.isChecked = true` dan `switchAutoBoot.isChecked = true` saat Master 1-Click Boost selesai.

---

## 12. Update 14 November 2026 — TurboBooster v2.1.2 (Fix Crash Launcher com.miui.home & Lockscreen Bounce)

- **Version**: `versionCode = 18`, `versionName = "2.1.2"`
- **Binary APK**: `release/TurboBooster.apk` (2.1 MB)
- **Target Device**: Xiaomi Redmi Pad SE (`xun`), HyperOS 2.0 / Android 15, Adreno 610, 4GB RAM

### 1. Root Cause Forensic Analysis (Bedah Forensik Masalah):
Berdasarkan investigasi menyeluruh melalui `dumpsys activity exit-info com.miui.home`, `dumpsys activity exit-info com.android.systemui`, dan `tombstone_14`:
1. **SIGSEGV Crash pada `SmoothCorner::clipSmoothCorner` (Adreno 610 Driver Conflict)**:
   - Skrip `boost_100_celah_master.sh` & `boost_flagship_unfcker.sh` sebelumnya menginjeksi property MQSAS 21: `persist.sys.support_window_smoothcorner true`, `persist.sys.support_view_smoothcorner true`, dan `persist.sys.computility.cpulevel 6`.
   - Pada GPU Adreno 610 (Snapdragon 680), hardware pipeline `libhwui.so` mengalami null pointer dereference (`fault addr 0x61`) di dalam fungsi `SmoothCorner::clipSmoothCorner` saat merender transisi jendela atau floating freeform window mode 5 ("flying app").
   - Akibatnya, `com.android.systemui` mengalami fatal exception (`reason=4 APP CRASH (EXCEPTION)`), me-reset Keyguard dan langsung mental (bounce) ke lock screen saat pengguna keluar dari aplikasi atau membuka floating window.
2. **LMKD `kill_heaviest_task = true` Menargetkan Launcher `com.miui.home`**:
   - `com.miui.home` di tablet HyperOS mengonsumsi RSS sebesar 150MB s/d 270MB untuk caching ikon, widget, dan tata letak launcher.
   - Saat aplikasi memori berat (CapCut, Facebook, Shopee, game, atau browser) dibuka, `com.miui.home` turun ke status background/cached. Dengan flag `kill_heaviest_task = true`, daemon LMKD secara agresif memilih proses dengan RSS terbesar (`com.miui.home`) dan mengirimkan `SIGKILL 9` (`reason=2 SIGNALED status=9` atau `reason=3 LOW_MEMORY`).
   - Diperparah oleh `persist.sys.mms.kill_fas_cached_idle true` dari Xiaomi MMS dan `max_cached_processes 16` yang terlalu sempit, memicu launcher terbunuh terus-menerus.
3. **Absennya Whitelist & Kompaksi Berlebih di `turbodaemon`**:
   - `turbodaemon` (Rust native daemon) belum memasukkan `com.miui.home` dan `com.android.systemui` ke dalam `protected_apps`.
   - Fitur screen-off gentle memory reclaimer sebelumnya mengeksekusi `cmd activity compact system`, yang memampatkan seluruh memori launcher ke zRAM, memicu page fault besar dan lonjakan PSI stall saat tablet dinyalakan kembali.

### 2. Tindakan Perbaikan & Solusi Komprehensif (v2.1.2):
1. **LMKD & Memory Headroom Tuning (`TurboBoosterManager.kt` & Shell Scripts)**:
   - Mengubah `kill_heaviest_task` dari `true` ke `false` via `device_config override lmkd_native kill_heaviest_task false` dan `device_config delete lmkd_native kill_heaviest_task`. LMKD kembali ke urutan OOM Score standar (membunuh background leeches 900+, bukan launcher).
   - Menaikkan `max_cached_processes` dari 16 menjadi 24 untuk memberikan ruang napas bagi `com.miui.home`.
   - Mematikan pembunuhan idle oleh Xiaomi MMS: `persist.sys.mms.kill_fas_cached_idle false`.
2. **Eliminasi Fatal Hardware Crash (`SmoothCorner`)**:
   - Mengunci `persist.sys.support_window_smoothcorner false` dan `persist.sys.support_view_smoothcorner false` via MQSAS 21.
   - Me-reset `persist.sys.computility.cpulevel 0` dan `persist.sys.advanced_visual_release 0` ke level hardware aman Snapdragon 680.
3. **Imunitas Mutlak Launcher, SystemUI & TurboBooster**:
   - Mengeksekusi proteksi tingkat framework:
     * `settings put system miui_home_animation_rate 2`
     * `cmd activity set-bg-restriction-level --user 0 com.miui.home exempted`
     * `cmd activity set-bg-restriction-level --user 0 com.android.systemui exempted`
     * `cmd activity set-bg-restriction-level --user 0 com.arya.turbobooster exempted`
     * `am set-standby-bucket com.miui.home active`
     * `am set-standby-bucket com.android.systemui active`
     * `am set-standby-bucket com.arya.turbobooster active`
     * `cmd appops set com.miui.home RUN_IN_BACKGROUND allow` & `RUN_ANY_IN_BACKGROUND allow`
     * `cmd appops set com.android.systemui RUN_IN_BACKGROUND allow` & `RUN_ANY_IN_BACKGROUND allow`
     * `dumpsys deviceidle whitelist +com.miui.home`, `+com.android.systemui`, `+com.arya.turbobooster`
     * `device_config put system_performance com.android.server.flags.skip_home_art_pins false`
     * `device_config put system_performance com.android.server.flags.pin_webview true`
     * `cmd pinner repin`
   - Menghapus `com.miui.home` dari perulangan `am compact full` di seluruh skrip (`boost_pure_zram_compaction.sh`, dll).
   - Menghapus residu fallback `cmd activity compact system` dari `MainActivity.kt` (menggantinya dengan safe trim `sm fstrim` tanpa mengorbankan heap launcher).
   - Memperbaiki `TurboBoosterManager.getProtectedApps` dan `removeProtectedApp` agar `com.miui.home`, `com.android.systemui`, `com.arya.turbobooster`, dan `moe.shizuku.privileged.api` selalu dijamin ada (immutable) bahkan setelah update dari versi lama yang memiliki shared_prefs usang.
4. **Rust Native Daemon (`turbodaemon`) Hardening**:
   - Memasukkan `com.miui.home`, `com.android.systemui`, `com.miui.securitycenter`, `com.google.android.gms`, `com.arya.turbobooster`, dan `moe.shizuku.privileged.api` ke dalam `protected_apps`.
   - Menghapus pemanggilan `cmd activity compact system` dari screen-off reclaimer dan `TRIGGER_RECLAIM`.
   - Mengoptimalkan deteksi layar `is_screen_awake()` via `getprop debug.tracing.screen_state` (0-fork fastpath, mengeliminasi spam `dumpsys power` berkala).
   - Menambahkan background monitoring thread yang secara persisten mengawal status `kill_heaviest_task false`, `max_cached_processes 24`, dan `support_window_smoothcorner false`, serta men-throttle eksekusi imunitas framework ke interval berkala aman (~5 menit) agar tidak membebani binder thread pool `system_server`.
   - Mengompilasi ulang binary via `build_daemon.sh` (`libturbodaemon.so` 537KB) dan mem-packaging ke APK.

### 3. Hasil Pengujian & Verifikasi Live Tablet:
- **Pengujian Multi-Aplikasi Berat**: CapCut Pad Pro (`com.lemon.ccpadpro`), Facebook (`com.facebook.katana`), Shopee (`com.shopee.id`), dan Oxide Browser (`com.aistudio.oxidebrowser.vxkrzt`) dibuka bergantian.
- **Pengujian Freeform Multi-Window ("flying app", Windowing Mode 5)**: Membuka aplikasi dalam jendela bebas melayang, memindahkannya, dan menekan tombol Home.
- **Hasil Exit-Info**: `com.miui.home` (PID 24841) bertahan 100% stabil, 0 kali restart, 0 crash exception, dan tablet sama sekali tidak pernah mental ke lockscreen.
- **Invarian Layar (Strict 205 DPI)**: Kerapatan layar tablet tetap terkunci aman di **205 DPI** (`Physical: 280`, `Override: 205`, `1200x1920`).

---

## 12. Update v2.1.3: Perbaikan Crash Roblox & Bypass HyperOS Downscale NPE

### 1. Root Cause Analysis Crash Roblox:
1. **HyperOS 2.0 Tablet Window Downscale NullPointerException**:
   - Saat Roblox dimasukkan ke dalam daftar debug app downscale (`cmd miui.downscale add-debug-app com.roblox.client 0.7/0.8`), `ActivityManagerService.attachApplicationLocked` memanggil `CompatModePackages.getCompatScaleFromProvider()` -> `MiuiDownscaleController.getCompatScale()` -> `DownscaleCloudRule.isTabletNormalEnable()`.
   - Di `DownscaleCloudRule.java` line 121, pemanggilan `DownscaleCloudData.getDeviceItem()` dilakukan pada referensi objek yang bernilai `null` (karena data cloud Xiaomi belum diunduh/kosong pada tablet rootless).
   - Hal ini memicu `NullPointerException` di thread `system_server`, merusak `IWindowSession`, dan menyebabkan AMS mengirim sinyal 9 (`kill -9`) saat proses Roblox sedang mengikat (`reason=7 INITIALIZATION FAILURE description=error during bind`).
2. **Ketergantungan IPC pada `statsd`**:
   - Library native C++ milik Google Play Services dan engine Roblox melakukan pemanggilan blocking ke AIDL service `stats` (`ServiceManagerCppClient: Waited one second for stats`).
   - Ketika daemon `statsd` dimatikan oleh skrip tracing lama via MQSAS Call 21, proses Roblox mengalami timeout dan gagal melakukan inisialisasi bind.

### 2. Solusi & Implementasi Teknis (v2.1.3):
1. **Eksklusi Mutlak Roblox dari Sistem Downscaling**:
   - Di `TurboBoosterManager.kt`, `MainActivity.kt`, dan `boost_deep_system.sh`, `com.roblox.client` resmi dieksklusi dari pemanggilan `cmd miui.downscale add-debug-app` dan `game_overlay`.
   - Menambahkan perintah pembersihan otomatis: `cmd miui.downscale remove-debug-app com.roblox.client`, `cmd game reset com.roblox.client`, dan `device_config delete game_overlay com.roblox.client`.
2. **Konfigurasi Native 90 FPS & TARE VIP**:
   - Mengunci frame rate Roblox langsung di 90 FPS native: `cmd game set --fps 90 com.roblox.client`.
   - Memberikan prioritas VIP Task Attribution & Resource Execution (TARE): `cmd tare set-vip 0 com.roblox.client true`.
3. **Imunitas AppOps & Standby Bucket**:
   - Memberikan izin penuh background execution:
     * `cmd appops set com.roblox.client RUN_IN_BACKGROUND allow`
     * `cmd appops set com.roblox.client RUN_ANY_IN_BACKGROUND allow`
     * `cmd appops set com.roblox.client 10008 allow`
     * `cmd appops set com.roblox.client WAKE_LOCK allow`
     * `dumpsys deviceidle whitelist +com.roblox.client`
     * `am set-standby-bucket com.roblox.client active`
   - Memasukkan `com.roblox.client` ke dalam `DEFAULT_PROTECTED_APPS`.
4. **Proteksi Daemon `statsd`**:
   - Menghapus `"statsd"` dari `TRACING_DAEMONS` di `TurboBoosterManager.kt` agar IPC stats service tetap aktif melayani thread Google/Roblox.
5. **Verifikasi Live**:
   - Roblox berhasil dibuka dari cold start (`com.roblox.client/.ActivityNativeMain`), PID aktif, dan login terverifikasi lancar tanpa force close.

---
*Catatan ini dibuat secara otomatis dan komprehensif sebagai single source of truth untuk seluruh sesi pengerjaan Redmi Pad SE & Android Craft Suite.*
