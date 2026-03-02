# Mobile Build Guide

<!-- TODO: Write full mobile build guide for SB-09 session -->
<!-- Content: Expo/React Native build environment setup, EAS Build vs local build,
     APK generation, sideloading on Android, configuring server URL + auth token
     in app settings, offline fallback (bundled Whisper small model),
     scrcpy setup for demo recording, known issues with Android audio permissions -->

## Placeholder

This document will cover:

### Prerequisites

- Node.js 20+ and npm
- Expo CLI
- Android device with developer mode enabled (for sideloading APK)
- (Optional) EAS Build account for cloud builds

### Build

```bash
cd mobile/

# Install dependencies
npm install

# Local APK build (requires Android SDK)
npx expo build:android --type apk

# Or cloud build via EAS
eas build --platform android --profile preview
```

### Install

```bash
# Enable "Install unknown apps" on Android
adb install stmna-voice.apk
# Or transfer APK via file manager and tap to install
```

### Configure

In the app settings:
- **Server URL**: `https://stv.yourdomain.com/v1/audio/transcriptions`
- **Bearer Token**: your token from `.env`
- **Mode**: Online (default) or Offline (bundled Whisper small model)

### Offline Fallback

The app bundles Whisper small model for offline use. Accuracy is lower than the server-side large-v3-turbo, but works without network connectivity.

### Recording Demo with scrcpy

```bash
# Mirror Android screen on Linux
scrcpy --record voice-demo.mp4

# Then convert to GIF
ffmpeg -i voice-demo.mp4 -vf "fps=15,scale=480:-1" voice-demo.gif
```

Coming in SB-09 session.
