# ARH Whisper Board

<p align="left">
  <img alt="Platform" src="https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?style=flat-square&logo=android">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.3-7F52FF?style=flat-square&logo=kotlin">
  <img alt="Compose" src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=jetpackcompose">
  <img alt="License" src="https://img.shields.io/badge/License-Apache%202.0-blue?style=flat-square">
</p>

**ARH Whisper Board** is a collision-free, ergonomic Android keyboard optimized for terminals (Termux), fast typing, Whisper AI voice transcription, and a native Markdown clipboard. 

Forked from [DictateKeyboard](https://github.com/DevEmperor/DictateKeyboard) (built on [FlorisBoard](https://github.com/florisboard/florisboard)), it strips away consumer bloat while engineering physical touch protection for developers.

---

## 🎯 Core Capabilities

### 1. Proximate Touch Protection (Collision-Free Thumb Typing)
* **The Problem**: On standard mobile keyboards, the `M` key sits directly adjacent to destructive keys (`Backspace` and `Enter`). When thumb-typing fast in terminals (e.g. `rm -rf`, `commit -m`, `make`), the thumb pad frequently grazes Backspace or Enter, causing accidental line deletions or premature command execution.
* **The Solution**: An asymmetric touch boundary in `TextKeyboard.kt`. A touch landing within the left 25% boundary of Backspace or Enter immediately resolves to the neighboring character key (`M`), completely preventing destructive misfires while retaining full intentional Backspace/Enter functionality on deliberate center taps.

### 2. Native Speech-to-Text & LLM Rewording
* **Whisper AI Dictation**: Real-time streaming transcription powered by OpenAI Whisper, Deepgram, or on-device local models.
* **OpenRouter / Gemini AI Prompt Rewording**: Transform dictated voice prompts into formal prose, code templates, or bulleted summaries.
* **Zero Accessibility Dependency**: Because it is the system IME, it enjoys permanent, native background input access without getting terminated by aggressive OEM battery managers (like Xiaomi MIUI/HyperOS).

### 3. Native Markdown Clipboard with Auto-H1 Export
* **HTML-to-Markdown Ingestion**: Formatted rich text copied from web pages, Notion, or Slack is automatically parsed into clean GitHub Flavored Markdown (tables, code fences, bold, links) via `flexmark-html2md`.
* **Auto-H1 Single Clip File Exporter**: In the clipboard drawer, clips can be exported to `.md` files in `Downloads/ARH-Notes/` with filenames automatically derived from the first `# Heading` or `<h1>` tag.
* **Pinning**: Keep frequently used developer prompts and terminal commands pinned at the top of the tray forever.

---

## 🏗️ Architecture & Modules

```
arh-whisper-board/
├── app/                  # Main Android IME application & Compose UI
├── lib/
│   ├── dictate-core/     # Whisper STT & OpenRouter LLM rewording clients
│   ├── android/          # Android OS & system clipboard extensions
│   ├── compose/          # Compose layout & theme helpers
│   ├── color/            # Color model utilities
│   ├── kotlin/           # Kotlin language extensions
│   └── snygg/            # Snygg CSS/Material3 keyboard stylesheet parser
└── gradle/               # Version catalog (libs.versions.toml)
```

---

## 🚀 Building from Source

### Prerequisites
* JDK 21
* Android SDK (API 35 / Build Tools 35.0.0+)

### Commands
```bash
# Clone the repository
git clone https://github.com/arhsmoque2/arh-whisper-board.git
cd arh-whisper-board

# Run unit tests
./gradlew testDebugUnitTest

# Assemble debug APK
./gradlew assembleDebug
```

---

## 📜 License

This project is open-source under the [Apache License 2.0](LICENSE).
Portions copyright (C) FlorisBoard Contributors, DevEmperor (Dictate), and ARH.
