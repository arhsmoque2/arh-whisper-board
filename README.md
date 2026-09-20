# ARH Whisper Board

<p align="left">
  <img alt="Platform" src="https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?style=flat-square&logo=android">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.3-7F52FF?style=flat-square&logo=kotlin">
  <img alt="Compose" src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=jetpackcompose">
  <img alt="Doctor Status" src="https://img.shields.io/badge/Doctor-100%25%20Passed-success?style=flat-square">
  <img alt="License" src="https://img.shields.io/badge/License-Apache%202.0-blue?style=flat-square">
</p>

**ARH Whisper Board** is an ergonomic, collision-free, low-latency Android Input Method Editor (IME) engineered for terminal power users, fast thumb typists, Whisper AI voice transcription, and a native Markdown clipboard. 

Forked from [DictateKeyboard](https://github.com/DevEmperor/DictateKeyboard) (built on [FlorisBoard](https://github.com/florisboard/florisboard)), it strips away consumer bloat while engineering physical touch protection, zero-GC dispatch loops, and active-learning calibration for modern large-screen devices (such as the Xiaomi Poco F7).

---

## 🎯 Core Capabilities

### 1. Proximate Edge Protection & Thumb Retraction Correction
* **The Problem**: On modern tall aspect ratio screens (20:9, ~395 dp width), the thumbs naturally rest over interior keys (`E-D-X` for left thumb; `U-J-N` for right thumb). Reaching outer edge keys (`A`, `Q`, `Z`, `P`, `M`) requires severe thumb retraction or hyperextension, systematically causing typists to undershoot (e.g. typing `S` when aiming for `A`, `W` for `Q`, `X` for `Z`, `O` for `P`). Furthermore, grazing the boundary of key `M` triggers adjacent destructive keys (`Backspace` or `Enter`).
* **The Solution**: Low-level asymmetric gravity wells inside [`TextKeyboard.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/keyboard/TextKeyboard.kt#L41-L150). Taps landing within the leftmost 22% of `S` resolve to `A`; leftmost 20% of `W` resolve to `Q`; leftmost 20% of `X` resolve to `Z`; rightmost 20% of `O` resolve to `P`; and leftmost 25% of Backspace/Enter resolve to `M`.

### 2. Active-Learning Touch Calibration Wizard & Device Presets
* **Guided Drill Wizard**: A 4-step interactive Jetpack Compose wizard ([`TouchCalibrationScreen.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/app/settings/typing/TouchCalibrationScreen.kt#L1-L250)) accessible from **Settings → Typing → Touch Calibration**. Users complete targeted pangram typing drills while the engine captures tap coordinates.
* **Statistical Drift Solver**: [`TouchCalibrationSolver.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/keyboard/TouchCalibrationSolver.kt#L1-L120) computes empirical drift vectors $(\Delta x, \Delta y)$, estimates Bayesian touch variance $\sigma^2$, and recommends bezel margins.
* **Undoable Checkpoint Stack**: Maintains an immutable 10-slot ring buffer (`CalibrationCheckpointStack`) with A/B sandbox verification and instant single-tap rollback.
* **Poco F7 Portrait Preset**: Built-in hardware preset with 10 dp left/right bezel padding, 6 dp bottom margin, and pre-calibrated edge retraction offsets.

### 3. Zero-GC Touch Dispatch Hot Path
* **120 Hz Locked Performance**: The hit-testing loop executes in under 1 ms with **zero heap allocations** (`PointF`, `RectF`, `ArrayList`, or iterator captures are prohibited).
* **AST Quality Gate**: Automated static analysis in [`whisper_doctor.py`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/tools/whisper_doctor.py#L29-L66) inspects the AST of `getKeyForPos()` to guarantee zero GC pressure and eliminate micro-stutter during burst typing.

### 4. Native Speech-to-Text & LLM Rewording
* **Whisper AI Dictation**: Real-time streaming transcription powered by OpenAI Whisper, Deepgram, or on-device local Sherpa-ONNX models.
* **Prompt Rewording Chips**: One-tap text transformation directly from the keyboard smartbar using OpenRouter or Gemini AI (Executive, BM Formal, Bulletize, Fix Grammar).
* **Zero Accessibility Dependency**: Runs as the native system IME, guaranteeing permanent background operation without termination by OEM battery savers (such as Xiaomi MIUI/HyperOS).

### 5. Native Markdown Clipboard with Auto-H1 Export
* **Rich Text Ingestion**: Formatted HTML from web pages, Notion, or Slack is parsed into clean GitHub Flavored Markdown (tables, code blocks, bold, links) via `flexmark-html2md`.
* **Auto-H1 Single Clip File Exporter**: Clips can be exported to `.md` files in `Downloads/ARH-Notes/` with filenames automatically derived from the first `# Heading` or `<h1>` tag.
* **Transactional Undo Buffer**: 10-item transient revision ring buffer preventing accidental clipboard overwrites.

---

## 🏛️ Verified Capability Inventory

All 12 native capabilities are contractually defined in [`capabilities.json`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/capabilities.json#L1-L92) and verified against [`asbuilt.md`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/asbuilt.md#L1-L72):

| # | Capability ID | Category | Status | Primary Code Anchor |
|---|---|---|---|---|
| 1 | `proximate_edge_protection` | `touch_engine` | 🟢 Verified | [`TextKeyboard.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/keyboard/TextKeyboard.kt#L41-L150) |
| 2 | `touch_calibration_wizard` | `touch_engine` | 🟢 Verified | [`TouchCalibrationScreen.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/app/settings/typing/TouchCalibrationScreen.kt#L1-L250) |
| 3 | `touch_beam_decoder` | `nlp_decoder` | 🟢 Verified | [`TouchBeamDecoder.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/ime/nlp/latin/TouchBeamDecoder.kt#L1-L100) |
| 4 | `stt_cloud_streaming_websocket` | `voice_ai` | 🟢 Verified | [`RealtimeClient.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/lib/dictate-core/src/main/kotlin/dev/patrickgold/florisboard/dictate/provider/RealtimeClient.kt) |
| 5 | `stt_on_device_whisper` | `voice_ai` | 🟢 Verified | [`LocalTranscriptionProvider.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/dictate/provider/LocalTranscriptionProvider.kt) / Sherpa-ONNX |
| 6 | `ai_prompt_rewording` | `voice_ai` | 🟢 Verified | [`DictateRewording.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/lib/dictate-core/src/main/kotlin/dev/patrickgold/florisboard/dictate/provider/DictateRewording.kt) |
| 7 | `markdown_clipboard_history` | `clipboard` | 🟢 Verified | [`ClipboardDatabase.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/ime/clipboard/ClipboardDatabase.kt) |
| 8 | `transactional_clipboard_undo` | `clipboard` | 🟢 Verified | [`ClipboardHistoryDao.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/ime/clipboard/ClipboardHistoryDao.kt) |
| 9 | `zero_gc_touch_dispatch` | `performance` | 🟢 Verified | [`TextKeyboardLayout.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/keyboard/TextKeyboardLayout.kt) |
| 10 | `snygg_theme_engine` | `ui_engine` | 🟢 Verified | [`SnyggStylesheet.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/lib/snygg/src/main/kotlin/org/florisboard/lib/snygg/SnyggStylesheet.kt) |
| 11 | `floris_smartbar` | `ui_engine` | 🟢 Verified | [`SmartbarView.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/ime/smartbar/SmartbarView.kt) |
| 12 | `custom_quick_text_snippets` | `productivity` | 🟢 Verified | [`QuickSnippetsManager.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/ime/smartbar/quick/QuickSnippetsManager.kt) |

---

## 🏗️ Architecture & Documentation

```
arh-whisper-board/
├── app/                  # Android IME application & Compose UI
├── lib/
│   ├── dictate-core/     # Whisper STT & OpenRouter LLM rewording clients
│   ├── android/          # Android OS & system clipboard extensions
│   ├── compose/          # Compose layout & theme helpers
│   ├── color/            # Color model utilities
│   ├── kotlin/           # Kotlin language extensions
│   └── snygg/            # Snygg CSS/Material3 keyboard stylesheet parser
├── tools/
│   └── whisper_doctor.py # Sub-second preflight gate (<150 ms)
├── scripts/
│   └── ci_asbuilt_doctor.py # Bidirectional manifest conformance checker
├── asbuilt.md            # As-built architectural specification
├── design.md             # Technical design & spatial physics spec
├── decisions.md          # Architecture Decision Records (ADR-0001 to ADR-0004)
├── capabilities.json     # Machine-readable capability contract
└── AGENTS.md             # Agent onboarding & operational standards
```

For detailed architectural and engineering specs:
* [Technical Design Specification (`design.md`)](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/design.md)
* [Architecture Decision Records (`decisions.md`)](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/decisions.md)
* [Agent Onboarding & Invariants (`AGENTS.md`)](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/AGENTS.md)
* [As-Built Specification (`asbuilt.md`)](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/asbuilt.md)

---

## 🚀 Building & Testing

### Prerequisites
* **JDK 21**
* **Android SDK** (API 35 / Build Tools 35.0.0+)
* **Python 3.10+** (for preflight verification doctors)

### Preflight Quality Gate Doctor (Fast Pre-Commit Check)
Before committing any changes or after editing touch code, execute the preflight doctor:
```bash
# Instant local preflight check (<150 ms)
python tools/whisper_doctor.py

# Full preflight check including Gradle touch & calibration unit tests
python tools/whisper_doctor.py --full
```

### Unit Tests
```bash
# Run all unit tests
./gradlew testDebugUnitTest

# Run touch and calibration unit test suite
./gradlew :app:testDebugUnitTest --tests "dev.patrickgold.florisboard.ime.text.keyboard.*" --tests "dev.patrickgold.florisboard.ime.nlp.latin.KeyProximityInfoTest"
```

### Assemble APK
```bash
# Assemble debug APK
./gradlew assembleDebug
```

---

## 📜 License

This project is open-source under the [Apache License 2.0](LICENSE).  
Portions copyright (C) FlorisBoard Contributors, DevEmperor (Dictate), and ARH.
