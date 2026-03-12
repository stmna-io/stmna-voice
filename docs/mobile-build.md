# Mobile Build Guide

> Build the STMNA Voice Android keyboard app from source and install it on your device.

The mobile app is a Kotlin/Compose project (Android Studio). It's a fork of [Whisper-to-Input](https://github.com/j3soon/whisper-to-input), rebranded for the STMNA ecosystem.

---

## Prerequisites

| Requirement | Details |
|-------------|---------|
| Android Studio | Hedgehog (2023.1) or newer |
| JDK | 17+ (bundled with Android Studio) |
| Android SDK | API 34+ (install via SDK Manager) |
| Android device | API 31+ (Android 12) with developer mode enabled |

---

## Build

```bash
cd mobile/

# Build release APK
./gradlew assembleRelease
```

The APK is generated at:
```
app/build/outputs/apk/release/app-release.apk
```

For a debug build (no signing required):
```bash
./gradlew assembleDebug
```

---

## Install

### Via ADB (USB debugging)

```bash
adb install app/build/outputs/apk/release/app-release.apk
```

### Via sideloading

Transfer the APK to your phone (USB, file manager, email) and tap to install. You'll need to allow "Install from unknown sources" for your file manager or browser.

### Pre-built APK

A pre-built `app-release.apk` is included in the `mobile/` directory for convenience. You can sideload it directly without building from source.

---

## Configure

After installing, see [docs/app-guide.md](app-guide.md) for:
- Enabling the keyboard in Android settings
- Connecting to your STMNA Desk whisper endpoint
- Settings reference and troubleshooting

---

## Recording a Demo with scrcpy

Mirror your Android screen on Linux and record a demo:

```bash
# Mirror + record
scrcpy --record voice-demo.mp4

# Convert to GIF for README / docs
ffmpeg -i voice-demo.mp4 -vf "fps=15,scale=480:-1" voice-demo.gif
```

---

## Project Structure

```
mobile/
├── app/
│   ├── src/main/
│   │   ├── java/com/...    ← Kotlin source (IME service, UI, API client)
│   │   └── res/            ← Layouts, drawables, themes
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts         ← Root build config
├── settings.gradle.kts
├── gradle.properties
├── gradlew                   ← Gradle wrapper (use this, not system Gradle)
├── app-release.apk           ← Pre-built APK for sideloading
└── LICENSE                   ← GPLv3
```
