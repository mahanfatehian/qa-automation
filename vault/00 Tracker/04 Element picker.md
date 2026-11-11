# 04 Element picker

Status: Not started

Spec: [[08 Picker requirements]]. Register: [[Requirement register]]. Order: step 5 in [[Development sequence]].

## Outcome

From a Find, Click, Type, or Read step, the user opens the page, clicks the thing they mean, and the step receives a selector. They do not start by writing XPath.

## Why this is a big task

This is the difference between a node editor and a tool QA will use. The blueprint does not change. The picker only fills the existing step fields.

## Current evidence

`POST /api/v1/tools/probe` opens a URL and tests a selector the user already typed. The inspector has a probe button. There is no click target and no ranked selector.

## Work

- [ ] Add a picker session on the runner: open the Start browser, go to the Open page URL, collect clickable elements.
- [ ] For each element return tag, visible text, box, and a ranked selector list: `data-testid`, stable id, name or aria-label, short CSS, XPath last.
- [ ] Return a screenshot the studio can draw boxes on.
- [ ] Studio: "Pick element" on Find, Click, Type, and Read. Click a box, choose a selector, write it into that step.
- [ ] If the page needs a login first, the picker can start from the current blueprint URL only. Do not build a recorder in this epic.
- [ ] Picker failure shows the grid error. It does not invent a selector.
- [ ] Keep probe-by-typed-selector. The picker does not replace it.

## Done when

On the quotes demo page, a click on the first quote fills the Find step, and a run using that filled step passes.

## Depends on

[[01 Prove the stack]] and a studio that can save the filled step from [[02 Studio you can trust]].

## Not in this task

A Chrome extension. A second editor. A general recorder. Those can be a later epic only after this one works.
