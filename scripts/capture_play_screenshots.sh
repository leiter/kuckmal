#!/usr/bin/env bash
# Capture the Google Play phone screenshot set for Kuckmal.
#
# Expects: one connected device (adb), the app installed, and the media list
# already imported (first run takes several minutes — see PLAY_RELEASE_PLAN.md).
#
# Coordinates below are for a 1080x1920 screen. On a different resolution the
# taps have to be re-measured.

set -euo pipefail

PKG=cut.the.crap.kuckmal
OUT=${1:-appstore/android/screenshots/phone/de-DE}
mkdir -p "$OUT"

shot() { sleep "${2:-3}"; adb exec-out screencap -p > "$OUT/$1"; echo "  → $1"; }
tap()  { adb shell input tap "$1" "$2"; }

echo "Cleaning up the status bar (SysUI demo mode)…"
adb shell settings put global sysui_demo_allowed 1
demo() { adb shell am broadcast -a com.android.systemui.demo "$@" >/dev/null; }
demo -e command enter
demo -e command clock -e hhmm 1200
demo -e command battery -e level 100 -e plugged false
demo -e command network -e wifi show -e level 4
demo -e command network -e mobile show -e datatype none -e level 4
demo -e command notifications -e visible false

echo "Launching $PKG…"
adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 5

echo "Capturing…"
shot 01_alle_sender.png 3          # channel list + cross-broadcaster topics

tap 210 500                        # select ARD
shot 02_sender_themen.png 3

tap 880 147                        # search icon
sleep 1
adb shell input text "Tatort"
adb shell input keyevent 66        # submit
sleep 4
adb shell input keyevent 4         # hide keyboard
shot 03_suche.png 2

tap 700 940                        # open the "Tatort" topic
shot 04_sendungen.png 4

tap 740 1600                       # open an episode
shot 05_details.png 4

echo "Restoring the status bar…"
demo -e command exit

echo
echo "Done. Files in $OUT:"
ls -1 "$OUT"
echo
echo "Play requires 2-8 phone screenshots, 320-3840 px per side, ratio at most 2:1."
