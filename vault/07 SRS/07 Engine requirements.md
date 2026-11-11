---
type: srs
epic: "[[03 Engine correctness]]"
---

# 07 Engine requirements

Part of [[00 SRS index]]. Work lives in [[03 Engine correctness]].

## FR-ENG-01 Job contract

A run shall enqueue a JSON job containing the run id, blueprint id, name, max steps, nodes, edges, and variables. The runner shall ignore studio-only fields.

Acceptance: a saved run still executes the graph as it was when queued, even if the user edits the blueprint afterward.

## FR-ENG-02 Walk

The runner shall start at the Start step and follow execution pins. If / else shall follow only `exec_true` or only `exec_false`. Repeat shall run `exec_loop_body` once per matched element, then `exec_completed`. Multiple outgoing execution pins of the same kind shall run in edge order, not in parallel, because one run has one browser.

Acceptance: a fixture with both branches proves the unused branch does not run.

## FR-ENG-03 Data into steps

Before a step runs, incoming data pins shall be resolved. Find shall put an element on `element_out`. Read shall put text on `text_out` and `extracted_value`. A later step's `element_in` or `text_in` shall receive that value.

Acceptance: the demo does not re-type the quote XPath into the Read step. The wire supplies the element.

## FR-ENG-04 Variables

Read shall save text into a named variable when asked. Type, Open page, Check, and If shall replace `{{name}}` from variables. `loop_index` and `loop_count` shall be set during Repeat.

Acceptance: a Type step can enter `{{quoteText}}` captured by an earlier Read.

## FR-ENG-05 Find strategies

Find shall support XPath, CSS, text contains, and regex over visible DOM text. It shall wait up to the step timeout. It shall not use a fixed sleep. Text search shall prefer the smallest matching element.

Acceptance: each strategy passes once against the fixture. A missing element fails that step with the strategy and the query in the message.

## FR-ENG-06 Click and type

Click shall use the element pin or an XPath. It shall scroll the target into view. If a normal click is intercepted, it shall retry with a JavaScript click when that option is on, and also when the browser reports the element is not interactable. Type shall clear first when asked, type the interpolated text, and press Enter when asked.

Acceptance: a covered button still activates when force JavaScript click is on.

## FR-ENG-07 Wait

Wait shall support a fixed delay up to 120 seconds, URL contains, and a document-settle heuristic. The fixed delay is the only intentional sleep. The network-settle mode shall be documented as a heuristic, not Chrome DevTools network idle.

Acceptance: URL contains passes on the fixture after navigation. A too-short timeout fails the Wait step.

## FR-ENG-08 Check

Check shall support not empty, equals, contains, and regex. Failure shall fail the run. The message shall include the operator, the expected value, and the actual value.

Acceptance: an empty read fails Check and the run status is FAILED.

## FR-ENG-09 Guards

A run shall stop at `maxSteps` (default 500, maximum 5000) if the graph cycles. The run shall be FAILED, not left RUNNING.

Acceptance: a two-step cycle stops with a max-step error.

## FR-ENG-10 Sessions

The runner shall open the browser from the Start step fields: browser, headless, width, height. It shall retry session creation while nodes register, then fail clearly. It shall set implicit wait to zero. It shall call `quit` on the driver after success, failure, and process shutdown of active sessions.

Acceptance: after a failed run, the grid session count returns to the prior idle count.

## FR-ENG-11 Concurrency

Each run shall execute on its own virtual thread and its own browser. One run shall not share a driver with another run.

Acceptance: two manual runs of a short blueprint can be queued together and both finish.

## FR-ENG-12 Loop freshness

Repeat shall re-find elements by index on each pass. It shall not reuse a stale element from the first query after the body changes the page.

Acceptance: a fixture that removes a row during the body still completes or fails with a clear index message, and does not throw an uncaught stale-element error.
