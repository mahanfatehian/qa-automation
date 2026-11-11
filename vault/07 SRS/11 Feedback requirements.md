---
type: srs
epic: "[[07 Feedback pack]]"
---

# 11 Feedback requirements

Part of [[00 SRS index]]. Work lives in [[07 Feedback pack]].

## FR-FB-01 Step events

The runner shall emit, for each step, running, then passed or failed. Each event shall include run id, step id, step type, time, duration when finished, a short message, and error text on failure.

Acceptance: the studio timeline shows every step of the demo in order.

## FR-FB-02 Run events

The runner shall emit run started, then run completed or run failed. If the browser cannot open, the run shall end FAILED or ERROR. It shall not stay QUEUED.

Acceptance: stopping every Chrome node, then running, ends in a terminal status with a grid message.

## FR-FB-03 Screenshot

The runner shall capture a PNG screenshot after each finished step and on run failure when a browser exists. The control plane shall store it and serve it as `image/png`. List endpoints shall not embed the image bytes. They shall say whether a screenshot exists.

Acceptance: a history call is small. Opening one step loads one image.

## FR-FB-04 Live color

While a run is streaming, the studio shall color the step yellow for running, green for passed, and red for failed.

Acceptance: a slow Wait step is yellow before it turns green.

## FR-FB-05 Drawer

The drawer shall show the step message, duration, output data, error, current URL, and screenshot. If the socket omits a large screenshot, the studio shall load it from the API.

Acceptance: a failed step shows the URL and the image without a second run.

## FR-FB-06 Replay

Connecting to the run socket shall replay stored step events. Reopening a past run from the studio shall show the same steps and screenshots.

Acceptance: refresh during a finished run still shows the timeline.

## FR-FB-07 Output safety

Telemetry and stored output shall include strings, numbers, and booleans. Element values shall be summarized as tag and truncated text. Strings in telemetry shall be truncated. The in-run variable shall still hold the full text for later steps.

Acceptance: a long Read does not put a WebElement in Postgres, and a later Check still sees the full text in that same run.

## FR-FB-08 Retention

The product shall document how screenshots are kept. Before release, a keep limit shall be chosen and implemented, or the README shall state that local volumes grow until `make clean`.

Acceptance: the chosen behavior is written in the README and matches the code.
