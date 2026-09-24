#!/usr/bin/env python3
"""Create a deployable, hashed mobile model bundle and build configuration.

The model conversion is intentionally external: this script never downloads
untrusted or license-ambiguous weights. It accepts the already converted
directory, delegates packaging to prepare_mediapipe_model.py, and prints the
exact Gradle properties needed by the Android build.
"""
from __future__ import annotations

import argparse
import hashlib
import subprocess
import sys
from pathlib import Path


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("converted_dir", type=Path)
    parser.add_argument("output_zip", type=Path)
    parser.add_argument("--url", required=True, help="HTTPS URL where the ZIP will be hosted")
    args = parser.parse_args()
    if not args.converted_dir.is_dir():
        raise SystemExit("converted_dir must be a directory")
    if not args.url.lower().startswith("https://"):
        raise SystemExit("--url must use HTTPS")
    command = [sys.executable, str(Path(__file__).with_name("prepare_mediapipe_model.py")),
               str(args.converted_dir), str(args.output_zip), "--backend", "mobile-diffusion"]
    subprocess.run(command, check=True)
    digest = hashlib.sha256(args.output_zip.read_bytes()).hexdigest()
    print(f"bildreise.modelBundleUrl={args.url}")
    print(f"bildreise.modelBundleSha256={digest}")
    print("Use these two values via ~/.gradle/gradle.properties or -P.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
