#!/bin/bash
# ============================================================
# STMNA Voice — Push-to-talk transcription script (Linux)
# ============================================================
# First press:  starts recording
# Second press: stops recording, uploads, pastes result
#
# Transcribed text is ALWAYS copied to clipboard.
# In terminals: ctrl+shift+v to paste manually.
# In other apps: auto-pasted via xdotool.
#
# Configuration: set STMNA_VOICE_TOKEN and STMNA_VOICE_ENDPOINT
# as environment variables, or create ~/.config/stmna-voice/config
# ============================================================

# ---- Configuration ----
# Load config file if it exists
CONFIG_FILE="${XDG_CONFIG_HOME:-$HOME/.config}/stmna-voice/config"
if [ -f "$CONFIG_FILE" ]; then
  # shellcheck source=/dev/null
  source "$CONFIG_FILE"
fi

# Environment variables override config file
TOKEN="${STMNA_VOICE_TOKEN:-}"
ENDPOINT="${STMNA_VOICE_ENDPOINT:-}"

if [ -z "$TOKEN" ] || [ -z "$ENDPOINT" ]; then
  notify-send -u critical "STMNA Voice: Missing configuration" \
    "Set STMNA_VOICE_TOKEN and STMNA_VOICE_ENDPOINT in environment or $CONFIG_FILE" -t 5000
  exit 1
fi

PIDFILE="/tmp/voice-recording.pid"
WAVFILE="/tmp/voice-recording.wav"
CURL_TIMEOUT=120
MAX_RETRIES=1

# ---- Function: upload and transcribe ----
upload_and_transcribe() {
  local attempt=$1
  local max_attempts=$((MAX_RETRIES + 1))

  # Upload with timeout, capture HTTP code on last line
  local HTTP_RESPONSE
  HTTP_RESPONSE=$(curl -s -w "\n%{http_code}" --max-time "$CURL_TIMEOUT" \
    -X POST "$ENDPOINT" \
    -H "Authorization: Bearer $TOKEN" \
    -F "file=@$WAVFILE")
  local CURL_EXIT=$?

  # Split response body and HTTP status code
  local HTTP_CODE
  HTTP_CODE=$(echo "$HTTP_RESPONSE" | tail -1)
  local BODY
  BODY=$(echo "$HTTP_RESPONSE" | sed '$d')

  # Check curl-level failure (network, timeout, etc.)
  if [ "$CURL_EXIT" -ne 0 ]; then
    if [ "$attempt" -lt "$max_attempts" ]; then
      notify-send -u low "Voice: Upload failed (curl $CURL_EXIT), retrying..." -t 2000
      sleep 2
      upload_and_transcribe $((attempt + 1))
      return $?
    fi
    local ERR="[VOICE ERROR] Upload failed after $max_attempts attempts (curl exit $CURL_EXIT)"
    printf '%s' "$ERR" | xclip -selection clipboard
    printf '%s' "$ERR" | xclip -selection primary
    notify-send -u critical "$ERR" -t 5000
    return 1
  fi

  # Check HTTP status
  if [ "$HTTP_CODE" != "200" ]; then
    if [ "$attempt" -lt "$max_attempts" ]; then
      notify-send -u low "Voice: Server error $HTTP_CODE, retrying..." -t 2000
      sleep 2
      upload_and_transcribe $((attempt + 1))
      return $?
    fi
    local ERR="[VOICE ERROR] Server error $HTTP_CODE after $max_attempts attempts. Body: ${BODY:0:200}"
    printf '%s' "$ERR" | xclip -selection clipboard
    printf '%s' "$ERR" | xclip -selection primary
    notify-send -u critical "Voice: Server error $HTTP_CODE after $max_attempts attempts" -t 5000
    return 1
  fi

  # Parse JSON response
  local TEXT
  TEXT=$(echo "$BODY" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    print(data.get('text', ''))
except:
    print('')
" 2>/dev/null)

  if [ -z "$TEXT" ]; then
    local ERR="[VOICE ERROR] Empty transcript. HTTP $HTTP_CODE. Body: ${BODY:0:200}"
    printf '%s' "$ERR" | xclip -selection clipboard
    printf '%s' "$ERR" | xclip -selection primary
    notify-send -u critical "Voice: Empty transcript returned" -t 5000
    return 1
  fi

  # -- Success: copy to both clipboards --
  # CLIPBOARD = ctrl+v / ctrl+shift+v
  # PRIMARY   = middle-click paste
  printf '%s' "$TEXT" | xclip -selection clipboard
  printf '%s' "$TEXT" | xclip -selection primary
  sleep 0.1

  # Try auto-paste (works in GUI apps, not terminals)
  xdotool key ctrl+v

  # Success notification — so you know clipboard is ready
  # (useful when auto-paste doesn't work, e.g. in terminal)
  local PREVIEW="${TEXT:0:60}"
  [ ${#TEXT} -gt 60 ] && PREVIEW="${PREVIEW}..."
  notify-send -u low -h string:sound-name:none "Voice: $PREVIEW" -t 3000

  return 0
}

# ---- Main logic ----
if [ -f "$PIDFILE" ]; then
  # Stop recording
  kill "$(cat "$PIDFILE")" 2>/dev/null
  rm -f "$PIDFILE"
  sleep 0.3

  # Sanity check: recording file exists
  if [ ! -f "$WAVFILE" ]; then
    ERR="[VOICE ERROR] No recording file found at $WAVFILE"
    printf '%s' "$ERR" | xclip -selection clipboard
    printf '%s' "$ERR" | xclip -selection primary
    notify-send -u critical "$ERR" -t 3000
    exit 1
  fi

  FSIZE=$(stat -c%s "$WAVFILE" 2>/dev/null || echo 0)
  notify-send -u low -h string:sound-name:none "Transcribing ($(( FSIZE / 1024 ))kB)..." -t 5000

  # Upload (with auto-retry on failure)
  if upload_and_transcribe 1; then
    # Success — clean up WAV
    rm -f "$WAVFILE"
  else
    # Failed — keep WAV for debugging
    notify-send -u critical "Voice: Recording kept at $WAVFILE" -t 5000
  fi
else
  # Start recording
  arecord -f S16_LE -r 16000 -c 1 "$WAVFILE" &
  echo $! > "$PIDFILE"
  notify-send -u low -h string:sound-name:none "Recording..." -t 1000
fi
