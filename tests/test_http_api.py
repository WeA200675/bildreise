import http.client
import json
import sys
import threading
import unittest
from pathlib import Path
from http.server import ThreadingHTTPServer

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "server"))
from main import Handler
from dream_provider import MockProvider


class BrokenProvider:
    def generate(self, request):
        raise RuntimeError("private diagnostic")


class APIHandler(Handler):
    provider = MockProvider()
    MAX_BODY = 1024

    def log_message(self, *args):
        pass


class HTTPTests(unittest.TestCase):
    def setUp(self):
        APIHandler.provider = MockProvider()
        self.server = ThreadingHTTPServer(("127.0.0.1", 0), APIHandler)
        self.thread = threading.Thread(target=self.server.serve_forever)
        self.thread.start()
        self.addCleanup(self.server.server_close)
        self.addCleanup(self.thread.join)
        self.addCleanup(self.server.shutdown)

    def request(self, body, headers=None):
        conn = http.client.HTTPConnection(*self.server.server_address, timeout=3)
        try:
            conn.request("POST", "/dream/generate", body=body, headers=headers or {})
            response = conn.getresponse()
            return response.status, json.loads(response.read())
        finally:
            conn.close()

    def test_invalid_requests(self):
        for body in (b"", b"[]", b"{", b'{"image_base64":42}', b'{"image_base64":""}'):
            with self.subTest(body=body):
                self.assertEqual(self.request(body), (400, {"error": "invalid_request"}))

    def test_negative_length(self):
        self.assertEqual(self.request(b"", {"Content-Length": "-1"}),
                         (400, {"error": "invalid_request"}))

    def test_oversize(self):
        self.assertEqual(self.request(b"", {"Content-Length": "1025"}),
                         (413, {"error": "payload_too_large"}))

    def test_provider_failure_and_recovery(self):
        body = b'{"image_base64":"aGVsbG8=","seed":42}'
        APIHandler.provider = BrokenProvider()
        self.assertEqual(self.request(body), (503, {"error": "provider_unavailable"}))
        APIHandler.provider = MockProvider()
        status, result = self.request(body)
        self.assertEqual(status, 200)
        self.assertEqual(result["seed"], 42)
        self.assertEqual(result["provider"], "mock")


if __name__ == "__main__":
    unittest.main()
