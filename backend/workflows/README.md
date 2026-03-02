# STMNA Voice — Workflow Reference

This directory contains the n8n workflow for the STMNA Voice transcription pipeline.

---

## Workflow Overview

```
Incoming audio file (POST /webhook/voice)
        │
        ▼
┌──────────────────────────────┐
│   STMNA_Voice Transcription  │
│                              │
│  1. Authenticate request     │
│  2. Receive audio binary     │
│  3. Send to whisper.cpp      │
│  4. Clean transcript (LLM)   │
│  5. Log to PostgreSQL        │
│  6. Return transcript        │
└──────────────────────────────┘
```

| File | Workflow | Trigger | Purpose |
|------|----------|---------|---------|
| `stmna-voice.json` | STMNA_Voice Transcription | Webhook (POST) | Receives audio, transcribes with whisper.cpp, cleans with LLM, stores result |

---

## Prerequisites

### Services

| Service | Required? | Notes |
|---------|-----------|-------|
| [whisper.cpp server](https://github.com/ggerganov/whisper.cpp) | Yes | Runs the actual transcription |
| [llama-swap](https://github.com/mostlygeek/llama-swap) or compatible OpenAI API | Yes | Cleans/formats raw transcripts |
| PostgreSQL 15+ | Yes | Stores transcription results and metrics |

### n8n Requirements

- n8n **1.75+**
- Custom n8n image with `ffmpeg` installed (required for audio format handling)
- Environment variable: `NODE_FUNCTION_ALLOW_BUILTIN=fs,child_process,path`

See [../config/](../config/) for the custom n8n Dockerfile used with this pipeline.

### Database

The workflow logs to a PostgreSQL database. Schema:

```bash
psql -U postgres -d stmna_voice -f ../../sql/voice-schema.sql
```

---

## Import Instructions

1. In n8n, go to **Settings → Import workflow**
2. Import `stmna-voice.json`
3. Reassign credentials after import (see below)
4. Set your webhook URL and configure the bearer token
5. **Activate** the workflow

---

## Required Credentials

Create these credential types in n8n (**Settings → Credentials → Add credential**), then reassign them after import.

### `Postgres` credential
Point to your `stmna_voice` database.

| Nodes using this credential |
|-----------------------------|
| Save Training Pair |
| Save Latency Metrics |

These nodes are optional — they log transcription pairs and latency for model evaluation. The core transcription path works without them.

---

## Environment Variables

Copy `.env.example` to `.env` and fill in all values.

The workflow requires these to be set in your n8n environment:

```env
WHISPER_URL=http://your-whisper:8083
LLAMA_SWAP_URL=http://your-llama-swap:8081
VOICE_BEARER_TOKEN=your-bearer-token
```

---

## Sending Audio

The workflow expects a `multipart/form-data` POST to the n8n webhook URL:

```bash
curl -X POST https://your-n8n/webhook/voice \
  -H "Authorization: Bearer YOUR_BEARER_TOKEN" \
  -F "audio=@recording.m4a"
```

Supported formats: anything ffmpeg can decode (m4a, mp3, wav, ogg, webm).

The response contains the cleaned transcript as JSON:

```json
{
  "transcript": "...",
  "duration_ms": 4200,
  "model": "whisper-large-v3"
}
```

---

## Adapting to Your Setup

- **Different STT backend:** Replace the whisper.cpp HTTP call with any API that accepts audio and returns a transcript string
- **Different LLM:** Replace `LLAMA_SWAP_URL` with any OpenAI-compatible endpoint and set your model name
- **No PostgreSQL:** Remove the logging nodes — the core transcription path works without them
