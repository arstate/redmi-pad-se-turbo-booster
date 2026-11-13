#!/usr/bin/env bash
set -e
export ANDROID_NDK_HOME="${ANDROID_NDK_HOME:-/mnt/HDD-2/SOFTWARE INSTALL/LINUX APP/android-sdk/ndk/27.2.12479018}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "[*] Building Rust TurboDaemon (aarch64-linux-android)..."
cd "$ROOT_DIR/turbodaemon"
cargo ndk -t arm64-v8a -P 26 build --release

mkdir -p "$ROOT_DIR/app/src/main/jniLibs/arm64-v8a"
cp "$ROOT_DIR/turbodaemon/target/aarch64-linux-android/release/libturbodaemon" "$ROOT_DIR/app/src/main/jniLibs/arm64-v8a/libturbodaemon.so"
chmod 755 "$ROOT_DIR/app/src/main/jniLibs/arm64-v8a/libturbodaemon.so"

echo "[✓] libturbodaemon.so successfully packaged to app/src/main/jniLibs/arm64-v8a/libturbodaemon.so"
ls -lh "$ROOT_DIR/app/src/main/jniLibs/arm64-v8a/libturbodaemon.so"
