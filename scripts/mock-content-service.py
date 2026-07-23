#!/usr/bin/env python3
"""
Standalone mock for the content system-update (PATCH) and content search (POST) endpoints
used by the ratings ops jobs #6 (/ratings/meta/update) and #7 (/update/v1/content/additionaltag).

No dependencies (Python 3 stdlib only). Listens on :8080 and returns Sunbird-style JSON:

  PATCH /system/v3/content/update/{id}   -> {"responseCode":"OK", ...}      (the write-back)
  POST  /v1/search                       -> {"result":{"count":0,"content":[]}}  (currently-tagged content)
  GET   /*                               -> a simple healthy payload

Point the app at it via application.properties (already the defaults):
  content.search-url = http://localhost:8080/v1/search
  content.update-url = http://localhost:8080/system/v3/content/update/
  (content.read-url stays on the REAL content service, e.g. http://localhost:9000/content/v4/read)

Run:
  python3 scripts/mock-content-service.py
  # optional custom port:  MOCK_PORT=8085 python3 scripts/mock-content-service.py

To exercise the #7 remove-tag path, make do_POST(/v1/search) return real identifiers, e.g.:
  {"responseCode":"OK","result":{"count":1,"content":[{"identifier":"do_XXXX"}]}}
"""
import json
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

PORT = int(os.environ.get("MOCK_PORT", "8080"))


class Handler(BaseHTTPRequestHandler):
    def _send(self, obj, code=200):
        body = json.dumps(obj).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _read_body(self):
        te = (self.headers.get("Transfer-Encoding") or "").lower()
        if "chunked" in te:  # Spring's JDK HttpClient sends PATCH bodies chunked
            raw = b""
            while True:
                size_line = self.rfile.readline().strip()
                if not size_line:
                    break
                try:
                    size = int(size_line, 16)
                except ValueError:
                    break
                if size == 0:
                    self.rfile.readline()
                    break
                raw += self.rfile.read(size)
                self.rfile.readline()
        else:
            n = int(self.headers.get("Content-Length", 0) or 0)
            raw = self.rfile.read(n) if n else b""
        try:
            return json.loads(raw) if raw else {}
        except Exception:
            return {}

    def do_PATCH(self):
        body = self._read_body()
        cid = self.path.rsplit("/", 1)[-1]
        print(f"[MOCK] PATCH {self.path}\n        body={json.dumps(body)[:400]}", flush=True)
        self._send({"id": "api.content.update", "ver": "3.0", "responseCode": "OK",
                    "result": {"identifier": cid, "status": "success"}})

    def do_POST(self):
        body = self._read_body()
        print(f"[MOCK] POST {self.path}  body={json.dumps(body)[:300]}", flush=True)
        if self.path.startswith("/v1/search"):
            self._send({"id": "api.content.search", "ver": "3.0", "responseCode": "OK",
                        "result": {"count": 0, "content": []}})
        else:
            self._send({"responseCode": "OK", "result": {}})

    def do_GET(self):
        self._send({"responseCode": "OK", "result": {"healthy": True}})

    def log_message(self, *args):
        pass  # silence default access logging; we print our own lines


if __name__ == "__main__":
    print(f"[MOCK] content/learning mock listening on :{PORT} "
          f"(PATCH /system/v3/content/update/{{id}}, POST /v1/search)", flush=True)
    ThreadingHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
