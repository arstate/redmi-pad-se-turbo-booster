#!/system/bin/sh
# Quad Speaker Studio Audio & Dolby Atmos 7.1 Surround Tuner for Redmi Pad SE
echo "=== Activating Quad Speaker Studio Audio Tuner ==="

# 1. Unlock Dolby Bitstream Surround Formats
cmd audio set-surround-format-enabled 5 true
cmd audio set-surround-format-enabled 6 true
cmd audio set-surround-format-enabled 18 true
cmd audio set-surround-format-enabled 19 true

# 2. Reset Encoded Surround Mode to AUTO (Anti-Duck, prevents -3dB channel attenuation)
cmd audio set-encoded-surround-mode 0

# 3. Enable Spatial Audio Services
settings put global spatial_audio_feature_enable 1
settings put global enableSpatialAudio true

echo "=== Quad Speaker Studio Audio Tuning Applied ==="
