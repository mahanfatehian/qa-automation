---
type: srs
---

# 13 Interfaces and data

Part of [[00 SRS index]]. This is the contract between studio, control plane, and runner. Implementation epics shall not invent a second payload.

## Studio to control plane

| Method | Path | Requirement |
| --- | --- | --- |
| GET | `/health` | FR-SYS-03 |
| GET | `/api/v1/system/status` | FR-BP-12 |
| GET, POST | `/api/v1/workflows` | FR-SYS-06, FR-BP-04 |
| GET, PUT, DELETE | `/api/v1/workflows/{id}` | FR-BP-07, FR-BP-09 |
| POST | `/api/v1/workflows/{id}/run` | FR-AUTO-01 |
| POST | `/api/v1/workflows/{id}/webhook/rotate` | FR-AUTO-06 |
| GET, POST | `/api/v1/workflows/{id}/schedules` | FR-AUTO-02 |
| PATCH, DELETE | `/api/v1/schedules/{id}` | FR-AUTO-04 |
| POST | `/api/v1/webhooks/trigger/{id}` | FR-AUTO-06, FR-AUTO-07 |
| GET | `/api/v1/runs?workflow_id=` | FR-FB-06 |
| GET | `/api/v1/runs/{id}` | FR-FB-02 |
| GET | `/api/v1/runs/{id}/results` | FR-FB-03 |
| GET | `/api/v1/runs/{id}/results/{result_id}/screenshot` | FR-FB-03 |
| GET | `/api/v1/runs/{id}/junit` | FR-AUTO-08 |
| POST | `/api/v1/tools/probe` | FR-PICK-05 |
| POST | `/api/v1/tools/pick` | FR-PICK-02 |
| WS | `/ws/runs/{run_id}` | FR-FB-04, FR-FB-06 |

The studio shall use same-origin `/api` and `/ws`. Nginx in the studio image and the Vite dev server shall proxy those prefixes to the control plane.

## Control plane to runner

| Method | Path | Requirement |
| --- | --- | --- |
| POST | `/internal/probe` | FR-PICK-05 |
| POST | `/internal/pick` | FR-PICK-02 |
| GET | `/health` | FR-SYS-03 |

Jobs are not HTTP. The control plane shall `RPUSH` JSON to `queue:workflow_jobs`. The runner shall `BLPOP` that list. Feedback shall be published on `telemetry:workflow_events` and translated to snake_case before it is stored or sent to the studio.

## Job fields

`runId`, `workflowId`, `workflowName`, `maxSteps`, `nodes`, `edges`, `variables`.

Node fields used by the runner: `id`, `type` or `data.nodeType`, `data`.

Edge fields: `source`, `target`, `sourceHandle`, `targetHandle`. Missing handles mean `exec_out` and `exec_in`.

## Type ids

| Studio name | Type id |
| --- | --- |
| Start browser | `START` |
| Open page | `NAVIGATE` |
| Find element | `SEARCH_ELEMENT` |
| Click | `CLICK` |
| Type | `TYPE_TEXT` |
| Read text | `EXTRACT_CONTENT` |
| If / else | `IF_CONDITION` |
| Repeat for each | `LOOP_ELEMENTS` |
| Wait | `WAIT` |
| Check | `ASSERT` |
| Scroll | `SCROLL` |
| Hover | `HOVER` |
| Select | `SELECT_OPTION` |
| Press key | `PRESS_KEY` |
| Enter frame | `ENTER_FRAME` |
| Leave frame | `LEAVE_FRAME` |
| Switch tab | `SWITCH_TAB` |
| Alert | `ALERT` |
| Upload | `UPLOAD` |
| Go back | `GO_BACK` |

## Stored run

`id`, `workflow_id`, `status`, `trigger`, `error_message`, `started_at`, `finished_at`, `created_at`.

## Stored step result

`id`, `run_id`, `node_id`, `node_type`, `status`, `duration_ms`, `message`, `error_message`, `output_data`, `screenshot_base64`, `sequence`, `created_at`.

Screenshot bytes are not part of the list schema. See FR-FB-03.
