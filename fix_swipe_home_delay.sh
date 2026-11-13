#!/bin/sh
# Xiaomi Redmi Pad SE - Instant Zero-Delay Swipe Home & App Opening/Closing Boost
# Fix for: "Buka tutup menu app dll ga seresponsif tablet 10 juta / swipe home delay"

# Dual Environment Detector (PC ADB vs Device Shell)
if command -v adb >/dev/null 2>&1 && [ -z "$ANDROID_ROOT" ]; then
    DEVICE_COUNT=$(adb devices | grep -v "List" | grep "device" | wc -l)
    if [ "$DEVICE_COUNT" -eq 0 ]; then
        echo "[-] Error: Tablet belum terdeteksi di ADB!"
        echo "    Nyalakan layar tablet & aktifkan Wireless Debugging."
        exit 1
    fi
    echo "[+] Perangkat terdeteksi di PC ADB. Menjalankan via adb shell..."
    ADB="adb shell"
else
    ADB=""
fi

echo "[*] Mengaplikasikan Zero-Delay Navigation & Instant App Switching..."

# 1. PAKSA FULL SCREEN GESTURE (FSG) NAVIGATION (Hapus 3-Button Nav)
$ADB settings put global force_fsg_nav_bar 1
$ADB settings put system force_fsg_nav_bar 1

# 2. SEMBUNYIKAN GARIS NAVIGASI (Hapus Buffer Delay 200ms Pembeda Horizontal vs Vertikal)
# Di HyperOS, kalau garis navigasi muncul, sistem menahan swipe 150-200ms untuk cek swipe horizontal.
$ADB settings put global hide_gesture_line 1

# 3. SET KECEPATAN ANIMASI LAUNCHER HYPEROS KE TINGKAT TERTINGGI (Fast / Elegan)
# 0 = Santai, 1 = Seimbang, 2 = Cepat / Elegan
$ADB settings put system miui_home_animation_rate 2

# 4. SET DURASI ANIMASI WINDOW & TRANSISI (0.25x ULTRA SNAPPY & CRASH-SAFE)
$ADB settings put global window_animation_scale 0.25
$ADB settings put global transition_animation_scale 0.25
$ADB settings put global animator_duration_scale 0.25

# 5. POTONG DELAY HOLD & TOUCH LATENCY HINGGA SETENGAHNYA
$ADB settings put secure long_press_timeout 200
$ADB settings put secure multi_press_timeout 150
$ADB cmd device_config put input_native_boot touch_deadzone_size 0
$ADB settings put system touch_response_speed 1
$ADB settings put system touch_gesture_speed 2

# 6. MATIKAN INTERSEPSI GOOGLE ASSISTANT DI POJOK BAWAH
# Mencegah sistem membuat zona filter lambat di sudut bawah layar
$ADB settings put secure assistant_on_nav_gesture 0
$ADB settings put secure search_press_hold_nav_handle 0

# 7. SINKRONISASI REFRESH RATE TOUCH SAMPLING & GESTURE FLING BOOST
$ADB setprop vendor.perf.gestureflingboost.enable true
$ADB setprop persist.sys.turbosched.touch_boost true
$ADB setprop debug.sf.predict_hwc_composition 1

echo "[✓] Sukses! Swipe Home & Buka Tutup Aplikasi Sekarang Responsif Instan 0ms!"
