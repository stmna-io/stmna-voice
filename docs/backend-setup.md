# Backend Setup Guide

<!-- TODO: Write full backend setup guide for SB-09 session -->
<!-- Content: whisper.cpp container deployment (rootless Podman), llama-swap persistent group
     config for Qwen3-4B Voice, n8n workflow import steps, PostgreSQL schema setup,
     Caddy bearer token auth config, voice.sh script deployment + keybinding,
     testing with curl, latency tuning tips -->

## Placeholder

This document will cover:

### Prerequisites

- STMNA Desk running (llama-swap, n8n, PostgreSQL)
- Or equivalent: AMD GPU + Vulkan + llama-swap + n8n

### whisper.cpp Container

- Rootless Podman compose for dedicated whisper-voice container
- Model: whisper large-v3-turbo Q5 (~3-4GB VRAM)
- Port: 8083 (Voice-dedicated, separate from Signal's whisper container)
- Network: stmna-net (so n8n can reach it)

### n8n Workflow

- Import `backend/workflows/stmna-voice.json` into your n8n instance
- Configure credentials (PostgreSQL connection)
- Set environment variables for whisper URL, llama-swap URL

### PostgreSQL Schema

- `voice_training_pairs` — raw/polished transcription pairs
- `voice_latency_metrics` — per-request timing data
- Schema SQL in `backend/config/schema.sql`

### Caddy Configuration

- Bearer token auth for public endpoint (stv.yourdomain.com)
- Reverse proxy to n8n webhook (port 5678)

### Linux Client (voice.sh)

- Script location and setup
- System keybinding configuration (GNOME, KDE, i3)
- Clipboard modes (CLIPBOARD vs PRIMARY)
- Debugging: check log output, test with curl

### Testing

```bash
# Test the endpoint directly
curl -X POST https://stv.yourdomain.com/v1/audio/transcriptions \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@test.wav" \
  -F "response_format=json"
```

Coming in SB-09 session.
