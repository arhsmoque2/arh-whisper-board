# Agent Onboarding & Runtime Orientation — ARH Whisper Board (`AGENTS.md`)

Welcome, Agent. This document provides the operational invariants, fast preflight gates, architectural anchors, and coding standards required to contribute safely to **ARH Whisper Board**.

---

## 🧭 System Overview & Mission

**ARH Whisper Board** is an ergonomic, collision-free Android Input Method Editor (IME) tailored for developers and power users:
* **Ergonomic Touch Engine**: Asymmetric proximate boundary protection and active-learning calibration resolving thumb retraction undershoot on modern tall-aspect displays (e.g. Xiaomi Poco F7).
* **Zero-GC Touch Loop**: Allocation-free hot path hit-testing guaranteeing locked 120 FPS typing without micro-stutter.
* **Native Voice AI**: Real-time streaming transcription (Whisper AI, Deepgram) and LLM rewording (OpenRouter, Gemini) without accessibility service dependencies.
* **Native Markdown Clipboard**: HTML-to-Markdown ingestion, code block preservation, and auto-H1 note export.

---

## 🗺️ Architectural Map & Primary Anchors

| Subsystem | Primary Code Anchors | Responsibilities |
|---|---|---|
| **Touch Engine (Hot Path)** | [`TextKeyboard.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/keyboard/TextKeyboard.kt#L41-L150) | Low-level hit-testing, asymmetric edge wells (`S->A`, `W->Q`, `X->Z`, `O->P`, `,->M`), and Backspace/Enter misfire protection. **Zero heap allocations permitted.** |
| **Calibration Data Model** | [`TouchCalibrationProfile.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/keyboard/TouchCalibrationProfile.kt#L1-L150) | Immutable profile models, Poco F7 hardware presets, and `CalibrationCheckpointStack` for undoable/revertible history. |
| **Statistical Solver** | [`TouchCalibrationSolver.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/keyboard/TouchCalibrationSolver.kt#L1-L120) | Calculates empirical mean drift vectors $(\Delta x, \Delta y)$, touch variance $\sigma^2$, and automated bezel margin proposals. |
| **Calibration UI Wizard** | [`TouchCalibrationScreen.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/app/settings/typing/TouchCalibrationScreen.kt#L1-L250) | 4-step interactive Jetpack Compose wizard (Setup, Guided Drill, Diagnostic Heatmap, A/B Sandbox Verification). |
| **Spatial Beam Decoder** | [`KeyProximityInfo.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/ime/nlp/latin/KeyProximityInfo.kt#L105-L150) <br/> [`TouchBeamDecoder.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/ime/nlp/latin/TouchBeamDecoder.kt#L1-L100) | Injects virtual centroid shifts into spatial beam decoding; enforces **Subtype Isolation**. |
| **Bayesian Touch Scoring** | [`TouchScoring.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/ime/nlp/latin/TouchScoring.kt#L1-L60) | Gaussian probability scoring over candidate keys with profile-configured touch variance $\sigma^2$. |
| **Window Layout Padding** | [`ImeWindowController.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/ime/window/ImeWindowController.kt#L269-L288) <br/> [`ImeWindowProps.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/ime/window/ImeWindowProps.kt#L30-L60) | Applies physical left, right, and bottom bezel margins to the IME window. |
| **Preflight Quality Gates** | [`whisper_doctor.py`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/tools/whisper_doctor.py#L1-L124) <br/> [`ci_asbuilt_doctor.py`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/scripts/ci_asbuilt_doctor.py#L1-L88) | Sub-second preflight AST verification (<150 ms) and bidirectional manifest conformance checking. |
| **Specifications & ADRs** | [`asbuilt.md`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/asbuilt.md) <br/> [`design.md`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/design.md) <br/> [`decisions.md`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/decisions.md) | As-built specification, technical design document, and Architecture Decision Records (ADR-0001 to ADR-0004). |

---

## 🛡️ The 4 Non-Negotiable Invariants

As an agent operating in this repository, you **MUST NOT** violate any of the following invariants:

### 1. Zero-GC Hot Path Invariant
* **Rule**: In [`TextKeyboard.getKeyForPos()`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/keyboard/TextKeyboard.kt#L41-L150) and `TextKeyboardLayout.onTouchDownInternal()`, **zero heap object allocations** are permitted.
* **Prohibited**: `ArrayList`, `mutableListOf`, `listOf`, `mapOf`, `PointF`, `RectF`, `FloatArray(...)`, or lambda iterator captures in the hot loop.
* **Allowed**: Stack-allocated scalar JVM primitives (`val dx = pos.x - bounds.left`, `val ratio = dx / bounds.width`).
* **Verification**: Checked on every commit by AST regex analysis in [`whisper_doctor.py`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/tools/whisper_doctor.py#L29-L66).

### 2. Subtype Isolation Invariant
* **Rule**: Calibrated offsets for one layout (e.g. English QWERTY) must **never** contaminate alternate layouts (Arabic, Cyrillic, Greek, Hebrew, or Numeric PIN pad).
* **Mechanism**: Offsets in `TouchCalibrationProfile.keyOffsets` are strictly indexed by **character keycode** (`KeyCode` / `Int`). Non-matching keycodes receive an exact `(0f, 0f)` displacement.
* **Verification**: Verified by [`KeyProximityInfoTest.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/test/kotlin/dev/patrickgold/florisboard/ime/nlp/latin/KeyProximityInfoTest.kt#L1-L80).

### 3. Reversible Calibration History Invariant
* **Rule**: Never mutate user calibration destructively without providing an undo path.
* **Mechanism**: Every newly calculated profile is pushed to `CalibrationCheckpointStack`. The user can A/B test between new and previous configurations and rollback with one tap.

### 4. Bidirectional As-Built Conformance Invariant
* **Rule**: The machine-readable manifest [`capabilities.json`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/capabilities.json#L1-L92) and human-readable [`asbuilt.md`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/asbuilt.md#L1-L72) must remain in **100% mathematical parity**.
* **Verification**: Enforced by [`ci_asbuilt_doctor.py`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/scripts/ci_asbuilt_doctor.py#L1-L88). If you add, remove, or modify a capability, update both files simultaneously.

---

## ⚡ Agent Preflight Protocol (Fast Feedback Loop)

Do **NOT** guess whether your changes pass. Execute the fast preflight doctor before completing any task:

```bash
# 1. Fast AST & Conformance Check (<150 ms)
python tools/whisper_doctor.py

# 2. Bidirectional Manifest Conformance Check
python scripts/ci_asbuilt_doctor.py

# 3. Touch & Calibration Unit Test Suite (~20 seconds)
./gradlew :app:testDebugUnitTest --tests "dev.patrickgold.florisboard.ime.text.keyboard.*" --tests "dev.patrickgold.florisboard.ime.nlp.latin.KeyProximityInfoTest"
```

---

## 💡 Engineering Patterns & Gotchas

### 1. JetPref Coroutine Scope
* In JetPref (`prefs.<group>.<property>.set(...)`), writing preference values is a **suspending function**.
* Inside Jetpack Compose callbacks, wrap writes inside `rememberCoroutineScope().launch { ... }`:
  ```kotlin
  val scope = rememberCoroutineScope()
  Button(onClick = {
      scope.launch {
          prefs.touchCalibration.profile.set(newProfile)
      }
  }) { ... }
  ```

### 2. Kotlin Coroutines `combine` Limit
* The Kotlin standard library `combine` function supports a maximum of **5 positional flow parameters**.
* When composing preferences in `ImeWindowController.kt`, group related sub-flows into composite data classes (e.g. `touchCalibrationFlow`) to prevent type-erasure and argument overload compiler errors.

### 3. Hardware Presets (Adding New Devices)
* Hardware presets reside in [`TouchCalibrationProfile.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/keyboard/TouchCalibrationProfile.kt#L140-L170).
* To add a preset for a new device (e.g., Pixel 9 Pro, Galaxy S25):
  1. Define a `val <DeviceName>Preset = TouchCalibrationProfile(...)`.
  2. Register it in `TouchCalibrationScreen.kt` device dropdown.
  3. Add a unit test in `TouchCalibrationProfileTest.kt`.
  4. Run `python tools/whisper_doctor.py`.

---

## 📝 Agent Handover & Task Closure Protocol

Before concluding your session:
1. Run `python tools/whisper_doctor.py` to confirm zero AST violations and 100% manifest parity.
2. If new capabilities were introduced, update [`capabilities.json`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/capabilities.json#L1-L92) and [`asbuilt.md`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/asbuilt.md#L1-L72).
3. If architectural decisions were made, document them in [`decisions.md`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/decisions.md) following the ADR format.
4. If technical designs or state machines were modified, update [`design.md`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/design.md).
5. Ensure all file references use GitHub markdown links in the format `[basename](file:///absolute/path/to/file#L1-L10)`.
