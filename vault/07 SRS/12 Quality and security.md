---
type: srs
epic: "[[08 Public hardening]]"
---

# 12 Quality and security

Part of [[00 SRS index]]. Work lives in [[08 Public hardening]]. Quality items also bind [[01 Prove the stack]] and [[03 Engine correctness]].

## NFR-01 Startup time

A second start with images already built shall reach a healthy studio without manual repairs. The first start may spend its time on image builds. That cost shall be stated in the README.

Acceptance: two consecutive `up` commands, the second without a rebuild, serve the studio.

## NFR-02 Windows path

`up.bat` and `down.bat` shall be the documented Windows commands. They shall not require GNU make.

Acceptance: a clean `up.bat` from the repo root is enough on the owner machine.

## NFR-03 Isolation

One run's browser shall not see another run's cookies or tabs.

Acceptance: two overlapping runs navigate to different fixture pages and each Read returns its own page text.

## NFR-04 Failure clarity

User-facing errors shall name the missing thing: URL, selector, grid, token, or pin type. They shall not say only "null" or "internal server error" for those cases.

Acceptance: the four cases above each produce a specific message in the studio or HTTP body.

## NFR-05 Fixture

The repo shall include a local HTML fixture and a blueprint that covers branch, repeat, check, scroll, hover, and select. Grid proof for those steps shall use the fixture so a public site outage does not block the release gate.

Acceptance: the fixture is served from the stack or opened as a file URL the grid can reach. The proof blueprint is loadable from the studio.

## FR-SEC-01 Local defaults

Default database credentials shall be for local use only. The README shall say not to publish the compose ports to the internet as shipped.

Acceptance: the warning is in the README beside the start command.

## FR-SEC-02 Non-local gate

Before any deploy that is not localhost, the system shall require a single admin token or a real login on the API and the studio. Webhook tokens alone shall not be treated as a full gate. Multi-tenant accounts are not required for the final local product.

Acceptance: with the gate enabled, an unauthenticated `GET /api/v1/workflows` is rejected. Local `up.bat` without the gate still works.

## FR-SEC-03 Internal endpoints

The runner's probe and picker endpoints shall not be part of the public studio origin except through the control plane. The control plane shall be the only caller documented for those routes.

Acceptance: the studio calls `/api/v1/tools/...`, not port 8081, for picker and probe.

## FR-SEC-04 Token compare

Webhook token checks shall not throw on length mismatch and shall not enqueue on mismatch.

Acceptance: a one-character token returns 401.

## FR-SEC-05 Migrations

After the first proven boot, schema changes shall ship with a migration. `create_all` may remain for an empty database. A model change shall not require a silent volume wipe without a note in the release.

Acceptance: a documented upgrade path exists before the second schema change.

## FR-SEC-06 Tests without a grid

The repo shall include tests for graph validation, pin compatibility, and webhook rejection that run without Docker.

Acceptance: one command runs those tests on the host Python.

## NFR-06 Logs and secrets

The services shall not log webhook tokens or database passwords.

Acceptance: a webhook run's control-plane log does not contain the token.
