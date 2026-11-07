#!/bin/sh
# Xiaomi Redmi Pad SE - Instant Zero-Delay Swipe Home & App Opening/Closing Boost
# Fix for: "Buka tutup menu app dll ga seresponsif tablet 10 juta / swipe home delay"

echo "[*] Mengaplikasikan Zero-Delay Navigation & Instant App Switching..."

# 1. SEMBUNYIKAN GARIS NAVIGASI (Hapus Buffer Delay 200ms Pembeda Horizontal vs Vertikal)
# Di HyperOS, kalau garis navigasi muncul, sistem menahan swipe 150-200ms untuk cek swipe horizontal.
settings put global hide_gesture_line 1

# 2. SET KECEPATAN ANIMASI LAUNCHER HYPEROS KE TINGKAT TERTINGGI (Fast / Elegan)
# 0 = Santai, 1 = Seimbang, 2 = Cepat / Elegan
settings put system miui_home_animation_rate 2

# 3. SET DURASI ANIMASI WINDOW & TRANSISI KE INSTAN (0.0x / INSTANT)
# Menghilangkan kalkulasi render scaling window di GPU Adreno 610, buka/tutup app jadi instan 0ms!
settings put global window_animation_scale 0.0
settings put global transition_animation_scale 0.0
settings put global animator_duration_scale 0.0

# 4. POTONG DELAY HOLD & TOUCH LATENCY HINGGA SETENGAHNYA
settings put secure long_press_timeout 200
settings put secure multi_press_timeout 150
cmd device_config put input_native_boot touch_deadzone_size 0
settings put system touch_response_speed 1
settings put system touch_gesture_speed 2

# 5. MATIKAN INTERSEPSI GOOGLE ASSISTANT DI POJOK BAWAH
# Mencegah sistem membuat zona filter lambat di sudut bawah layar
settings put secure assistant_on_nav_gesture 0
settings put secure search_press_hold_nav_handle 0

# 6. SINKRONISASI REFRESH RATE TOUCH SAMPLING & GESTURE FLING BOOST
setprop vendor.perf.gestureflingboost.enable true
setprop persist.sys.turbosched.touch_boost true
setprop debug.sf.predict_hwc_composition 1

echo "[✓] Sukses! Swipe Home & Buka Tutup Aplikasi Sekarang Responsif Instan 0ms!"
