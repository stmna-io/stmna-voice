# STMNA_Voice Workflow Reference

This directory contains the n8n workflow for the STMNA_Voice transcription pipeline. It takes audio input via HTTP POST, transcribes it with whisper.cpp, cleans the transcript with a small LLM, and returns the result. The whole round-trip averages 2.4 seconds.

---

## Workflow Overview

```
Audio file (POST /webhook/transcribe)
        │
        ▼
┌──────────────────────────────┐
│   STMNA_Voice Transcription  │
│                              │
│  1. Set metadata + timing    │
│  2. FFmpeg convert to WAV    │
│  3. Whisper STT (bilingual)  │
│  4. Hallucination filter     │
│  5. Delimiter wrapping       │
│  6. Qwen LLM polish         │
│  7. Return transcript        │
│                              │
│  Async: archive audio to disk│
│  Async: save training pair   │
│  Async: log latency metrics  │
└──────────────────────────────┘
```

| File | Workflow | Nodes | Trigger | Purpose |
|------|----------|-------|---------|---------|
| `stmna-voice.json` | STMNA_Voice Transcription | 19 | Webhook (POST) | Receives audio, transcribes, cleans, archives, logs, returns transcript |

---

## Pipeline Details

### Audio Processing

The workflow accepts any audio format ffmpeg can decode (m4a, mp3, wav, ogg, webm). FFmpeg converts the input to 16kHz mono WAV before sending to whisper.cpp.

### Whisper Transcription

Sends audio to a dedicated whisper.cpp server with tuned parameters:

- Response format: `verbose_json` (includes segment-level confidence scores)
- Bilingual prompt with domain vocabulary (reduces hallucination on mixed FR/EN input)
- Anti-hallucination settings: `entropy_thold=2.0`, `no_speech_thold=0.8`, `suppress_nst=true`, `temperature=0.0`

### Hallucination Filter

Five detection methods run post-transcription:

1. **Known phantom phrases:** matches against ~25 common whisper hallucination strings
2. **Non-Latin script detection:** flags unexpected Japanese/Chinese/Arabic/Korean/Cyrillic output
3. **Segment confidence:** rejects transcripts where all segments have `no_speech_prob > 0.7`
4. **Repetition dedup:** catches 3+ identical repeated sentences
5. **Tail segment trimming:** removes trailing segments with known phantom phrases and avg word probability below 0.45

### Delimiter Wrapping

The Process Whisper Response node wraps the raw transcript in delimiters before sending to the LLM. This prevents the LLM from confusing transcript content with its own instructions, which was causing the model to occasionally "respond" to the transcript instead of cleaning it.

### LLM Polish

Qwen3-4B (Instruct, Q4_K_M) runs as a persistent always-on model via llama-swap. It corrects grammar, fixes punctuation, and formats the transcript without changing meaning. Temperature: 0.1, max tokens: 2048.

### Audio Archival

After the response is sent, the async branch saves the converted WAV to local disk. Files are organized by date: `YYYY-MM-DD/{timestamp}_{language}_{duration}s.wav`. The file path is stored in the training pair database row alongside the raw and polished transcripts.

This produces complete training triples: `(audio file, raw Whisper output, LLM-polished transcript)`. These triples enable future STT model fine-tuning where you need both the audio input and its ground truth text.

Disk write is non-blocking (runs after the HTTP response) and adds <1ms. If the write fails, the training pair is still saved without audio -- the pipeline never blocks on archive errors.

---

## Prerequisites

### Services

| Service | Required? | Notes |
|---------|-----------|-------|
| [whisper.cpp server](https://github.com/ggerganov/whisper.cpp) | Yes | Dedicated instance for voice (port 8083), separate from Signal pipeline |
| [llama-swap](https://github.com/mostlygeek/llama-swap) or compatible OpenAI API | Yes | Runs Qwen3-4B as persistent always-on model |
| PostgreSQL 15+ | Optional | Stores training pairs and latency metrics for evaluation |

### n8n Requirements

- n8n **1.75+**
- Custom n8n image with `ffmpeg` installed (required for audio format conversion)
- Environment variable: `NODE_FUNCTION_ALLOW_BUILTIN=fs,child_process,path`

### Database

The workflow logs to a PostgreSQL database (`stmna_voice`) with two tables:

- `voice_training_pairs`: stores `(audio file path, raw transcript, polished transcript)` triples for each transcription. The audio path links to the archived WAV on disk. Together these enable STT model fine-tuning (requires audio + ground truth text).
- `voice_latency_metrics`: per-request timing breakdown (whisper, qwen, audio archive, total)

These tables are optional. The core transcription path works without PostgreSQL.

---

## Import Instructions

1. In n8n, go to **Settings > Import workflow**
2. Import `stmna-voice.json`
3. Reassign credentials after import (see below)
4. Set your webhook URL and configure the bearer token
5. **Activate** the workflow

---

## Required Credentials

### `Postgres` credential

Point to your `stmna_voice` database. Used by the async logging branch only.

| Nodes using this credential |
|-----------------------------|
| Save Training Pair |
| Save Latency Metrics |

---

## Sending Audio

The workflow expects a `multipart/form-data` POST with a `file` field:

```bash
curl -X POST https://your-voice-endpoint/v1/audio/transcriptions \
  -H "Authorization: Bearer YOUR_BEARER_TOKEN" \
  -F "file=@recording.m4a"
```

Supported formats: anything ffmpeg can decode (m4a, mp3, wav, ogg, webm, flac).

The response contains the cleaned transcript:

```json
{
  "text": "Your transcribed and cleaned text here."
}
```

### Client Implementations

- **Linux:** Push-to-talk shell script using `arecord`, `curl`, and `xdotool` for paste. See [desktop/voice.sh](../../desktop/voice.sh)
- **Android:** [STMNA_Voice Mobile](https://github.com/stmna-io/stmna-voice-mobile) keyboard app with direct API integration

---

## Environment Variables

Copy `.env.example` to `.env` and fill in all values:

```env
WHISPER_URL=http://your-whisper:8083
LLAMA_SWAP_URL=http://your-llama-swap:8081
VOICE_MODEL=qwen3-4b-voice
VOICE_BEARER_TOKEN=your-bearer-token
```

See `.env.example` for the full list.

---

## Adapting to Your Setup

- **Different STT backend:** Replace the whisper.cpp HTTP call with any API that accepts audio and returns a transcript string
- **Different LLM:** Replace `LLAMA_SWAP_URL` with any OpenAI-compatible endpoint and set your model name. A small model (3-4B parameters) is sufficient for transcript cleanup.
- **No PostgreSQL:** Remove the logging nodes. The core transcription path works without them.
- **Dual whisper architecture:** The Voice pipeline uses a dedicated whisper instance (port 8083) separate from the Signal pipeline's instance (port 8084). This prevents concurrent requests from blocking each other. If you only run one pipeline, a single instance works fine.
