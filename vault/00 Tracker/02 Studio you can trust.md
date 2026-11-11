# 02 Studio you can trust

Status: Partial

Spec: [[06 Blueprint requirements]]. Register: [[Requirement register]]. Order: step 2 in [[Development sequence]].

## Outcome

A QA engineer can create, edit, save, reload, run, and delete a blueprint without losing the graph or guessing whether a run is live.

## Why this is a big task

The canvas exists. The paths around it are thin: no run history, no delete, weak error display, and types that may not survive a real build. If save or reload lies, the rest of the product does not matter.

## Current evidence

- Palette, custom pins, inspector, schedule modal, and live drawer are in `frontend/src`.
- Save, run, load demo, and a selector probe button are wired.
- Past runs are not listed in the studio.
- A blueprint cannot be deleted from the studio.
- Screenshots that were too large for the socket are not loaded from the API.
- Dirty state does not warn before switching blueprints.

## Work

- [ ] Fix the studio so `npm run build` passes.
- [ ] Reload a saved blueprint and get the same nodes, edges, and pin handles.
- [ ] Warn before switching or reloading a dirty graph.
- [ ] Delete a blueprint from the studio, including its schedules and runs.
- [ ] Show API validation errors on the canvas, not only as a raw string.
- [ ] List past runs for the open blueprint.
- [ ] Open a past run and restore step color plus its screenshots from the API.
- [ ] Keep the socket alive for the run, and show disconnected if it drops.
- [ ] Empty state: a new blueprint starts with one Start step and a one-line pin explanation.
- [ ] Keyboard: Ctrl+S saves, Ctrl+Enter runs.
- [ ] Grid pill shows ready, node count, or down, and refreshes without a full page load.

## Done when

Create, edit, save, reload, run, inspect a past run, and delete all work from the studio with no manual database edits.

## Depends on

[[01 Prove the stack]]

## Not in this task

New step types. The picker. Auth.
