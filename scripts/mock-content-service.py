#!/usr/bin/env python3
"""
Standalone mock for the content / user / course endpoints used by:
  - ratings ops #6 (/ratings/meta/update) and #7 (/update/v1/content/additionaltag)
  - assessment submit (user validation)
  - cohorts: top-performers, active-users, auto-enrollment

No dependencies (Python 3 stdlib only). Listens on :8080 and returns Sunbird-style JSON.

Content / search / user:
  PATCH /system/v3/content/update/{id}   -> {"responseCode":"OK", ...}         (ratings #6 write-back)
  POST  /v1/content/search                -> additional-tag search (empty) OR, when the request filters
                                            by "identifier", a live course with a "batches" array
                                            (cohort active-users / auto-enroll batch lookup)
  POST  /private/user/v1/search          -> user validation (string userId) OR full user records
                                            (list userId, used by cohort enrichment)
  GET   /content/v3/hierarchy/{id}        -> content hierarchy (assess-1 -> parent course-1, etc.)

Course / LMS (cohorts):
  POST  /v1/batch/participants/list      -> participants of a batch
  GET   /v1/user/courses/list/{uuid}     -> a user's enrolled batches
  POST  /v1/course/batch/create          -> new batchId (or none, for the 502 path)
  POST  /v1/course/enroll                -> OK

Point the app at it via application.properties (already the defaults):
  content.search-url            = http://localhost:8080/v1/content/search
  content.update-url            = http://localhost:8080/system/v3/content/update/
  user.search-url               = http://localhost:8080/private/user/v1/search
  cohorts.course.service-host   = http://localhost:8080/
  (content.read-url / content.hierarchy-url stay on the REAL content service on :9000 by default;
   set CONTENT_HIERARCHY_URL=http://localhost:8080/content/v3/hierarchy to mock hierarchy too.)

Run:
  python3 scripts/mock-content-service.py
  # optional custom port:  MOCK_PORT=8085 python3 scripts/mock-content-service.py

Cohort test fixtures (see scripts/README or the Postman "Cohorts" folder):
  course-1          -> one open batch "batch-1"; participants [user-1, user-2]
  course-emptybatch -> one open batch "batch-empty"; NO participants   (active-users -> 404)
  course-nobatch    -> NO batches                                      (active-users -> 404; auto-enroll creates)
  course-createfail -> NO batches, and batch-create returns no id      (auto-enroll -> 502)
  user "enrolled-user" is already in batch-1; any other user is not.
NOTE: top-performers additionally reads the Cassandra MV user_assessment_top_performer, which this
HTTP mock cannot provide — seed a row there for a 200, otherwise the endpoint returns 404.
"""
import json
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

PORT = int(os.environ.get("MOCK_PORT", "8080"))


def _open_batch(batch_id):
    return {"batchId": batch_id, "endDate": None, "name": "Open Batch", "enrollmentType": "open",
            "enrollmentEndDate": None, "startDate": "2026-01-01", "status": 1, "createdFor": []}


def batches_for(course_id):
    if course_id in ("course-nobatch", "course-createfail"):
        return []
    if course_id == "course-emptybatch":
        return [_open_batch("batch-empty")]
    if course_id == "course-1":
        return [_open_batch("batch-1")]
    return [_open_batch("batch-" + course_id)]  # arbitrary course -> one open batch


def participants_for(batch_id):
    return [] if batch_id == "batch-empty" else ["user-1", "user-2"]


def user_record(uid):
    return {"userId": uid, "firstName": "First-" + uid, "lastName": "Last-" + uid,
            "email": uid + "@example.com", "channel": "dept-" + uid,
            "profileDetails": {"professionalDetails": [{"designation": "Engineer", "designationOther": None}]}}


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
        if "chunked" in te:  # Spring's JDK HttpClient sends bodies chunked
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
        req = (body or {}).get("request") or {}

        if self.path.startswith("/v1/content/search"):
            filters = req.get("filters") or {}
            identifier = filters.get("identifier")
            if identifier:  # cohort batch lookup (searchLiveContent by identifier)
                cid = identifier[0] if isinstance(identifier, list) else identifier
                content = [{"identifier": cid, "name": "Course " + cid, "primaryCategory": "Course",
                            "leafNodesCount": 1, "contentType": "Course", "batches": batches_for(cid)}]
                self._send({"id": "api.content.search", "ver": "3.0", "responseCode": "OK",
                            "result": {"count": len(content), "content": content}})
            else:  # additional-tag search (ratings #7) -> empty by default
                self._send({"id": "api.content.search", "ver": "3.0", "responseCode": "OK",
                            "result": {"count": 0, "content": []}})

        elif self.path.startswith("/private/user/v1/search"):
            uid = (req.get("filters") or {}).get("userId")
            if isinstance(uid, list):  # cohort enrichment -> full records
                content = [user_record(u) for u in uid]
                self._send({"id": "api.user.search", "ver": "v1", "responseCode": "OK",
                            "result": {"response": {"count": len(content), "content": content}}})
            else:  # assessment validateUser -> "invalid-user" is rejected
                count = 0 if uid == "invalid-user" else 1
                self._send({"id": "api.user.search", "ver": "v1", "responseCode": "OK",
                            "result": {"response": {"count": count,
                                                    "content": ([] if count == 0 else [{"userId": uid}])}}})

        elif self.path.startswith("/v1/batch/participants/list"):
            batch_id = (req.get("batch") or {}).get("batchId")
            participants = participants_for(batch_id)
            self._send({"id": "api.course.participants", "ver": "v1", "responseCode": "OK",
                        "result": {"batch": {"count": len(participants), "participants": participants}}})

        elif self.path.startswith("/v1/course/batch/create"):
            course_id = req.get("courseId")
            result = {} if course_id == "course-createfail" else {"batchId": "batch-new"}
            self._send({"id": "api.course.batch.create", "ver": "v1", "responseCode": "OK", "result": result})

        elif self.path.startswith("/v1/course/enroll"):
            self._send({"id": "api.course.enroll", "ver": "v1", "responseCode": "OK",
                        "result": {"status": "success"}})

        else:
            self._send({"responseCode": "OK", "result": {}})

    def do_GET(self):
        if "/content/v3/hierarchy/" in self.path:
            cid = self.path.split("/content/v3/hierarchy/")[1].split("?")[0]
            print(f"[MOCK] GET hierarchy {cid}", flush=True)
            if cid == "assess-1":
                content = {"identifier": "assess-1", "parent": "course-1", "contentType": "SelfAssess"}
            elif cid == "course-1":
                content = {"identifier": "course-1", "parent": None, "contentType": "Course"}
            else:
                content = {"identifier": cid, "parent": None, "contentType": "Resource"}
            self._send({"responseCode": "OK", "result": {"content": content}})
            return

        if "/v1/user/courses/list/" in self.path:
            uuid = self.path.split("/v1/user/courses/list/")[1].split("?")[0]
            print(f"[MOCK] GET user-courses {uuid}", flush=True)
            courses = ([{"batchId": "batch-1", "courseId": "course-1", "userId": uuid}]
                       if uuid == "enrolled-user" else [])
            self._send({"id": "api.user.courses.list", "ver": "v1", "responseCode": "OK",
                        "result": {"courses": courses}})
            return

        self._send({"responseCode": "OK", "result": {"healthy": True}})

    def log_message(self, *args):
        pass  # silence default access logging; we print our own lines


if __name__ == "__main__":
    print(f"[MOCK] content/user/course mock listening on :{PORT} "
          f"(ratings, assessment, cohorts)", flush=True)
    ThreadingHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
