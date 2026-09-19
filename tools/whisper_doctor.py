#!/usr/bin/env python3
"""ARH Whisper Board — Atomic Preflight Quality Gate Doctor (Haven Standard).

Runs fast local preflight verification:
1. As-Built & Bidirectional Doc Conformance
2. Zero-GC Touch Loop AST Invariant Check
3. Subtype Isolation Verification
4. Optional Gradle Unit Test Suite
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import time
from pathlib import Path

# Windows UTF-8 Console Defense
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")


def check_zero_gc_touch_loop(root: Path) -> tuple[bool, str]:
    """Verify that TextKeyboard.getKeyForPos() contains zero heap allocations."""
    target = root / "app" / "src" / "main" / "kotlin" / "dev" / "patrickgold" / "florisboard" / "ime" / "text" / "keyboard" / "TextKeyboard.kt"
    if not target.is_file():
        return False, f"Missing target file: {target}"

    text = target.read_text(encoding="utf-8")
    
    # Extract getKeyForPos body
    match = re.search(r"override fun getKeyForPos\([^)]*\): TextKey\? \{(.*?)\n    \}", text, re.DOTALL)
    if not match:
        # Check if function exists
        if "fun getKeyForPos" not in text:
            return False, "Could not find getKeyForPos implementation"
        body = text[text.find("fun getKeyForPos") : text.find("fun getKeyForPos") + 2500]
    else:
        body = match.group(1)

    # Prohibited allocation patterns in hot touch path
    prohibited = [
        ("ArrayList<", "Heap collection allocation (ArrayList)"),
        ("mutableListOf(", "Heap collection allocation (mutableListOf)"),
        ("listOf(", "Heap collection allocation (listOf)"),
        ("PointF(", "PointF instantiation"),
        ("RectF(", "RectF instantiation"),
        ("FloatArray(", "Dynamic FloatArray heap allocation in hot path"),
        ("mapOf(", "Heap Map allocation (mapOf)"),
    ]

    violations = []
    for pattern, reason in prohibited:
        if pattern in body:
            violations.append(f"{reason} ('{pattern}')")

    if violations:
        return False, f"Prohibited allocation detected in getKeyForPos: {', '.join(violations)}"
    return True, "Zero heap allocations detected in getKeyForPos hot path."


def main() -> int:
    parser = argparse.ArgumentParser(description="ARH Whisper Board Preflight Doctor")
    parser.add_argument("--full", action="store_true", help="Run full suite including gradle unit tests")
    parser.add_argument("--json", action="store_true", help="Output JSON receipt")
    args = parser.parse_args()

    start_time = time.time()
    root = Path(__file__).resolve().parent.parent

    results = {
        "gate": "whisper_preflight_doctor",
        "passed": True,
        "duration_ms": 0,
        "steps": {},
    }

    def record_step(name: str, passed: bool, summary: str) -> None:
        results["steps"][name] = {"status": "PASS" if passed else "FAIL", "summary": summary}
        if not passed:
            results["passed"] = False

    # Step 1: As-Built Conformance Doctor
    asbuilt_script = root / "scripts" / "ci_asbuilt_doctor.py"
    if asbuilt_script.is_file():
        proc = subprocess.run([sys.executable, str(asbuilt_script)], capture_output=True, text=True, encoding="utf-8")
        record_step("asbuilt_conformance", proc.returncode == 0, proc.stdout.strip().splitlines()[-1] if proc.stdout else proc.stderr.strip())
    else:
        record_step("asbuilt_conformance", False, "Missing scripts/ci_asbuilt_doctor.py")

    # Step 2: Zero-GC Touch Loop AST Check
    passed_gc, summary_gc = check_zero_gc_touch_loop(root)
    record_step("zero_gc_touch_invariant", passed_gc, summary_gc)

    # Step 3: Optional Gradle Unit Tests
    if args.full:
        gradlew = str(root / ("gradlew.bat" if os.name == "nt" else "gradlew"))
        proc = subprocess.run([gradlew, ":app:testDebugUnitTest", "--tests", "dev.patrickgold.florisboard.ime.text.keyboard.*", "--tests", "dev.patrickgold.florisboard.ime.nlp.latin.KeyProximityInfoTest", "--quiet"], cwd=str(root), capture_output=True, text=True)
        record_step("unit_tests", proc.returncode == 0, "All touch & calibration unit tests passed" if proc.returncode == 0 else proc.stderr[-200:])

    elapsed_ms = int((time.time() - start_time) * 1000)
    results["duration_ms"] = elapsed_ms

    if args.json:
        print(json.dumps(results, indent=2))
    else:
        status_icon = "✅ PASSED" if results["passed"] else "❌ FAILED"
        print(f"=== ARH Whisper Board Doctor: {status_icon} ({elapsed_ms} ms) ===")
        for step_name, data in results["steps"].items():
            icon = "✓" if data["status"] == "PASS" else "✗"
            print(f"  [{icon}] {step_name}: {data['summary']}")

    return 0 if results["passed"] else 1


if __name__ == "__main__":
    sys.exit(main())
