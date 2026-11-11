# Competency Certificate — Demo Guide

## What is it?

When a learner completes **all five levels** of a competency, they now get a **competency
certificate** automatically. No one has to request it or press a button. Learners and admins can then
look up the certificate's status and download it.

Each organisation decides for itself:
- whether its learners get competency certificates,
- which competencies earn one (all of them, or only selected ones), and
- which certificate design (template) is used.

All of this comes from the organisation's existing **form configuration**, so a new organisation is
set up with configuration alone and no code change.

---

## How it works, step by step

### 1. It starts with a passbook update

Whenever a learner earns a competency level, the platform records it in the learner's **passbook**
(the *Update Passbook* API). This is where the certificate journey begins.

As soon as the passbook is saved, the update is confirmed straight away. Certificate generation then
starts **in the background (asynchronously)**, so the learner never waits for a certificate to be
created, and a problem with certificates can never block a passbook update.

### 2. Find out who the learner is (User Read)

The background process first looks up the learner's profile to find:
- their **organisation** (root org), which decides which settings apply, and
- their **name**, which is printed on the certificate.

### 3. Check the organisation's settings (Form Read)

Next it reads the organisation's form configuration and looks for the competency certificate
settings:

```json
"certificateConfig": {
  "competency": {
    "competencyIds": [],
    "templateUrl": "<certificate template id>"
  }
}
```

- **No settings found:** the organisation hasn't switched certificates on, so the process stops quietly.
- **`competencyIds`:** which competencies earn a certificate. An **empty list means all competencies**.
- **`templateUrl`:** the **certificate template id**, the design used for this organisation's
  certificates. This is how the template is chosen: it always comes from the organisation's form
  configuration.

### 4. Check the learner has completed the competency

For each competency in that passbook update that the organisation allows, the process checks the
learner's passbook to confirm they hold **every level, 1 to 5**. If any level is missing, nothing
happens yet; the certificate is issued once the final level is earned.

### 5. Make sure it isn't issued twice

If the learner already has a certificate for that competency, it's skipped. Each learner gets
**one certificate per competency**.

### 6. Generate the certificate (RC proxy mapper)

The process sends the request to the **RC proxy mapper**, the certificate service, with:
- the **template id** from the organisation's form configuration,
- the learner's **name**,
- the **competency name**, and
- the date of issue.

The certificate service creates the certificate from the template, stores it and returns a link to
it.

### 7. Save the result

The outcome is recorded against the learner and competency:

| Status | Meaning |
|---|---|
| **GENERATED** | The certificate was created and its download link is saved |
| **FAILED** | Something went wrong; it's retried automatically on the learner's next passbook update |

---

## The flow at a glance

```
Learner earns a competency level
        │
        ▼
Update Passbook ──► passbook saved, response sent immediately
        │
        ▼   (background)
User Read ──────► learner's organisation + name
        │
        ▼
Form Read ──────► is the org enabled? which competencies? which template id?
        │
        ▼
Passbook check ─► all levels 1–5 completed?
        │
        ▼
Already issued? ─► yes → stop
        │ no
        ▼
RC Proxy Mapper ► certificate created from the template → link returned
        │
        ▼
Result saved ───► GENERATED / FAILED
```

---

## Viewing and downloading certificates

Two new APIs are available:

| API | What it gives |
|---|---|
| **Certificate Status** `POST /v1/certificate/competency/status` | Status of the certificates (generated or failed) |
| **Certificate Download** `POST /v1/certificate/competency/download` | Download links for generated certificates only |

Both can be searched by **organisation**, **learner** or **competency**, in any combination:

```json
{ "request": { "filters": { "rootOrgId": "...", "userId": "...", "competencyId": "..." } } }
```

At least one filter is needed. If nothing matches, an empty list is returned.

---

## Demo script

Use a learner whose organisation has competency certificates configured, and who has completed all
five levels of a competency.

**Step 1: Show the organisation's settings.** Open the org's form configuration and point out
`certificateConfig.competency`: the allowed competencies and the template id.

```bash
curl -s -X POST 'https://portal-staging.aastrika.org/apis/v1/form/read' \
  -H 'Content-Type: application/json' \
  -d '{"request":{"type":"web_layout","subtype":"v1","action":"get","component":"ekshamata","framework":"v2","rootOrgId":"<ROOT_ORG_ID>"}}'
```

**Step 2: Update the learner's passbook.** This starts the process.

```bash
curl -s -X PATCH 'http://localhost:7011/user/v1/passbook' \
  -H 'Content-Type: application/json' \
  -H 'x-authenticated-userid: <ACTING_USER_ID>' \
  -d '{
    "request": {
      "userId": "<USER_ID>",
      "typeName": "competency",
      "competencyDetails": [
        {
          "competencyId": "<COMPETENCY_ID>",
          "additionalParams": { "competencyName": "<COMPETENCY_NAME>" },
          "acquiredDetails": {
            "acquiredChannel": "course",
            "competencyLevelId": "5",
            "effectiveDate": "2026-10-13 10:00:00",
            "additionalParams": { "courseId": "<COURSE_ID>" }
          }
        }
      ]
    }
  }'
```

Point out that the response comes back immediately, while the certificate is created in the
background.

**Step 3: Check the status.**

```bash
curl -s -X POST 'http://localhost:7011/v1/certificate/competency/status' \
  -H 'Content-Type: application/json' \
  -d '{"request":{"filters":{"userId":"<USER_ID>","competencyId":"<COMPETENCY_ID>"}}}'
```

You should see `"status": "GENERATED"` with a `certificateUrl`.

**Step 4: Download the certificate.**

```bash
curl -s -X POST 'http://localhost:7011/v1/certificate/competency/download' \
  -H 'Content-Type: application/json' \
  -d '{"request":{"filters":{"userId":"<USER_ID>"}}}'
```

Open the `certificateUrl` in a browser to show the certificate.

**Step 5: Show it's issued only once.** Repeat Step 2. No second certificate is created.

---

## Key points to highlight

- **Automatic:** the certificate is issued the moment the last level is completed.
- **Non-blocking:** generation runs in the background; passbook updates are never slowed or affected.
- **Configurable per organisation:** on or off, which competencies, and which template, all from form
  configuration.
- **Issued once:** no duplicate certificates.
- **Self-healing:** a failed attempt is retried automatically on the next passbook update.