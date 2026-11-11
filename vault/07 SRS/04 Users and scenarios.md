---
type: srs
---

# 04 Users and scenarios

Part of [[00 SRS index]]. Implements the stories behind [[Requirement register]].

## Users

| User | Needs |
| --- | --- |
| QA engineer | Build and run a browser test without code. See why it failed. |
| Developer using CI | Trigger a saved blueprint and fail the pipeline if it fails. |
| Owner of the repo | Start the whole stack locally and know it is not safe to expose raw. |

There is no separate admin product in the final local release. The gate in [[12 Quality and security]] exists only before a non-local deploy.

## UC-01 Clone and start

1. Clone the repo.
2. Run `up.bat` or `docker compose up --build -d --scale chrome-node=2`.
3. Open the studio, the API docs, and the grid console.
4. The quotes blueprint is already there.

Alternate: Docker is not running. The command fails with Docker's own error. The app does not pretend to start.

Traces to [[05 System requirements]].

## UC-02 Build a test without code

1. Create a blueprint. It starts with one Start step.
2. Drag Open page, Find, Click or Read, and Check.
3. Connect white pins left to right.
4. Connect the green element pin from Find into Click or Read.
5. Save. Reload. The graph is the same.
6. Run.

Alternate: the user connects a white pin to a green pin. The studio refuses the wire. Save also refuses it if it is forced in.

Traces to [[06 Blueprint requirements]] and [[07 Engine requirements]].

## UC-03 Pick an element

1. The blueprint has an Open page URL.
2. On Find, Click, Type, or Read, the user chooses Pick element.
3. The page opens on the grid. Boxes appear on a screenshot.
4. The user clicks the quote text.
5. A ranked selector is written into that step.
6. The user can still type a selector and probe it.

Alternate: the page does not load. The picker shows the error and writes nothing.

Traces to [[08 Picker requirements]].

## UC-04 Read a failure

1. A selector is wrong.
2. The run stops on that step.
3. The step is red. The drawer shows the error, the URL, and the screenshot.
4. Later, the same run can be reopened with the same evidence.
5. The browser session is gone from the grid.

Traces to [[11 Feedback requirements]] and [[07 Engine requirements]].

## UC-05 Run it later

1. The user adds a cron or an interval.
2. A run appears in history without anyone pressing Run.
3. Pausing the schedule stops new fires.
4. A CI job calls the webhook with `wait=true` and fails if the blueprint fails.
5. JUnit XML is available for that run.

Traces to [[10 Automation requirements]].

## UC-06 Use a real page action

1. The user adds Scroll, Hover, or Select where the page needs it.
2. The step uses the same pins and the same failure screenshot as Click.
3. A local fixture proves the step without a public site.

Traces to [[09 Action requirements]].
