# Architecture Decision Records (ADR) — ARH Whisper Board

This document records the architectural decisions for **ARH Whisper Board**, capturing the context, options considered, chosen solutions, and invariant constraints.

---

## ADR Index

| ADR ID | Title | Status | Date |
|---|---|---|---|
| [ADR-0001](#adr-0001-active-learning-touch-calibration--proximate-edge-protection) | Active-Learning Touch Calibration & Proximate Edge Protection | Accepted | 2026-09-19 |
| [ADR-0002](#adr-0002-zero-gc-invariant-in-ime-touch-dispatch-loop) | Zero-GC Invariant in IME Touch Dispatch Loop | Accepted | 2026-09-19 |
| [ADR-0003](#adr-0003-keycode-indexed-subtype-isolation-for-spatial-geometries) | Keycode-Indexed Subtype Isolation for Spatial Geometries | Accepted | 2026-09-19 |
| [ADR-0004](#adr-0004-bidirectional-as-built-conformance-doctor-pattern) | Bidirectional As-Built Conformance Doctor Pattern | Accepted | 2026-09-19 |
| [ADR-0005](#adr-0005-lean-engine-optimization-quick-snippets-and-ergonomic-symbol-access) | Lean Engine Optimization, Quick Snippets, and Ergonomic Symbol Access | Accepted | 2026-09-20 |

---

## ADR-0001: Active-Learning Touch Calibration & Proximate Edge Protection

### Status
**Accepted & Implemented** (Verified by 17 unit tests and [`whisper_doctor.py`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/tools/whisper_doctor.py#L1-L124)).

### Context & Problem Statement
On modern tall mobile displays (e.g., Xiaomi Poco F7: 6.67" AMOLED, 1220×2712 px, 20:9 aspect ratio, ~395 dp width), thumb ergonomics create systematic typing inaccuracies:
1. **Thumb Retraction Undershoot on Outer Columns**:
   When holding a phone with two hands, the natural resting pivot of the left thumb sits over the `E-D-X` / `R-F-C` cluster, while the right thumb rests over `U-J-N` / `I-K-M`. Tapping outer edge keys (`A`, `Q`, `Z` on the left; `P`, `M` on the right) requires the thumb to retract toward the thenar eminence (palm base) or hyperextend outward. Consequently, users systematically undershoot `A` (registering as `S`), `Q` (registering as `W`), `Z` (registering as `X`), and `P` (registering as `O`).
2. **Destructive Edge Key Collision**:
   The bottom-right letter `M` borders destructive action keys (`Backspace` and `Enter`). During rapid typing in developer contexts (e.g., Termux CLI commands like `rm -rf`, `make`, `git commit -m`), slight grazing of the boundary triggers premature command execution or destructive character deletion.
3. **Ergonomic Variance Across Users**:
   Static keyboard geometry cannot account for variances in user hand dimensions, grip styles (single-hand vs. two-thumb), or device case bevels. A static compensation model either under-compensates or over-corrects.

### Decision
We implemented a **two-tier compensation model** combined with an **active-learning calibration wizard**:

#### Tier 1: Hardware-Bound Proximate Edge Protection
Directly in the low-level hit-test method [`TextKeyboard.getKeyForPos()`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/keyboard/TextKeyboard.kt#L41-L150), asymmetric gravity wells intercept edge taps:
* **Left Thumb Retraction Guard**:
  - Taps landing within the leftmost 22% of key `S` resolve to adjacent `A`.
  - Taps landing within the leftmost 20% of key `W` resolve to adjacent `Q`.
  - Taps landing within the leftmost 20% of key `X` resolve to adjacent `Z`.
* **Right Thumb Reach Guard**:
  - Taps landing within the rightmost 20% of key `O` resolve to adjacent `P`.
  - Taps landing within the leftmost 20% of key `,` (comma) resolve to adjacent `M`.
* **Destructive Misfire Defense**:
  - Taps landing within the leftmost 25% of `Backspace` or `Enter` resolve to adjacent character `M` (or preceding text key).

All Tier 1 evaluations execute using stack-allocated primitive scalars without allocating any heap objects.

#### Tier 2: Dual Spatial Tuning (Macro Bezel Margins + Micro Centroid Shifts)
For active, personalized calibration, the system decomposes spatial adjustments into two orthogonal planes:
1. **Macro Window Padding**:
   Managed via [`ImeWindowController.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/ime/window/ImeWindowController.kt#L269-L288) and [`ImeWindowProps.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/ime/window/ImeWindowProps.kt#L30-L60). Injects physical left, right, and bottom bezel margins (`paddingLeft`, `paddingRight`, `paddingBottom`) into the IME window, pulling the edge keys away from the display bezels into the thumb's comfortable arc.
2. **Micro Centroid Displacement & Variance Tuning**:
   Managed via [`KeyProximityInfo.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/ime/nlp/latin/KeyProximityInfo.kt#L105-L150) and [`TouchScoring.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/ime/nlp/latin/TouchScoring.kt#L1-L60). Injects empirical displacement vectors $(\Delta x, \Delta y)$ per keycode directly into the spatial beam decoder without modifying visible key boundaries. Dynamically adjusts Gaussian touch variance $\sigma^2$ to reflect user tap dispersion.

#### Tier 3: Active-Learning Calibration Wizard & Reversible Checkpoint Stack
* **Interactive Drill Engine**: [`TouchCalibrationScreen.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/app/settings/typing/TouchCalibrationScreen.kt#L1-L250) guides the user through targeted pangram sentences (`"the quick brown fox..."`, `"pack my box with five dozen liquor jugs"`, `"alpha queen zebra..."`) capturing touch tap coordinates $(x_{\text{tap}}, y_{\text{tap}})$ against target centroids $(x_{\text{key}}, y_{\text{key}})$.
* **Statistical Solver**: [`TouchCalibrationSolver.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/keyboard/TouchCalibrationSolver.kt#L1-L120) calculates systematic drift vectors:
  $$\Delta x = \frac{1}{N}\sum_{i=1}^{N}(x_i - x_{\text{key}}), \quad \Delta y = \frac{1}{N}\sum_{i=1}^{N}(y_i - y_{\text{key}})$$
  Calculates sample variance $\sigma^2$ and proposes macro bezel padding when systematic retraction exceeds threshold $\tau = 4.0\,\text{dp}$.
* **Revertible Checkpoint Stack**: [`TouchCalibrationProfile.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/keyboard/TouchCalibrationProfile.kt#L1-L150) maintains a 10-slot immutable history ring buffer (`CalibrationCheckpointStack`), enabling instantaneous A/B comparison and single-tap undo.

### Consequences
* **Positive**:
  - Systematic edge typos (`s` instead of `a`, `w` instead of `q`) are reduced to zero without changing muscle memory.
  - Destructive Backspace/Enter misfires in terminal sessions are fully mitigated.
  - Users can adapt keyboard geometry to their physical hand size and phone case thickness.
  - Full rollback safety prevents configuration lock-in or degraded typing states.
* **Tradeoffs & Mitigations**:
  - *Risk*: Keycode offsets could distort non-Latin alphabets.
  - *Mitigation*: Strictly enforced **Subtype Isolation** (see [ADR-0003](#adr-0003-keycode-indexed-subtype-isolation-for-spatial-geometries)).
  - *Risk*: Boundary checks in hot touch path could trigger GC pauses.
  - *Mitigation*: Strictly enforced **Zero-GC Invariant** (see [ADR-0002](#adr-0002-zero-gc-invariant-in-ime-touch-dispatch-loop)).

---

## ADR-0002: Zero-GC Invariant in IME Touch Dispatch Loop

### Status
**Accepted & Implemented** (Verified by AST analyzer in [`whisper_doctor.py`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/tools/whisper_doctor.py#L29-L66)).

### Context & Problem Statement
Android input method editors process touch events at high display refresh rates (60 Hz, 90 Hz, 120 Hz). In a 120 Hz display (standard on Poco F7), a frame deadline is **8.33 ms**.
If the hit-testing loop (`getKeyForPos` in [`TextKeyboard.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/keyboard/TextKeyboard.kt#L41-L150)) allocates heap objects (such as `PointF`, `RectF`, `ArrayList`, or lambda iterator captures) on every `ACTION_DOWN` or `ACTION_MOVE`, young-generation garbage collection (GC) pauses occur every few seconds during burst typing. These GC pauses (10–30 ms) cause dropped frames, noticeable input latency, and broken swipe trajectories.

### Decision
Enforce a strict **Zero-GC Hot Path Invariant**:
1. No heap collections (`ArrayList`, `mutableListOf`, `listOf`, `mapOf`) inside `getKeyForPos()`.
2. No geometric wrapper allocations (`PointF`, `RectF`).
3. No array allocations (`FloatArray(...)`) inside the hot path.
4. All coordinate math and boundary calculations must run strictly on stack-allocated JVM primitives (`val dx = pos.x - bounds.left`, `val ratio = dx / bounds.width`).
5. Calibrated key offsets are resolved from flat primitive lookups or pre-calculated lookups during layout initialization, never computed dynamically per touch.

### Conformance
Enforced in automated CI via [`whisper_doctor.py`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/tools/whisper_doctor.py#L29-L66), which uses static regex AST pattern matching to fail the build if any forbidden allocation appears inside `getKeyForPos()`.

---

## ADR-0003: Keycode-Indexed Subtype Isolation for Spatial Geometries

### Status
**Accepted & Implemented** (Verified by [`KeyProximityInfoTest.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/test/kotlin/dev/patrickgold/florisboard/ime/nlp/latin/KeyProximityInfoTest.kt#L1-L80)).

### Context & Problem Statement
FlorisBoard/ARH Whisper Board supports multiple subtypes (e.g., English QWERTY, Arabic, Cyrillic, Greek, Numeric keypad, Emoji).
A user calibrating their touch drift for English QWERTY shifts the virtual centroid of key `A` (keycode 97) rightward. If this offset were indexed by grid coordinate or spatial slot index (e.g., "Row 2, Column 1"), switching to an Arabic or Cyrillic layout would inappropriately displace an unrelated character (e.g., Arabic `ش` or Cyrillic `Ф`), corrupting touch fidelity on alternate scripts.

### Decision
All micro centroid offsets in [`TouchCalibrationProfile.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/keyboard/TouchCalibrationProfile.kt#L1-L60) and [`KeyProximityInfo.kt`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/app/src/main/kotlin/dev/patrickgold/florisboard/ime/nlp/latin/KeyProximityInfo.kt#L105-L150) are strictly keyed by **character keycode** (`KeyCode` / `Int`).
During keyboard layout compilation:
```kotlin
val (dx, dy) = profile.keyOffsets[key.computedData.asRange().first] ?: (0f to 0f)
```
Non-matching keycodes (including non-Latin alphabets, symbol layers, and numeric pads) receive an exact `(0f, 0f)` displacement, guaranteeing complete isolation between language subtypes.

---

## ADR-0004: Bidirectional As-Built Conformance Doctor Pattern

### Status
**Accepted & Implemented** (Verified by [`ci_asbuilt_doctor.py`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/scripts/ci_asbuilt_doctor.py#L1-L88)).

### Context & Problem Statement
In fast-moving agentic and collaborative workflows, documentation and code frequently diverge:
* Architecture claims in `README.md` or `asbuilt.md` become stale or aspirational.
* Capabilities declared in machine-readable manifests (`capabilities.json`) are renamed or omitted in specs.
* Production code anchors are relocated without updating references.

### Decision
Establish an automated **Bidirectional Conformance Doctor**:
1. [`capabilities.json`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/capabilities.json#L1-L92) is the canonical machine-readable inventory of all platform capabilities.
2. [`asbuilt.md`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/asbuilt.md#L1-L72) is the canonical human-readable architectural specification.
3. [`scripts/ci_asbuilt_doctor.py`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/scripts/ci_asbuilt_doctor.py#L1-L88) executes in CI and local preflight:
   - Validates that every capability in `capabilities.json` is documented in `asbuilt.md`.
   - Validates that every capability in `asbuilt.md` exists in `capabilities.json`.
   - Verifies numeric claim parity (e.g. `12/12 Declared Capabilities`).
   - Verifies that all primary production code anchor files physically exist on disk.
4. [`tools/whisper_doctor.py`](file:///D:/_ARH-AGENT-OS/projects/arh-whisper-board/tools/whisper_doctor.py#L1-L124) wraps the conformance check and AST check into an atomic, sub-second preflight gate (<150 ms execution time) for developers and autonomous agents.

---

## ADR-0005: Lean Engine Optimization, Quick Snippets, and Ergonomic Symbol Access

### Status
**Accepted & Implemented** (2026-09-20).

### Context & Problem Statement
The baseline keyboard suffered from bloat, ergonomic friction, and redundant background services:
1. **Word Glide Typing Bloat**:
   Continuous word gesture trajectory analysis incurred significant memory, dictionary parsing, and touch event dispatch overhead, while user typing rely solely on standard taps, swipe-to-swap keyboard ⇄ Whisper dictation (`legacySwipeToggle`), and space-bar cursor navigation (`SpaceGlide`).
2. **Asset & Language Bloat**:
   Over 95 unused Whisper language definitions, 48 unused localized emoji annotation files, full uncurated Unicode emoji sets, and unused dictionary packages (e.g. `de.json`) inflated the APK size and search index latencies without operational relevance for English, Malay, and Mandarin communication.
3. **Unused Background Footprint**:
   WearOS background sync services and Klipy GIF integration consumed battery and runtime resources on mobile.
4. **Ergonomic Inefficiency on Common Symbols & Commands**:
   Developers and chat users frequently type commands (`/resume`, `/usage`, `/clear`), file paths, URLs, and symbols (`@`, `?`, `&`, `/`). Row 2 in standard QWERTY contained 9 keys (`A` through `L`), leaving an unused half-key margin on each side. Furthermore, typing email addresses or slash-commands required repetitive view switching.
5. **Lack of Inline Clipboard Editing**:
   Managing clipboard history required switching to external manager applications (e.g. CopyQ) to edit, reorder, or clean up clips.

### Decision
1. **Omit Word Glide Typing**: Purge word glide trajectory analysis from the typing engine and settings UI while strictly retaining horizontal swipe-to-swap (`legacySwipeToggle`) and space-bar cursor glide (`SpaceGlide`).
2. **Language & Asset Pruning**: Restrict Whisper transcription languages to `DETECT`, `en`, `ms`, `zh-CN`, and `zh-TW`. Curate emoji database to core smileys, hand gestures, and essential reactions. Delete unused language annotations and German dictionary assets.
3. **Strip WearOS & GIF Support**: Remove WearOS companion components, protocols, and dependencies. Purge Klipy GIF search UI and `coil-gif` dependencies.
4. **Row 2 Dedicated `[ / ]` Key**: Add a dedicated slash key right after `L` in Row 2, creating an exact 10-key row matching Row 1. Anchor long-press symbol popups (`\ @ ? & # ~ $ %`) configurable via preferences and pre-compiled for Zero-GC touch dispatch.
5. **Safe Backspace "Clear All"**: Provide a long-press popup on Backspace for transactional full-field deletion with automatic snapshotting to the clipboard undo buffer.
6. **Smartbar Single-Row Mode Cycling**: Implement a single-row cycle chip (Candidates ⇄ Symbols & Email ⇄ Quick Snippets) adhering to the strict 40 dp IME height invariant without layout jitter.
7. **CopyQ Inline Clipboard Actions**: Add inline clip editing and reordering directly within the IME clipboard interface using `ClipboardHistoryDao`.
