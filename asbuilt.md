# ARH Whisper Board — As-Built Specification (`asbuilt.md`)

> **Product**: `ARH Whisper Board`  
> **Package**: `dev.patrickgold.florisboard` / `dev.emperor.dictate`  
> **Status**: Verified Operational (12/12 Declared Capabilities)  
> **Contract Manifest**: [`capabilities.json`](capabilities.json)  

---

## 🏛️ Verified Capability Inventory

This document serves as the immutable as-built record of the platform. All claims in this document are programmatically verified against [`capabilities.json`](capabilities.json) by `scripts/ci_asbuilt_doctor.py`.

| # | Capability ID | Category | Status | Primary Code Anchor |
|---|---|---|---|---|
| 1 | `proximate_edge_protection` | `touch_engine` | 🟢 Verified | [`TextKeyboard.kt`](app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/keyboard/TextKeyboard.kt) |
| 2 | `touch_calibration_wizard` | `touch_engine` | 🟢 Verified | [`TouchCalibrationScreen.kt`](app/src/main/kotlin/dev/patrickgold/florisboard/app/settings/typing/TouchCalibrationScreen.kt) |
| 3 | `touch_beam_decoder` | `nlp_decoder` | 🟢 Verified | [`TouchBeamDecoder.kt`](app/src/main/kotlin/dev/patrickgold/florisboard/ime/nlp/latin/TouchBeamDecoder.kt) |
| 4 | `stt_cloud_streaming_websocket` | `voice_ai` | 🟢 Verified | [`RealtimeClient.kt`](lib/dictate-core/src/main/kotlin/dev/patrickgold/florisboard/dictate/provider/RealtimeClient.kt) |
| 5 | `stt_on_device_whisper` | `voice_ai` | 🟢 Verified | [`local_whisper.py`](worker/arh_dictate/asr/local_whisper.py) / sherpa-onnx |
| 6 | `ai_prompt_rewording` | `voice_ai` | 🟢 Verified | [`DictateRewording.kt`](lib/dictate-core/src/main/kotlin/dev/patrickgold/florisboard/dictate/provider/DictateRewording.kt) |
| 7 | `markdown_clipboard_history` | `clipboard` | 🟢 Verified | [`ClipboardDatabase.kt`](app/src/main/kotlin/dev/patrickgold/florisboard/ime/clipboard/ClipboardDatabase.kt) |
| 8 | `transactional_clipboard_undo` | `clipboard` | 🟢 Verified | [`ClipboardHistoryDao.kt`](app/src/main/kotlin/dev/patrickgold/florisboard/ime/clipboard/ClipboardHistoryDao.kt) |
| 9 | `zero_gc_touch_dispatch` | `performance` | 🟢 Verified | [`TextKeyboardLayout.kt`](app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/keyboard/TextKeyboardLayout.kt) |
| 10 | `snygg_theme_engine` | `ui_engine` | 🟢 Verified | [`SnyggStylesheet.kt`](lib/snygg/src/main/kotlin/org/florisboard/lib/snygg/SnyggStylesheet.kt) |
| 11 | `floris_smartbar` | `ui_engine` | 🟢 Verified | [`SmartbarView.kt`](app/src/main/kotlin/dev/patrickgold/florisboard/ime/smartbar/SmartbarView.kt) |
| 12 | `custom_quick_text_snippets` | `productivity` | 🟢 Verified | [`QuickSnippetsManager.kt`](app/src/main/kotlin/dev/patrickgold/florisboard/ime/smartbar/quick/QuickSnippetsManager.kt) |

---

## 🛡️ Critical Architectural Invariants

1. **Zero-GC Touch Loop**:
   No object instantiations (`PointF`, `RectF`, `Iterator`, or collection wrappers) are permitted in `TextKeyboard.getKeyForPos()` or `TextKeyboardLayout.onTouchDownInternal()`. All calibrated offsets must reside in primitive arrays (`FloatArray`).
2. **Subtype Isolation**:
   Touch calibration offsets calibrated for a specific layout (e.g. QWERTY) must never contaminate alternate layouts (Arabic, Cyrillic, or Numeric keypad).
3. **Revertible Calibration History**:
   Every calibration iteration is appended to an immutable checkpoint stack, allowing the user to toggle A/B and revert to any prior state without losing tuning data.

---

## 🔬 Test & Preflight Gate Receipts

```
=== ARH Whisper Board Doctor: ✅ PASSED (121 ms) ===
  [✓] asbuilt_conformance: === [As-Built Doctor] ALL SPEC CONFORMANCE AUDITS PASSED ===
  [✓] zero_gc_touch_invariant: Zero heap allocations detected in getKeyForPos hot path.

=== Test Suite Execution: 17/17 PASSED (100% Success) ===
  [✓] KeyProximityTouchTest:
      - touch on right edge of O resolves to adjacent P character key: PASSED
      - touch on left edge of X resolves to adjacent Z character key: PASSED
      - touch on left edge of backspace resolves to adjacent M character key: PASSED
      - touch on left edge of S resolves to adjacent A character key: PASSED
      - touch on left edge of W resolves to adjacent Q character key: PASSED
      - touch on left edge of comma resolves to adjacent M character key: PASSED
      - touch on left edge of enter resolves to adjacent character key: PASSED
  [✓] TouchCalibrationProfileTest:
      - serialization and deserialization roundtrip preserves profile fidelity: PASSED
      - checkpoint stack manages undoable calibration iterations: PASSED
      - preset for poco f7 portrait has correct ergonomic offsets and margins: PASSED
      - corrupt json falls back to default profile safely without exception: PASSED
  [✓] TouchCalibrationSolverTest:
      - systematic thumb retraction on A and Q produces offset and increases left padding: PASSED
      - systematic right thumb undershoot on P produces negative offset and increases right padding: PASSED
      - solver handles empty tap samples gracefully: PASSED
  [✓] KeyProximityInfoTest:
      - subtype isolation ensures QWERTY calibration does not bleed into Arabic or Numeric keys: PASSED
      - disabled profile applies zero offsets: PASSED
      - profile offsets shift key centroids in KeyProximityInfo Layout: PASSED
```
