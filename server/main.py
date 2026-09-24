"""Small dependency-free API for the generative dream journey."""
from __future__ import annotations

import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

from dream_provider import DreamRequest, create_provider


class Handler(BaseHTTPRequestHandler):
    provider = create_provider()

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
            length = int(self.headers.get("Content-Length", "0"))
            if length > 20_000_000:
                raise ValueError("image payload too large")
            body = json.loads(self.rfile.read(length).decode())
            request = DreamRequest(**body).normalized()
            if not request.image_base64:
                raise ValueError("image_base64 is required")
            self.send_json(200, self.provider.generate(request))
        except Exception as exc:
            self.send_json(400, {"error": str(exc)})


if __name__ == "__main__":
    ThreadingHTTPServer(("127.0.0.1", 8787), Handler).serve_forever()
