# Edge Launcher

A small Android overlay service that treats configured applications as an ordered workspace carousel.

## Current development build

- Kotlin, min SDK 23, target SDK 33
- Up to three configurable workspace apps in the UI
- Carousel core supports any number of slots
- Configurable two/three-finger navigation
- Configurable swipe sensitivity and edge touch-zone width
- Two/three-finger long-press opens settings
- Left-edge inward swipe selects the previous workspace
- Right-edge inward swipe selects the next workspace
- Carousel wraps in both directions
- Selected default app launches after boot
- Missing apps are skipped; settings opens if none remain
- Edge handles are flush, grayscale, rounded on the inside edge, elevated, and fade after interaction

## Build and test

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME="$HOME/Library/Android/sdk"

cd gesture-switcher
./gradlew testDebugUnitTest assembleDebug
```

Run instrumented tests on the dedicated emulator only:

```sh
export ANDROID_SERIAL=emulator-5554
./gradlew connectedDebugAndroidTest
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Emulator

The development AVD is `EdgeLauncher_API35_Tablet`, configured as a 1920x1080 landscape tablet with 2 GB RAM.

## Safety

Edge Launcher does not replace Android Home, root the device, use Accessibility, or require network/storage permissions. It requires `Display over other apps` and runs a foreground service. Uninstalling it removes the service and configuration.
