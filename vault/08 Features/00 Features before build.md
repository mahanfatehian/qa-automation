---
type: feature-brief
status: spec-before-build
---

# Features before build

This note freezes the features asked for in conversation, before more implementation. If a later coding session adds a feature that is not here, add it here first.

Priority over every feature: [[Speed and feel]]. A correct feature that feels slow or nagging is not done.

## Product shape

- One blueprint canvas. White pins order steps. Colored pins pass an element, text, or boolean.
- Not a general automation builder. See [[Blueprint not n8n]] and [[Direction correction]].
- The scenario shape is Given, When, Then. Cucumber is not a dependency. See [[Given When Then]].
- Steps must cover what a QA automation test actually needs, not only a click procedure. See [[QA node taxonomy]].

## Feature list

| Feature | What was asked | Status | Spec |
| --- | --- | --- | --- |
| One-command local start | Clone and run with no required `.env` | Partial, booted once | [[05 System requirements]] |
| Selenium Grid behind the runner | Chrome cluster, Java drives the browser | Partial | [[05 System requirements]] |
| Blueprint studio | Drag steps, connect pins, save, run | Partial | [[06 Blueprint requirements]] |
| Given / When / Then palette | Procedure shape without Cucumber | In the studio | [[Given When Then]] |
| Search without only XPath | CSS, id, name, link text, tag, text, regex | Partial in the runner | [[QA node taxonomy]] |
| Inputs | Type, dropdown, checkbox, keys, hover, scroll, upload, click variants | Partial | [[09 Action requirements]] |
| Verify and Check | Text, visible, enabled, selected, attribute, count, URL, title | Partial | [[QA node taxonomy]] |
| Automatic runs | Cron, interval, webhook, JUnit | Partial, webhook proven on the fixture | [[10 Automation requirements]] |
| Examples | One run per row, `{{column}}` | Coded, not proven on the running stack | [[Given When Then]] |
| Step feedback | Status, duration, error, URL, screenshot, history | Partial | [[11 Feedback requirements]] |
| Element selection engine | DevTools-style cursor pick on the page as it is now | Specified, not built | [[Element selection engine]] |
| Dogfood tests | Playwright drives the studio | 3 tests passed once | `e2e/` |
| Speed and feel | Fast, non-annoying, always | This is the constraint on all rows | [[Speed and feel]] |
| Locator healing | A database that fixes broken selectors over time | Rejected. See the decision. | [[Healenium decision]] |

## Element selection engine, in one paragraph

The test maker must not have to invent an XPath. A selection action opens the page in the state the blueprint has already reached, the user points at an element the way browser DevTools does, and the product shows what that element is: text, input, password, checkbox, radio, select with its options, button, or link. From that, the user adds the next When or Then step with the selector already filled. Full rules are in [[Element selection engine]].

## Do not build next until this note is read

- A Cucumber runtime, `.feature` files, or step-definition classes.
- A second editor beside the blueprint.
- A picker that only works on the first URL and ignores steps already run.
- A picker that screenshots on every mouse move.
- Any feature that blocks the canvas while the grid is working.
