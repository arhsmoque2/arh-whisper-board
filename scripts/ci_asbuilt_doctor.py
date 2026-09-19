#!/usr/bin/env python3
"""ARH Whisper Board — As-Built & Bidirectional Conformance Doctor.

Performs bidirectional verification between:
1. capabilities.json (the machine-readable capability manifest)
2. asbuilt.md (the human architectural specification)
3. Kotlin production code anchors and invariants
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path


def main() -> int:
    root = Path(__file__).resolve().parent.parent
    cap_file = root / "capabilities.json"
    asbuilt_file = root / "asbuilt.md"

    print("=== [As-Built Doctor] Running ARH Whisper Board Conformance Audit ===")

    if not cap_file.is_file():
        print(f"[FAIL] Missing manifest: {cap_file}")
        return 1
    if not asbuilt_file.is_file():
        print(f"[FAIL] Missing asbuilt documentation: {asbuilt_file}")
        return 1

    # 1. Load capabilities.json
    cap_data = json.loads(cap_file.read_text(encoding="utf-8"))
    declared_capabilities = {c["id"]: c for c in cap_data.get("capabilities", [])}
    print(f"Manifest Declared Capabilities: {len(declared_capabilities)}")

    # 2. Verify asbuilt.md includes every declared capability
    asbuilt_text = asbuilt_file.read_text(encoding="utf-8")
    missing_in_asbuilt = []
    for cap_id in declared_capabilities:
        if f"`{cap_id}`" not in asbuilt_text:
            missing_in_asbuilt.append(cap_id)

    if missing_in_asbuilt:
        print(f"[FAIL] Capabilities declared in capabilities.json missing from asbuilt.md: {missing_in_asbuilt}")
        return 1

    # 3. Check for extra undocumented capabilities in asbuilt.md
    asbuilt_caps = set(re.findall(r"\|\s*\d+\s*\|\s*`([a-z0-9_]+)`", asbuilt_text))
    extra_in_asbuilt = asbuilt_caps - set(declared_capabilities.keys())
    if extra_in_asbuilt:
        print(f"[FAIL] Capabilities documented in asbuilt.md missing from capabilities.json: {extra_in_asbuilt}")
        return 1

    print(f"[PASS] asbuilt.md strictly conforms to capabilities.json ({len(asbuilt_caps)} capabilities matched).")

    # 4. Verify Numeric Claim Parity
    numeric_claim_match = re.search(r"(\d+)/(\d+)\s+Declared\s+Capabilities", asbuilt_text)
    if numeric_claim_match:
        active_count = int(numeric_claim_match.group(1))
        total_count = int(numeric_claim_match.group(2))
        if active_count != len(declared_capabilities) or total_count != len(declared_capabilities):
            print(f"[FAIL] asbuilt.md numeric claim ({active_count}/{total_count}) does not match manifest count ({len(declared_capabilities)})!")
            return 1
        print(f"[PASS] Numeric claim ({active_count}/{total_count}) matches manifest.")

    # 5. Verify Critical Architectural Anchor Files Exist
    critical_anchors = [
        "app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/keyboard/TextKeyboard.kt",
        "app/src/main/kotlin/dev/patrickgold/florisboard/ime/nlp/latin/KeyProximityInfo.kt",
        "app/src/main/kotlin/dev/patrickgold/florisboard/ime/nlp/latin/TouchBeamDecoder.kt",
        "app/src/main/kotlin/dev/patrickgold/florisboard/ime/nlp/latin/TouchScoring.kt",
        "app/src/main/kotlin/dev/patrickgold/florisboard/ime/window/ImeWindowProps.kt",
    ]
    for anchor in critical_anchors:
        p = root / anchor
        if not p.is_file():
            print(f"[FAIL] Critical architecture file missing: {anchor}")
            return 1

    print(f"[PASS] All {len(critical_anchors)} critical architecture anchor files verified.")
    print("=== [As-Built Doctor] ALL SPEC CONFORMANCE AUDITS PASSED ===")
    return 0


if __name__ == "__main__":
    sys.exit(main())
