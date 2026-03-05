<div align="center">

  <h1>STMNA Voice</h1>
  <h3>Sovereign Speech-to-Text Pipeline</h3>
  <p><em>Push-to-talk dictation that types polished text at your cursor, learns your voice, and never phones home.</em></p>

  [![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
  [![Built on AMD](https://img.shields.io/badge/Built%20on-AMD%20Strix%20Halo-ED1C24)](https://www.amd.com)
  [![Powered by n8n](https://img.shields.io/badge/Powered%20by-n8n-FF6D5A)](https://n8n.io)
  [![whisper.cpp](https://img.shields.io/badge/STT-whisper.cpp%20Vulkan-000000)](https://github.com/ggerganov/whisper.cpp)
  ![Status](https://img.shields.io/badge/Status-Phase%201%20Live-brightgreen)

  <br/>

  [📖 Docs](#documentation) · [🚀 Quick Start](#quick-start) · [🏗️ Architecture](#architecture) · [📱 Mobile App](#mobile-app)

</div>

---

<!-- TODO: Hero GIF — mobile push-to-talk → text appears at cursor on desktop (scrcpy recording) -->

---

## What is STMNA Voice?

STMNA Voice is a **self-hosted speech-to-text pipeline** that runs entirely on your own hardware. Speak → your words appear at your cursor, polished and corrected. On your phone or your laptop. With no audio ever leaving your network.

Unlike commercial dictation tools:

- **No cloud** — audio never touches a third-party server
- **Self-improving** — every transcription builds a personal training dataset that improves accuracy over time
- **Cross-platform** — same backend serves Linux desktop (voice.sh) and Android (STMNA Voice Mobile)
- **Open format** — training data is yours forever, not locked in any vendor's proprietary format

### Hardware Requirement

STMNA Voice runs on [STMNA Desk](https://github.com/stmna-io/stmna-desk) — AMD Strix Halo + 128GB unified memory. The backend (whisper.cpp + Qwen LLM) runs on the Desk. Clients are thin: your phone or laptop just records audio and sends it.

---

## Repo Structure (Monorepo)

```
stmna-voice/
├── backend/          ← Server-side pipeline (n8n workflow + whisper.cpp config)
│   ├── workflows/    ← Sanitized n8n workflow JSON (import into your n8n instance)
│   └── config/       ← whisper.cpp server config, llama-swap group config
├── desktop/          ← Linux push-to-talk client (voice.sh)
│   └── voice.sh      ← Push-to-talk script — record, transcribe, paste
├── mobile/           ← Android app (STMNA Voice Mobile — fork of Echos)
│   └── ...           ← React Native / Expo source
├── docs/
│   ├── install-guide.md   ← Backend deployment guide
│   ├── linux-guide.md     ← Linux desktop client setup
│   ├── backend-setup.md   ← Deploy whisper.cpp + n8n workflow
│   └── mobile-build.md    ← Build and install Android APK
├── LICENSE
└── README.md
```

---

## Performance

Measured on AMD Ryzen AI Max+ 395 · whisper large-v3-turbo Q5 · Qwen3-4B Voice Q4_K_M

| Metric | Value | Notes |
|--------|-------|-------|
| Warm pipeline latency | ~1700–3400ms | Model always loaded (persistent group) |
| Typical latency | ~2000–4000ms | Scales with audio length |
| Whisper inference | ~700–1300ms | Audio length dependent |
| Qwen polish | ~1000–2800ms | Transcript length dependent |
| whisper.cpp VRAM | ~3–4GB | Shares GPU with other inference workloads |

**Why not faster?** Qwen runs on every request in Phase 1 to maximize training data collection. Confidence-based skipping comes after fine-tuning. See [docs/backend-setup.md](docs/backend-setup.md) for the roadmap.

---

## Architecture

```
┌─────────────────┐     ┌──────────────────────────────────────────────────┐
│   voice.sh      │     │                   STMNA Desk                     │
│   (Linux)       │     │                                                  │
│                 │──┐  │  ┌─────────────────────────────────────────┐    │
│   STMNA Voice   │  │  │  │  n8n Webhook (/v1/audio/transcriptions) │    │
│   Mobile        │──┼─▶│  │                    │                    │    │
│   (Android)     │  │  │  │                    ▼                    │    │
│                 │  │  │  │  ┌──────────────────────────────────┐   │    │
│   Any OpenAI-   │──┘  │  │  │  FFmpeg (format conversion)      │   │    │
│   compatible    │     │  │  │  Hallucination Filter            │   │    │
│   STT client    │     │  │  │  whisper.cpp (Vulkan, large-v3-turbo Q5) │    │
└─────────────────┘     │  │  │  Qwen3-4B Polish (quality + accent)│   │    │
        ↑               │  │  └──────────────────────────────────┘   │    │
        │ polished text │  │                    │                    │    │
        └───────────────┘  │                    ▼                    │    │
                           │  ┌──────────────────────────────────┐   │    │
                           │  │  PostgreSQL (async)              │   │    │
  VPS (Caddy HTTPS)        │  │  Training pairs + latency metrics│   │    │
  stv.yourdomain.com ──────┘  └──────────────────────────────────┘   │    │
                           └──────────────────────────────────────────┘
```

---

## Quick Start

### Backend Setup

See [docs/backend-setup.md](docs/backend-setup.md) for full instructions.

```bash
# 1. Clone
git clone https://github.com/stmna-io/stmna-voice.git
cd stmna-voice

# 2. Configure
cp .env.example .env
# Edit .env with your Desk IP, whisper model path, etc.

# 3. Import workflow
# Open your n8n instance → Import workflow from backend/workflows/stmna-voice.json

# 4. Deploy whisper.cpp container (see backend/config/)

# 5. Test
curl -X POST https://stv.yourdomain.com/v1/audio/transcriptions \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@test-audio.wav"
```

### Linux Client (voice.sh)

```bash
# Download the voice.sh script
cp backend/config/voice.sh ~/bin/voice.sh
chmod +x ~/bin/voice.sh

# Set your endpoint and token in voice.sh
# Bind to a system keyboard shortcut (e.g. Super+V)
```

### Android

See [mobile/](mobile/) and [docs/mobile-build.md](docs/mobile-build.md).

---

## Documentation

| Document | Description |
|----------|-------------|
| [Install Guide](docs/install-guide.md) | Full backend deployment guide (requires STMNA Desk) |
| [Linux Guide](docs/linux-guide.md) | Linux desktop client — push-to-talk setup, keyboard shortcut |
| [Backend Setup](docs/backend-setup.md) | whisper.cpp deployment, n8n workflow import, Caddy auth |
| [Mobile Build](docs/mobile-build.md) | Build STMNA Voice Mobile APK from source, install, configure |

---

## Mobile App

STMNA Voice Mobile is an Android app forked from [Echos by A1 Lab](https://github.com/jan3dev/a1echos) (MIT license).

**What it does differently:**
- Default mode: thin client — sends audio to your STMNA Desk backend
- Bundled Whisper small model as offline fallback
- STMNA branding and settings UI

**Status:** Phase 3 — in development. See [mobile/](mobile/) for source.

---

## The Self-Improving Loop

Every transcription generates a training pair: `(raw Whisper output, Qwen-polished output)`. These pairs accumulate in PostgreSQL. After collecting ~50 hours of approved data, we fine-tune Whisper large-v3-turbo on YOUR voice. The result:

- Whisper learns your accent, vocabulary, and speech patterns
- Qwen correction rate drops as Whisper improves
- Eventually, Qwen can be skipped entirely for most requests
- Your training data is open-format — fine-tune any Whisper variant, forever

This dataset is your most valuable long-term asset from running STMNA Voice.

---

## Contributing

See the [stmna-desk](https://github.com/stmna-io/stmna-desk) repo for hardware prerequisites.

Areas where contributions are welcome:
- 📱 React Native / Expo improvements to the mobile app
- 🎤 Whisper prompt tuning for different languages and accents
- 📊 Benchmark data on non-Strix-Halo AMD hardware
- 📝 Documentation improvements

---

## License

Apache 2.0 — see [LICENSE](LICENSE)

The mobile app ([mobile/](mobile/)) is based on [Echos](https://github.com/jan3dev/a1echos) (MIT license). See [mobile/LICENSE](mobile/LICENSE) for details.

---

<div align="center">
  <sub>Built by <a href="https://stmna.io">STMNA_</a> · Engineered resilience. Sovereign by design.</sub>
</div>
