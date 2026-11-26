"""Small dependency-free API for the generative dream journey."""
from __future__ import annotations

import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from socket import timeout as SocketTimeout

from dream_provider import DreamRequest, create_provider


class Handler(BaseHTTPRequestHandler):
    provider = create_provider()
    MAX_BODY = 20_000_000

    def setup(self):
        super().setup()
        self.connection.settimeout(15)

    def send_json(self, status: int, body: dict):
        encoded = json.dumps(body).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Content-Length", str(len(encoded)))
        self.end_headers()
        self.wfile.write(encoded)

    def do_GET(self):
        if self.path == "/health":
            self.send_json(200, {"status": "ok", "provider": type(self.provider).__name__})
            return
        self.send_json(404, {"error": "not_found"})

    def do_OPTIONS(self):
        self.send_response(204)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.end_headers()

    def do_POST(self):
        if self.path != "/dream/generate":
            self.send_json(404, {"error": "not_found"})
            return
        try:
            lengths = self.headers.get_all("Content-Length", [])
            if len(lengths) != 1 or not lengths[0].isascii() or not lengths[0].isdigit() or self.headers.get("Transfer-Encoding"):
                raise ValueError("invalid content length")
            length = int(lengths[0])
            if length > self.MAX_BODY:
                self.send_json(413, {"error": "payload_too_large"})
                return
            if length == 0:
                raise ValueError("empty body")
            raw = self.rfile.read(length)
            if len(raw) != length:
                raise ValueError("incomplete body")
            body = json.loads(raw.decode())
            if not isinstance(body, dict) or not isinstance(body.get("image_base64"), str):
                raise ValueError("invalid image request")
            request = DreamRequest(**body).normalized()
            if not request.image_base64:
                raise ValueError("image_base64 is required")
        except SocketTimeout:
            self.send_json(408, {"error": "request_timeout"})
            return
        except (ValueError, TypeError, OverflowError):
            self.send_json(400, {"error": "invalid_request"})
            return
        try:
            result = self.provider.generate(request)
        except Exception:
            self.send_json(503, {"error": "provider_unavailable"})
            return
        self.send_json(200, result)


if __name__ == "__main__":
    ThreadingHTTPServer(("127.0.0.1", 8787), Handler).serve_forever()
