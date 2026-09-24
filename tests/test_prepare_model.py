import hashlib
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from zipfile import ZipFile


class PrepareModelTests(unittest.TestCase):
    def test_mobile_bundle_script_prints_https_configuration(self):
        root = Path(__file__).resolve().parents[1]
        with tempfile.TemporaryDirectory() as tmp:
            model = Path(tmp) / "model"
            model.mkdir()
            (model / "weights.bin").write_bytes(b"weights")
            output = Path(tmp) / "bundle.zip"
            result = subprocess.run([
                sys.executable, str(root / "tools/build_mobile_bundle.py"),
                str(model), str(output), "--url", "https://example.test/model.zip",
            ], check=True, capture_output=True, text=True)
            self.assertIn("bildreise.modelBundleUrl=https://example.test/model.zip", result.stdout)
            self.assertRegex(result.stdout, r"bildreise\.modelBundleSha256=[0-9a-f]{64}")

    def test_bundle_contains_manifest_and_hash(self):
        root = Path(__file__).resolve().parents[1]
        with tempfile.TemporaryDirectory() as tmp:
            model = Path(tmp) / "model"
            model.mkdir()
            (model / "weights.bin").write_bytes(b"weights")
            output = Path(tmp) / "bundle.zip"
            subprocess.run([sys.executable, str(root / "tools/prepare_mediapipe_model.py"), str(model), str(output)], check=True)
            with ZipFile(output) as archive:
                manifest = json.loads(archive.read("manifest.json"))
            self.assertEqual(manifest["files"], ["weights.bin"])
            self.assertEqual(manifest["sha256"]["weights.bin"], hashlib.sha256(b"weights").hexdigest())


if __name__ == "__main__":
    unittest.main()
