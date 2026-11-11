# 07 Feedback pack

Status: Partial

Spec: [[11 Feedback requirements]]. Register: [[Requirement register]]. Order: step 4 in [[Development sequence]].

## Outcome

After every run, a person can see which step ran, how long it took, what it read, why it failed, and what the page looked like.

## Why this is a big task

A green or red job is not enough for QA. The live drawer exists. History, omitted screenshots, and failure context are not finished. Without this, scheduled runs are opaque.

## Current evidence

- The runner publishes running, passed, and failed per step.
- Screenshots are stored and can be fetched by result id.
- The studio paints the live node and shows the latest socket screenshot.
- JUnit XML exists.
- Large screenshots can be dropped from the socket.
- A failed step now stores the page URL and the selector in its output. The drawer shows both.
- Past runs reopen from the earlier-runs list. Clicking a step loads that step's screenshot from the API.

## Work

- [x] Every failed step stores message, URL, and screenshot.
- [x] The drawer shows URL and the selector or text that was used.
- [x] If the socket omits a screenshot, the studio loads it from the API.
- [x] Reopening a past run shows the same step list and screenshots.
- [ ] JUnit includes one case per step and a failure body for the failed step.
- [ ] A run that never got a browser still becomes FAILED or ERROR, not stuck QUEUED.
- [ ] Old screenshots: decide a keep limit and document it. Do not delete blindly in this epic unless the limit is implemented.

## Done when

A failed demo, a passed demo, and a reopened past run all show the failing or passing step, its text, and its screenshot without reading the database.

## Depends on

[[01 Prove the stack]]

## Not in this task

Video recording. Pixel diff against the last pass. Those can be notes under this epic after the still-screenshot path works.
