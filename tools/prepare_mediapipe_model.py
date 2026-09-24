#!/usr/bin/env python3
"""Validate a converted MediaPipe Image Generator bundle.

Conversion itself is intentionally delegated to Google's official MediaPipe
conversion script. This tool makes the final bundle reproducible and safe to
serve to the Android downloader.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("model_dir", type=Path)
    parser.add_argument("output_zip", type=Path)
    parser.add_argument("--backend", default="mobile-diffusion", choices=("mobile-diffusion", "mediapipe-sd15"))
    args = parser.parse_args()
    if not args.model_dir.is_dir():
        raise SystemExit("model_dir must be a directory")
    files = sorted(p for p in args.model_dir.rglob("*") if p.is_file())
    if not files:
        raise SystemExit("model_dir is empty")
    manifest = {
        "schema": 1,
        "backend": args.backend,
        "files": [str(p.relative_to(args.model_dir)) for p in files],
        "sha256": {str(p.relative_to(args.model_dir)): sha256(p) for p in files},
    }
    args.output_zip.parent.mkdir(parents=True, exist_ok=True)
    with ZipFile(args.output_zip, "w", ZIP_DEFLATED) as archive:
        for file in files:
            archive.write(file, file.relative_to(args.model_dir))
        archive.writestr("manifest.json", json.dumps(manifest, indent=2, sort_keys=True))
    print(json.dumps({"bundle": str(args.output_zip), "sha256": sha256(args.output_zip), "files": len(files)}))


if __name__ == "__main__":
    main()
