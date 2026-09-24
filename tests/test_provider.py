import base64
import json
import os
import sys
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "server"))
from dream_provider import DreamRequest, MockProvider, build_prompt, inject_prompt_and_seed


class ProviderTests(unittest.TestCase):
    def test_request_is_clamped_and_seeded(self):
        req = DreamRequest("aGVsbG8=", recognition=9, intensity=-1).normalized()
        self.assertEqual(req.recognition, .9)
        self.assertEqual(req.intensity, .1)
        self.assertIsInstance(req.seed, int)

    def test_prompt_keeps_subject(self):
        req = DreamRequest("aGVsbG8=", profile="sculpture", recognition=.8).normalized()
        self.assertIn("preserve the main subject silhouette", build_prompt(req))

    def test_mock_is_deterministically_addressable(self):
        result = MockProvider().generate(DreamRequest("aGVsbG8=", seed=42))
        self.assertEqual(result["seed"], 42)
        self.assertEqual(result["status"], "ready")

    def test_workflow_injection(self):
        workflow = {"1": {"inputs": {"text": "old", "seed": 1}}}
        result = inject_prompt_and_seed(workflow, "new", 99)
        self.assertEqual(result["1"]["inputs"], {"text": "new", "seed": 99})


if __name__ == "__main__":
    unittest.main()
