#!/usr/bin/env python3
"""
Fetches and extracts vendored sherpa-onnx and onnxruntime native libraries and JARs
for on-device STT and VAD. Cross-platform replacement for fetch-sherpa-onnx.sh.
"""

import hashlib
import os
import shutil
import sys
import urllib.request
import zipfile
from pathlib import Path

VERSION = "1.13.3"
ORT_VERSION = "1.24.3"
AAR_SHA256 = "243ad797a3b6e75ebbeaf7a2ab4aec0777e7d71b730685abb762a120940b07b6"
URL = f"https://github.com/k2-fsa/sherpa-onnx/releases/download/v{VERSION}/sherpa-onnx-{VERSION}.aar"

ORT_AAR_SHA256 = "67397e4a970e75617f765d2015ceaf911917e1d822276cfb5792744e8085cbce"
ORT_URL = f"https://repo1.maven.org/maven2/com/microsoft/onnxruntime/onnxruntime-android/{ORT_VERSION}/onnxruntime-android-{ORT_VERSION}.aar"

SO_FILES = ["libonnxruntime.so", "libsherpa-onnx-jni.so"]
ABIS = ["arm64-v8a", "armeabi-v7a", "x86_64"]


def verify_sha256(filepath: Path, expected: str):
    h = hashlib.sha256()
    with open(filepath, "rb") as f:
        while chunk := f.read(65536):
            h.update(chunk)
    digest = h.hexdigest().lower()
    if digest != expected.lower():
        raise ValueError(f"Checksum mismatch for {filepath}: got {digest}, expected {expected}")


def download(url: str, dst: Path):
    print(f"Downloading {url} ...")
    urllib.request.urlretrieve(url, dst)


def main():
    repo_root = Path(__file__).resolve().parent.parent
    libs_dir = repo_root / "app" / "libs"
    jni_root = repo_root / "app" / "src" / "main" / "jniLibs"
    jar_dst = libs_dir / f"sherpa-onnx-{VERSION}.jar"
    ort_jar_dst = libs_dir / f"onnxruntime-android-{ORT_VERSION}.jar"

    force = "--force" in sys.argv
    if not force and jar_dst.exists() and ort_jar_dst.exists():
        all_so_exist = True
        for abi in ABIS:
            for so in SO_FILES:
                if not (jni_root / abi / so).exists():
                    all_so_exist = False
            if not (jni_root / abi / "libonnxruntime4j_jni.so").exists():
                all_so_exist = False
        if all_so_exist:
            print(f"[OK] sherpa-onnx {VERSION} already present (use --force to re-fetch).")
            return

    libs_dir.mkdir(parents=True, exist_ok=True)
    for abi in ABIS:
        (jni_root / abi).mkdir(parents=True, exist_ok=True)

    cache_dir = repo_root / ".cache"
    cache_dir.mkdir(parents=True, exist_ok=True)
    sherpa_aar = cache_dir / f"sherpa-onnx-{VERSION}.aar"
    ort_aar = cache_dir / f"onnxruntime-android-{ORT_VERSION}.aar"

    if not sherpa_aar.exists():
        download(URL, sherpa_aar)
    verify_sha256(sherpa_aar, AAR_SHA256)
    print("[OK] sherpa-onnx checksum verified.")

    if not ort_aar.exists():
        download(ORT_URL, ort_aar)
    verify_sha256(ort_aar, ORT_AAR_SHA256)
    print("[OK] onnxruntime checksum verified.")

    print(f"Extracting sherpa-onnx ({', '.join(ABIS)}) ...")
    with zipfile.ZipFile(sherpa_aar) as z:
        with z.open("classes.jar") as f, open(jar_dst, "wb") as o:
            shutil.copyfileobj(f, o)
        for abi in ABIS:
            for so in SO_FILES:
                target = jni_root / abi / so
                with z.open(f"jni/{abi}/{so}") as f, open(target, "wb") as o:
                    shutil.copyfileobj(f, o)

    print(f"Extracting onnxruntime ({', '.join(ABIS)}) ...")
    with zipfile.ZipFile(ort_aar) as z:
        with z.open("classes.jar") as f, open(ort_jar_dst, "wb") as o:
            shutil.copyfileobj(f, o)
        for abi in ABIS:
            target = jni_root / abi / "libonnxruntime4j_jni.so"
            with z.open(f"jni/{abi}/libonnxruntime4j_jni.so") as f, open(target, "wb") as o:
                shutil.copyfileobj(f, o)

    print("[OK] sherpa-onnx and onnxruntime native libs installed successfully.")


if __name__ == "__main__":
    main()
