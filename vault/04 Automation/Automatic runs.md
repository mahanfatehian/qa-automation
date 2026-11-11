# Automatic runs

This is the part inspired by n8n. It attaches to a blueprint. It is not a second product.

## Triggers

- **Manual:** Run blueprint in the studio. `POST /api/v1/workflows/{id}/run`
- **Cron:** 5-field UTC cron, for example `0 */6 * * *`
- **Interval:** seconds, minimum 15
- **Webhook:** `POST /api/v1/webhooks/trigger/{id}?token={webhook_token}`

`wait=true` holds the HTTP call until the blueprint passes or fails. CI should use that. Non-pass returns 422. Timeout returns 504.

JUnit for the same run: `GET /api/v1/runs/{id}/junit`

## Rules already implemented

- Schedules live in Postgres and are restored when the control plane starts.
- APScheduler fires them.
- Cron and interval skip if that blueprint already has a queued or running job.
- Manual and webhook always enqueue, so a person or a pipeline is not blocked by a schedule.
- Each blueprint has its own webhook token. Rotate with `POST /api/v1/workflows/{id}/webhook/rotate`.

## Later, still about runs, not a new canvas

- quiet hours / concurrency cap per blueprint
- retry the whole blueprint, not hidden retries inside every step
- keep last N artifacts and delete old screenshots
- a status badge in GitHub from the webhook response

See [[Step feedback]] for what a finished run must show.
