# Contributing to STMNA_Voice

Thank you for your interest in contributing. Here are the areas where help is most useful right now.

## What We Need

**Benchmark data on non-Strix-Halo AMD hardware.** If you run whisper.cpp + llama-swap on a different AMD APU or GPU, open an issue with your inference speeds and configuration. This helps the community understand what hardware works and at what performance level.

**Whisper prompt tuning for different languages and accents.** The current bilingual prompt is tuned for English and French. If you speak another language or have a distinctive accent, share what prompt modifications improve accuracy for you.

**voice.sh improvements.** The Linux desktop client currently requires X11 for auto-paste (`xdotool`). Wayland support, alternative paste mechanisms, and compatibility with other desktop environments (KDE, Sway, Hyprland) are all welcome.

**Documentation improvements.** Clearer setup instructions, additional troubleshooting entries, and corrections are always appreciated.

**Bug reports with hardware details.** Reproducible bug reports with your OS, kernel version, whisper.cpp build, and hardware specs help us diagnose issues faster.

## Android / Kotlin Contributions

The Android keyboard app lives in a separate repository: [stmna-voice-mobile](https://github.com/stmna-io/stmna-voice-mobile). Kotlin/Compose contributions, UI improvements, and Android-specific bug reports should go there.

## How to Submit

1. Fork the repository
2. Create a branch for your change
3. Submit a pull request with a clear description of what changed and why

For bug reports and benchmark data, open an issue using the appropriate template.

## Code Style

- Shell scripts: bash, shellcheck-clean
- n8n workflow JSON: follow the sanitization rules in the workflow README
- Documentation: plain English, no jargon without explanation, copy-pasteable commands

## License

By contributing, you agree that your contributions will be licensed under Apache 2.0 (same as the project).
