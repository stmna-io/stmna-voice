---
title: "STMNA Voice  -- Linux Client Guide"
repo: stmna-voice
updated: 2026-03-05
---

# STMNA Voice  -- Linux Client Guide

> Push-to-talk transcription for Linux desktops. Press a key to record, press again to transcribe and paste at your cursor.

## Prerequisites

| Requirement | Where to get it |
|-------------|----------------|
| STMNA Voice backend running | [Install guide](install-guide.md) |
| X11 display server | Required for xdotool auto-paste (Wayland not supported) |
| Dependencies (see below) | `apt install` |

> **Wayland limitation:** `xdotool` does not work on Wayland. The clipboard copy still works (via `xclip`), but auto-paste at cursor will not function. You will need to paste manually with Ctrl+V.

### Install dependencies

```bash
sudo apt install -y alsa-utils curl xclip xdotool python3 libnotify-bin
```

| Package | Purpose |
|---------|---------|
| `alsa-utils` | `arecord` for audio recording |
| `curl` | HTTP upload to backend |
| `xclip` | Clipboard access (CLIPBOARD + PRIMARY) |
| `xdotool` | Simulate Ctrl+V paste at cursor |
| `python3` | Parse JSON response |
| `libnotify-bin` | Desktop notifications (`notify-send`) |

---

## Installation

### 1. Copy the script

```bash
mkdir -p ~/bin
cp desktop/voice.sh ~/bin/voice.sh
chmod +x ~/bin/voice.sh
```

### 2. Configure

Create the config file:

```bash
mkdir -p ~/.config/stmna-voice
cat > ~/.config/stmna-voice/config << 'EOF'
# STMNA Voice configuration
# Get your token from the n8n Voice workflow webhook settings
STMNA_VOICE_TOKEN="your-token-here"

# Your STMNA Desk endpoint
# LAN: http://YOUR_DESK_IP:5678/webhook/YOUR_WEBHOOK_PATH
# Public: https://stv.yourdomain.com/v1/audio/transcriptions
STMNA_VOICE_ENDPOINT="https://stv.yourdomain.com/v1/audio/transcriptions"
EOF
```

> **Required:** Replace the token and endpoint with your actual values from the STMNA Voice backend setup.

Alternatively, set environment variables instead of using the config file:

```bash
export STMNA_VOICE_TOKEN="your-token-here"
export STMNA_VOICE_ENDPOINT="https://stv.yourdomain.com/v1/audio/transcriptions"
```

### 3. Bind a keyboard shortcut

The script is designed to be triggered by a system keyboard shortcut. Each press toggles recording on/off.

**Linux Mint / Cinnamon:**
1. System Settings > Keyboard > Shortcuts > Custom Shortcuts
2. Add: Name `STMNA Voice`, Command `/home/YOUR_USER/bin/voice.sh`
3. Click to assign a key (e.g., `Super+V` or a dedicated media key)

**GNOME:**
1. Settings > Keyboard > Custom Shortcuts
2. Add: Name `STMNA Voice`, Command `/home/YOUR_USER/bin/voice.sh`
3. Set shortcut key

**i3 / Sway:**
```
bindsym $mod+v exec ~/bin/voice.sh
```

### 4. Test

Press your shortcut once  -- you should see a "Recording..." notification.

Press again  -- you should see "Transcribing..." followed by the transcribed text pasted at your cursor.

---

## How It Works

1. **First press:** Starts `arecord` (16-bit PCM, 16kHz, mono) recording to `/tmp/voice-recording.wav`
2. **Second press:** Stops recording, uploads the WAV file to your STMNA Voice endpoint via HTTP POST
3. **On success:** Transcribed text is copied to both X11 clipboards (CLIPBOARD + PRIMARY) and auto-pasted via `xdotool key ctrl+v`
4. **On failure:** Error message is copied to clipboard, desktop notification shows the error, WAV file is kept at `/tmp/voice-recording.wav` for debugging

### Auto-paste behavior

- **GUI apps** (browser, editor, chat): auto-paste works via `xdotool`
- **Terminals**: auto-paste does NOT work (terminals use Ctrl+Shift+V). Paste manually.
- **All apps**: text is always in clipboard regardless of auto-paste

---

## Troubleshooting

### "Missing configuration" notification on first press

**Cause:** `STMNA_VOICE_TOKEN` or `STMNA_VOICE_ENDPOINT` is not set.

**Fix:** Create `~/.config/stmna-voice/config` with both values (see Installation step 2).

### Recording starts but no transcription

**Cause:** The backend is unreachable, or the token is invalid.

**Fix:** Test the endpoint manually:

```bash
# Record a short clip
arecord -f S16_LE -r 16000 -c 1 -d 3 /tmp/test.wav

# Upload it
curl -v -X POST "$STMNA_VOICE_ENDPOINT" \
  -H "Authorization: Bearer $STMNA_VOICE_TOKEN" \
  -F "file=@/tmp/test.wav"
```

### Auto-paste does not work

**Cause:** You are on Wayland, or the focused app intercepts Ctrl+V differently.

**Fix:** Paste manually with Ctrl+V (or Ctrl+Shift+V in terminals). The text is always in the clipboard.

### "No recording file found" error

**Cause:** `arecord` failed to start or the audio device is unavailable.

**Fix:** Check your audio input:

```bash
arecord -l    # list available capture devices
arecord -f S16_LE -r 16000 -c 1 -d 2 /tmp/test.wav   # test recording
```

If no capture device is found, check that your microphone is connected and not muted in your system audio settings.
