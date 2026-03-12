---
title: "STMNA Voice App Guide"
repo: stmna-voice
updated: 2026-03-05
---

# STMNA Voice App Guide

> Install the STMNA Voice keyboard on your Android phone, connect it to your Desk, and start dictating. Text appears wherever your cursor is.

The app is a custom build of Whisper-to-Input, rebranded with STMNA's identity and keyboard design. It registers as an Android keyboard (IME) that records your voice, sends audio to your Desk for transcription, and types the result directly into whatever app you're using.

Your audio never leaves your network. The phone is a thin client. All processing happens on your hardware.

---

## Get the APK

The app is distributed as an APK file. Three options:

| Method | Where |
|--------|-------|
| Sideload | Download `app-release.apk` from the [mobile/ directory](../mobile/app-release.apk) or [releases page](https://f.slowdawn.cc/stmna-io/stmna-voice/releases) |
| Google Play Store | Coming soon |
| Accrescent | Coming soon |

---

## Install (sideloading)

### Step 1: Transfer the APK to your phone

Either:
- Download directly on your phone from the releases page above
- Transfer from your computer via USB, email, or file manager

### Step 2: Allow unknown apps

Android blocks APKs from outside the Play Store by default. When you tap the APK file, Android will show an "Unsafe app blocked" warning.

1. Tap **More details**
2. Tap **Install anyway**

> **Note:** On some Android versions, you need to go to Settings > Apps > Special app access > Install unknown apps and enable it for your file manager or browser first.

### Step 3: Install and grant permissions

1. Tap **Install**
2. When prompted, allow **Record Audio** (required for voice input)
3. Allow **Notifications** (shows status messages if something goes wrong)

---

## Connect to your Desk

Open the STMNA Voice app. You will see the settings screen.

### Configuration

| Field | Value |
|-------|-------|
| Speech to Text Backend | **OpenAI API** |
| Endpoint | `https://stv.yourdomain.com/v1/audio/transcriptions` |
| API Key | Your bearer token (from your Caddy or n8n auth setup) |
| Model | Leave empty |
| Language Code | Leave empty (auto-detected) |

> **Local network alternative:** If your phone is on the same network as your Desk (Wi-Fi at home), you can use the LAN address directly: `http://YOUR_DESK_IP:5678/webhook/transcribe`. This skips Caddy and the internet entirely.

Tap **Apply**. The app tests the connection and shows a toast message:
- "Connection successful!" means you're good
- "Server reachable (HTTP 405)" is also fine (the server is there but expects audio, not an empty POST)
- "Connection failed" means check your endpoint URL, network, and whether n8n is running

### Settings reference

| Setting | What it does | Recommended |
|---------|-------------|-------------|
| Auto Recording Start | Start recording as soon as you switch to the keyboard | On |
| Auto Switch Back | Return to your previous keyboard after transcription | Off (try it, some people prefer it) |
| Add Trailing Space | Add a space after each transcription | Off |
| Post-processing | Chinese character conversion | No Conversion (unless you need it) |
| Alt Theme | Switch between light and dark keyboard themes | Your preference |

---

## Enable the keyboard

The app needs to be registered as an input method before you can use it.

1. Go to **Settings > System > Languages and input > On-screen keyboard** (path varies by Android version and manufacturer)
2. Find **STMNA Voice** in the list
3. Enable it
4. Android will warn you that the keyboard can observe all input. This is standard for any keyboard app. Tap **OK**.

---

## Use it

1. Open any app with a text field (messaging, browser, notes, email)
2. Tap the text field to bring up the keyboard
3. Tap the keyboard icon in the navigation bar (bottom right on most phones) and select **STMNA Voice**
4. The voice keyboard appears with a microphone button in the center

### Keyboard layout

- **Microphone** (center): Tap to start recording, tap again to stop. Your speech is sent to your Desk and the transcription appears in the text field.
- **Backspace** (upper right): Delete characters. Hold to keep deleting.
- **Enter** (lower right): Insert a newline. If pressed while recording, stops recording and adds a newline after the transcription.
- **Settings gear** (upper left): Opens the settings screen.
- **Globe/switch** (upper left): Switch to your previous keyboard.

### Tips

- Keep recordings under 30 seconds for best results. The pipeline handles longer audio, but latency scales with duration.
- The app works in any language your Whisper model supports. Language is auto-detected per recording.
- If a transcription seems wrong, just backspace and try again. Short phrases (under 2 seconds) are less reliable due to a known Whisper limitation.
- For terminal-style apps where the keyboard doesn't appear, you may need to use a floating keyboard app or switch input methods manually.

---

## Troubleshooting

### "Connection failed" on Apply

- **Check the URL:** Make sure it includes the full path (`/v1/audio/transcriptions` or `/webhook/transcribe`)
- **Check your network:** Is your phone on the same network as your Desk? Can you open the n8n UI in your phone's browser?
- **Check n8n:** Is the Voice workflow active? Go to `http://YOUR_DESK_IP:5678` and verify.
- **HTTPS required for external access:** If connecting over the internet (not LAN), you need Caddy or another reverse proxy handling TLS.

### Transcription returns empty or garbage

- **Short audio:** Recordings under 2 seconds trigger Whisper hallucination. Try speaking a full sentence.
- **Background noise:** Find a quieter spot or speak closer to the microphone.
- **Model not loaded:** Check that llama-swap and Whisper are running on your Desk. The Voice workflow needs both.

### Keyboard doesn't appear in the list

- Go to Settings > Apps > STMNA Voice > Permissions and make sure nothing is disabled
- Try restarting your phone after installation

### Audio permission denied

If you accidentally denied the microphone permission:
1. Go to Settings > Apps > STMNA Voice > Permissions
2. Enable Microphone
3. Restart the app

---

## Offline fallback

The STMNA Voice app does not currently include an offline mode. If your Desk is unreachable, the keyboard will show a connection error.

For offline voice input, install [FUTO Voice Input](https://voiceinput.futo.org/) as a fallback keyboard. It runs Whisper on-device (smaller model, lower accuracy, no LLM post-processing) but works without any server.

---

## What's next

- [Voice install guide](install-guide.md) -- set up the server-side pipeline
- [Desk install guide](https://f.slowdawn.cc/stmna-io/stmna-desk/src/branch/main/docs/install-guide.md) -- full infrastructure setup
