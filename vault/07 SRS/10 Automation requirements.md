---
type: srs
epic: "[[06 Automatic runs and CI]]"
---

# 10 Automation requirements

Part of [[00 SRS index]]. Work lives in [[06 Automatic runs and CI]].

Automatic runs attach to one blueprint. They are not a second product. See [[Automatic runs]].

## FR-AUTO-01 Manual

The studio shall run the open blueprint. If it is dirty, it shall save first. The response shall include the run id and status QUEUED.

Acceptance: Run blueprint creates a row and the runner picks it up.

## FR-AUTO-02 Cron

The user shall attach a 5-field UTC cron expression. Invalid cron shall be rejected. The schedule shall survive a control-plane restart.

Acceptance: `0 */6 * * *` is accepted. `not a cron` is rejected. A restart does not drop an enabled schedule.

## FR-AUTO-03 Interval

The user shall attach an interval in seconds, minimum 15. Shorter values shall be rejected.

Acceptance: 14 is rejected. 900 is accepted.

## FR-AUTO-04 Pause and delete

The user shall pause, resume, and delete a schedule. A paused schedule shall not fire.

Acceptance: after pause, no new run with that schedule's trigger appears for two intervals.

## FR-AUTO-05 Overlap

A cron or interval fire shall not enqueue if that blueprint already has a QUEUED or RUNNING run. Manual and webhook runs shall still enqueue.

Acceptance: a long run causes the next schedule tick to skip, and a webhook during that run still creates a second run.

## FR-AUTO-06 Webhook

Each blueprint shall have a webhook token. `POST /api/v1/webhooks/trigger/{id}?token=` shall enqueue a run. A missing or wrong token, including a different length, shall return 401 and shall not enqueue.

Acceptance: wrong token returns 401. Rotate token invalidates the old token.

## FR-AUTO-07 Wait

`wait=true` shall hold the webhook until PASSED, FAILED, or ERROR, or until timeout. PASSED returns 200. Any other terminal status returns 422. Timeout returns 504. Default timeout is 180 seconds. Maximum is 900.

Acceptance: a failing fixture returns 422 with the run id. A hung grid returns 504 rather than an open connection with no limit.

## FR-AUTO-08 JUnit

`GET /api/v1/runs/{id}/junit` shall return XML with one test case per step that produced a result, and a failure element for a failed step.

Acceptance: a failed Check appears as a failed testcase. The file is `application/xml`.

## FR-AUTO-09 History of schedules

The schedule modal shall show enabled state, expression or interval, and last fire time.

Acceptance: after one interval fire, last fire time is visible without reading the database.

## FR-AUTO-10 CI snippet

The README shall include a GitHub Actions example that calls the webhook with `wait=true` and fails the job on a non-200 response. The example shall state that the stack must already be running.

Acceptance: the snippet matches the real path and query parameters.
