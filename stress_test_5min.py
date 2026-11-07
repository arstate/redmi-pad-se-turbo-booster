#!/usr/bin/env python3
import subprocess
import time
import sys

TARGET_DEVICE = "192.168.1.21:39475"

APPS = [
    ("YouTube", "monkey -p com.google.android.youtube -c android.intent.category.LAUNCHER 1"),
    ("Mobile Legends", "am start -n com.mobile.legends/com.moba.unityplugin.MobaGameMainActivityWithExtractor"),
    ("Free Fire Max", "monkey -p com.dts.freefiremax -c android.intent.category.LAUNCHER 1"),
    ("WhatsApp", "monkey -p com.whatsapp -c android.intent.category.LAUNCHER 1"),
    ("Chrome", "monkey -p com.android.chrome -c android.intent.category.LAUNCHER 1"),
    ("Free Fire Max", "monkey -p com.dts.freefiremax -c android.intent.category.LAUNCHER 1"),
    ("Mobile Legends", "am start -n com.mobile.legends/com.moba.unityplugin.MobaGameMainActivityWithExtractor"),
]

def adb_cmd(cmd):
    full_cmd = ["adb", "-s", TARGET_DEVICE, "shell", cmd]
    return subprocess.run(full_cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)

print("[*] MEMULAI STRESS TEST MULTITASKING 5 MENIT PADA OPPO A60...", flush=True)
start_time = time.time()
duration = 300  # 5 minutes
cycle = 1

while time.time() - start_time < duration:
    elapsed = int(time.time() - start_time)
    print(f"\n--- SIKLUS {cycle} (Elapsed: {elapsed}/{duration}s) ---", flush=True)
    
    for name, cmd in APPS:
        if time.time() - start_time >= duration:
            break
        print(f"[{int(time.time() - start_time)}s] Membuka {name}...", flush=True)
        adb_cmd(cmd)
        time.sleep(5)  # Biarkan app render dan load memori
        
    cycle += 1

print("\n[✓] STRESS TEST 5 MENIT SELESAI DENGAN SUKSES!", flush=True)
