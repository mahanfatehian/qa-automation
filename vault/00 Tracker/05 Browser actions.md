# 05 Browser actions

Status: Not started

Spec: [[09 Action requirements]]. Register: [[Requirement register]]. Order: step 6 in [[Development sequence]].

## Outcome

The common browser moves that a QA test needs are steps on the same blueprint, with the same pins and the same failure feedback.

## Why this is a big task

The current catalog can open, find, click, type, read, branch, and check. Real pages also need scroll, hover, dropdowns, keys, frames, tabs, alerts, and uploads. Each new step must be a handler, a catalog entry, an inspector form, and a validation type. No step is done because the palette shows a name.

## Order inside this epic

1. Scroll
2. Hover
3. Select option
4. Press key
5. Switch frame and leave frame
6. Switch tab
7. Accept or dismiss alert
8. Upload file
9. Go back

## Work

- [ ] Scroll: by pixels, to an element pin, to top, to bottom. Execution pins only, plus an optional element pin.
- [ ] Hover: element pin or selector.
- [ ] Select option: element pin plus visible text or value.
- [ ] Press key: key name, optional element pin.
- [ ] Frame: enter by selector, and a step that returns to the top document.
- [ ] Tab: switch by index or URL fragment, then continue on that tab.
- [ ] Alert: accept or dismiss, optional text out for the alert message.
- [ ] Upload: file path mounted into the runner, sent to a file input. Document the Docker path limit.
- [ ] Go back.
- [ ] Each new type is in Python validation, the Java registry, and the studio catalog.
- [ ] Each new type has one grid run against the local fixture that proves pass and fail.

## Done when

A fixture blueprint uses Scroll, Hover, and Select, and a failed selector still fails only that step with a screenshot.

## Depends on

[[03 Engine correctness]]

## Not in this task

Slack, email, generic HTTP nodes, or a code step as the normal path.
