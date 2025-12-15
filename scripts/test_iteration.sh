  #!/bin/bash
  ./gradlew assembleDebug && \
  adb install -r app/build/outputs/apk/debug/app-debug.apk && \
  adb shell am instrument -w -r -e class cut.the.crap.compose.
  ComposeBrowseViewScreenshotTest#captureDetailView
  cut.the.crap.test/androidx.test.runner.AndroidJUnitRunner >
  /dev/null 2>&1 && \
  sleep 2 && \
  adb pull /sdcard/Android/data/cut.the.crap/files/test-screen
  shots/$(adb shell ls -t
  /sdcard/Android/data/cut.the.crap/files/test-screenshots/ |
  head -1 | tr -d '\r') tmp/detail_latest.png