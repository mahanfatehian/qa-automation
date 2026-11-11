# 06 Automatic runs and CI

Status: Partial

Spec: [[10 Automation requirements]]. Register: [[Requirement register]]. Order: step 7 in [[Development sequence]].

## Outcome

The same blueprint that runs from the studio also runs on a clock and from a pipeline, and the pipeline gets a pass or fail it can trust.

## Why this is a big task

Unattended runs were the only idea taken from generic automation tools. The scheduler and webhook exist in code. They have not been proven, and the studio does not show whether a scheduled run happened.

## Current evidence

- Cron and interval schedules persist in Postgres and are restored on startup.
- A scheduled run is skipped if that blueprint is already queued or running.
- Webhook: `POST /api/v1/webhooks/trigger/{id}?token=...&wait=true`
- JUnit XML: `GET /api/v1/runs/{id}/junit`
- The schedule modal can add, pause, and delete a schedule and copy a curl command.

## Work

- [ ] Create a 15-minute interval in the studio, wait for one fire, and see the run in history.
- [ ] Pause it and prove it does not fire.
- [ ] Call the webhook with a bad token and get 401.
- [ ] Call the webhook with `wait=true` and get 200 on pass or 422 on fail.
- [ ] Confirm a scheduled run does not pile up behind a long run.
- [ ] Show last scheduled run time in the modal.
- [ ] Add a sample GitHub Actions snippet to the README that calls the webhook and fails the job on 422.
- [ ] Document that the stack must already be running. This epic does not host a cloud runner.

## Done when

One blueprint is fired by the scheduler and by a curl webhook, and both results show up in the studio with the same step feedback as a manual run.

## Depends on

[[01 Prove the stack]] and [[07 Feedback pack]] for the result to be readable.

## Not in this task

A hosted multi-tenant scheduler. Per-user billing. A second canvas.
