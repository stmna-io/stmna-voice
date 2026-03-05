---
title: "STMNA Voice Install Guide"
repo: stmna-voice
prereq: "stmna-desk install guide, Core + Automation tiers"
validated: staging
updated: 2026-03-05
---

# STMNA Voice Install Guide

> By the end of this guide, you will have the STMNA Voice transcription pipeline running: record audio on your phone or Linux desktop, get back a structured transcription processed by a local LLM.
>
> Tested on Ubuntu 24.04 LTS, deployed via Dockge on a staging VM (10.0.10.55) during SB-06.

## Prerequisites

| Requirement | Where to get it |
|-------------|----------------|
| STMNA Desk, Core + Automation tiers (Steps 1-8) | [Desk install guide](https://f.slowdawn.cc/stmna-io/stmna-desk/src/branch/main/docs/install-guide.md) |
| Whisper server running | Desk install guide, Step 8 |
| n8n running with custom image | Desk install guide, Step 7 |
| llama-swap running with at least one LLM | Desk install guide, Step 5 |
| A client device | Android phone ([app guide](app-guide.md)) or Linux desktop ([Linux guide](linux-guide.md)) |

No additional Desk services are needed beyond Core + Automation.

If installing on other infrastructure: see [Running on Other Infrastructure](#running-on-other-infrastructure) below.

---

## Step 1: Configure n8n Credentials

Open n8n at `http://YOUR_IP:5678` and create the following credential:

| Credential Name | Type | Values |
|----------------|------|--------|
| Postgres Signal | PostgreSQL | Host: `postgres-voice`, Port: `5432`, Database: `stmna_signal`, User: `voice`, Password: your postgres password |

> **Note:** If you already configured this credential for the Signal pipeline, you do not need to create it again. The Voice workflow uses the same credential.

---

## Step 2: Import the Voice Workflow

Import `stmna-voice.json` from the `backend/workflows/` directory.

In n8n, go to Workflows > Import from File and select the JSON file.

After importing, **re-link credentials manually**: open each credential node (they will show a red warning), select "Postgres Signal" from the dropdown, and save. Sanitized workflow files contain placeholder credential IDs that do not exist on your instance.

Verify after re-linking:
- All credential nodes show green (no warnings)
- The Whisper endpoint URL points to your Whisper server (`http://whisper-voice:8083/v1/audio/transcriptions`)
- The LLM endpoint URL points to your llama-swap instance (`http://llama-swap:8080/v1`)

---

## Step 3: Smoke Test

### Test the Whisper endpoint

```bash
# Create a short test audio file (silence)
podman exec n8n sh -c "dd if=/dev/zero bs=1 count=32000 2>/dev/null | \
  ffmpeg -f s16le -ar 16000 -ac 1 -i - -y /tmp/test.wav 2>/dev/null"

# Send it to Whisper
podman exec n8n sh -c "wget -q -O- --post-file=/tmp/test.wav \
  --header='Content-Type: audio/wav' \
  'http://whisper-voice:8083/v1/audio/transcriptions'"
```

**Expected result:** A JSON response with a `text` field (likely empty for silence, but no errors).

### Test via client

**Android:** Install the STMNA Voice app (see [app guide](app-guide.md)), connect it to your Desk, and record a short voice note.

**Linux:** Install the voice.sh client (see [Linux guide](linux-guide.md)), configure your endpoint and token, and press the shortcut to record.

Check the n8n execution log to verify the pipeline ran successfully.

---

## Troubleshooting

### Whisper returns "model not found" or connection refused

**Cause:** Whisper server is not running, or the model file is missing.

**Fix:** Check that `whisper-voice` is running and the model file exists:

```bash
podman ps --filter name=whisper-voice
podman logs whisper-voice 2>&1 | tail -5
```

The model file (`ggml-large-v3-turbo-q5_0.bin`) must exist in your models directory.

### n8n workflow fails at LLM step

**Cause:** llama-swap is not running or has no model loaded.

**Fix:** Verify llama-swap is healthy and has at least one model configured:

```bash
curl -s http://localhost:8081/v1/models
```

---

## Running on Other Infrastructure

This guide is written and tested against STMNA Desk. If running on other infrastructure, adapt the following:

| Dependency | What to substitute |
|------------|-------------------|
| llama-swap (local inference) | Any OpenAI-compatible inference endpoint |
| Whisper server (local) | Any Whisper-compatible transcription API (OpenAI, Groq) |
| PostgreSQL | Any PostgreSQL 15+ instance |

This path is community-supported. The STMNA team validates against Desk only.

---

## What's Next

- [Linux guide](linux-guide.md) -- install and configure the Linux desktop client
- [App guide](app-guide.md) -- install and configure the Android app
- [Desk install guide](https://f.slowdawn.cc/stmna-io/stmna-desk/src/branch/main/docs/install-guide.md) -- full infrastructure setup
- [Signal install guide](https://f.slowdawn.cc/stmna-io/stmna-signal/src/branch/main/docs/install-guide.md) -- add the Signal content pipeline
