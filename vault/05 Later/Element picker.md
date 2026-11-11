# Later - element picker

A typed probe and a click-on-screenshot pick exist. The engine that still has to be built is [[Element selection engine]]: cursor selection on the page as the blueprint left it, plus a parse of what the control is. It must follow [[Speed and feel]].

## Why

Typing XPath is the slow part. QA should click the thing on the page and get a selector back into the selected step.

## First slice already in the app

`POST /api/v1/tools/probe` opens a grid session, opens a URL, tries a selector, and returns a count, a text sample, and a screenshot. The inspector button "Probe this selector" uses that. It does not pick by click.

## Next engine

1. Open a live grid session for the blueprint's Start browser.
2. Show the page (noVNC on the node, or a screenshot stream).
3. User clicks a point.
4. Runner returns a ranked selector list, not one brittle XPath:
   - `data-testid` / `data-test`
   - stable `id`
   - name / aria-label
   - short CSS
   - XPath last
5. Write the chosen selector into the selected Find, Click, Type, or Read step.

Do not add a second editor for this. The click result lands in the existing step fields.

## Do not copy

Selenium IDE and Playwright codegen are references for the picker and a later recorder. They are not a reason to replace the blueprint with a linear script.
