  #!/bin/bash
  ./gradlew assembleDebug && \
  adb install -r app/build/outputs/apk/debug/app-debug.apk && \
  adb shell am instrument -w -r -e class com.mediathekview.android.compose.
  ComposeBrowseViewScreenshotTest#captureDetailView
  com.mediathekview.android.test/androidx.test.runner.AndroidJUnitRunner >
  /dev/null 2>&1 && \
  sleep 2 && \
  adb pull /sdcard/Android/data/com.mediathekview.android/files/test-screen
  shots/$(adb shell ls -t
  /sdcard/Android/data/com.mediathekview.android/files/test-screenshots/ |
  head -1 | tr -d '\r') tmp/detail_latest.png