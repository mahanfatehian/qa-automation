---
type: sequence
---

# Development sequence

Part of [[00 SRS index]]. This is the only build order for the final product. Finish the exit check before starting the next step. Do not pull a later step forward because it is more interesting.

The work notes stay in `00 Tracker`. This note says when those notes may be touched.

## Step 1 — Boot

Epic: [[01 Prove the stack]]

Requirements: FR-SYS-01 through FR-SYS-08, NFR-01, NFR-02.

Exit: the command and the demo result are written at the bottom of that epic note. A failure that is a product bug is fixed here. A dead public site is replaced by the fixture only after the fixture exists in Step 3. Until then, record the site failure and keep going if the studio and grid themselves came up.

## Step 2 — Studio trust

Epic: [[02 Studio you can trust]]

Requirements: FR-BP-01 through FR-BP-12.

Exit: create, edit, save, reload, run, reopen a run, and delete, all from the studio.

## Step 3 — Engine and fixture

Epic: [[03 Engine correctness]]

Requirements: FR-ENG-01 through FR-ENG-12, NFR-03, NFR-05.

Exit: the fixture blueprint passes, a bad selector fails one step, and the grid session is gone.

## Step 4 — Feedback

Epic: [[07 Feedback pack]]

Requirements: FR-FB-01 through FR-FB-08.

Exit: a failed run and a reopened run both show URL, error, and screenshot.

## Step 5 — Picker

Epic: [[04 Element picker]]

Requirements: FR-PICK-01 through FR-PICK-07.

Exit: a click fills a Find step and that step passes.

## Step 6 — Actions

Epic: [[05 Browser actions]]

Requirements: FR-ACT-01, FR-ACT-02, FR-ACT-03, then FR-ACT-04 through FR-ACT-10.

Exit: Scroll, Hover, and Select pass on the fixture. Other actions are passed or explicitly deferred in the epic note and in [[15 Final acceptance]].

## Step 7 — Unattended runs

Epic: [[06 Automatic runs and CI]]

Requirements: FR-AUTO-01 through FR-AUTO-10.

Exit: one schedule fire and one webhook are visible in the studio with the same feedback as a manual run.

## Step 8 — Hardening

Epic: [[08 Public hardening]]

Requirements: FR-SEC-01 through FR-SEC-06, NFR-04, NFR-06.

Exit: host tests pass, the token bug is gone, and the README states the local-only rule. The off-by-default gate exists if the note says a non-local deploy is supported.

## Step 9 — Gate

Epic: [[09 Release gate]]

Requirements: [[15 Final acceptance]].

Exit: every box in that epic is checked, and [[Requirement register]] has no Must row left in Not started or Partial.

## Rules while stepping

- One step is in progress at a time.
- A checkbox is checked only after the acceptance line is shown, not after the file is saved.
- If a step needs a requirement change, edit the SRS note and the register in the same change. Do not silently shrink the final product.
- Stay on the blueprint. If a change adds a non-browser node, stop and read [[14 Out of scope]].
