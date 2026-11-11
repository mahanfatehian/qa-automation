# 03 Engine correctness

Status: Partial

Spec: [[07 Engine requirements]]. Register: [[Requirement register]]. Order: step 3 in [[Development sequence]].

## Outcome

The runner walks a blueprint the way the pins say, fails on the step that broke, always closes the browser, and does not lose an element or a variable between steps.

## Why this is a big task

The handlers exist. Correctness is the product. A wrong branch, a leaked grid session, or a stale loop element makes every later feature untrustworthy.

## Current evidence

Coded steps: Start, Open page, Find, Click, Type, Read, If / else, Repeat for each, Wait, Check.

Known risks, not yet proven:

- `secrets.compare_digest` can throw when the webhook token length differs.
- Loop body failures also mark the loop step failed.
- Data pins are in-memory only. A reload of the page must not be expected to keep an element pin.
- `{{variable}}` works only for names already saved by Read or the loop index.
- Max step guard exists. It has not been tested with a cycle.

## Work

- [ ] One Start step is required before a run. Save rejects a second Start.
- [ ] Execution pins and data pins cannot be mixed. Prove with a bad save.
- [ ] If / else follows only the true pin or only the false pin.
- [ ] Repeat for each runs the body once per match, then the done pin, and re-finds by index.
- [ ] A failed step stops the blueprint, stores the error, and still quits the driver.
- [ ] Grid session creation retries when nodes are still registering, then fails with a clear message.
- [ ] Click scrolls the target into view before clicking.
- [ ] Read can save a variable, and a later Type or Check can use `{{name}}`.
- [ ] Webhook token mismatch returns 401, including a wrong-length token.
- [ ] A cycle stops at `maxSteps` and the run is failed, not stuck.
- [ ] Add a small local HTML fixture and a blueprint that covers branch, loop, and check. Run it on the grid.

## Done when

The fixture blueprint passes, a forced bad selector fails on that step only, and the grid shows no leftover session after the run.

## Depends on

[[01 Prove the stack]]

## Not in this task

Picker, scroll-as-its-own-step, auth.
