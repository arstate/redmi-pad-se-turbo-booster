# Proguard rules for Turbo Booster
-keep class rikka.shizuku.** { *; }
-keep interface rikka.shizuku.** { *; }

# Keep App components, receivers, services, and view bindings
-keep class com.arya.turbobooster.databinding.** { *; }
-keep class com.arya.turbobooster.receiver.** { *; }
-keep class com.arya.turbobooster.MainActivity { *; }
-keep class com.arya.turbobooster.booster.** { *; }

