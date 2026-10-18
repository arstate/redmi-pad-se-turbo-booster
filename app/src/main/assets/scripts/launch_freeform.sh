#!/system/bin/sh
# Helper untuk meluncurkan aplikasi ke Jendela Bebas (AOSP Freeform Multi-Window)
# Usage: sh /sdcard/launch_freeform.sh <nama_paket_atau_alias>
PKG="$1"
case "$PKG" in
  chrome) PKG="com.android.chrome/com.google.android.apps.chrome.Main" ;;
  settings) PKG="com.android.settings/.MainSettings" ;;
  spotify) PKG="com.spotify.music/.MainActivity" ;;
  files) PKG="com.marc.files/nl.marc_apps.files.MainActivity" ;;
  calendar) PKG="com.google.android.calendar/com.android.calendar.AllInOneActivity" ;;
  zarchiver) PKG="ru.zdevs.zarchiver/ru.zdevs.zarchiver.ZArchiver" ;;
esac

if [ -z "$PKG" ]; then
  echo "Usage: sh /sdcard/launch_freeform.sh <pkg_name_or_alias>"
  exit 1
fi

if echo "$PKG" | grep -q "/"; then
  am start --windowingMode 5 -f 0x18000000 -n "$PKG"
else
  am start --windowingMode 5 -f 0x18000000 -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p "$PKG"
fi
