---
type: srs
epic: "[[04 Element picker]]"
---

# 08 Picker requirements

Part of [[00 SRS index]]. Work lives in [[04 Element picker]].

The picker fills an existing step. It does not add a second canvas and it does not record a whole test in this release. A recorder may be added later only as a new requirement.

## FR-PICK-01 Entry

Find, Click, Type, and Read shall offer Pick element. The picker shall use the Open page URL already on the blueprint. If that URL is missing, it shall say so and shall not open a browser.

Acceptance: the button is absent on Start, If, Wait, and Check.

## FR-PICK-02 Session

The runner shall open a short-lived grid session, navigate to the URL, and collect visible interactive elements plus a screenshot. The session shall be closed before the response returns.

Acceptance: a picker call does not leave a session on the hub.

## FR-PICK-03 Ranked selectors

For each element the runner shall return tag, visible text, bounding box, and selectors in this order: `data-testid` or `data-test`, stable id, name or aria-label, a short CSS path, XPath last. It shall not return a selector it did not derive from the page.

Acceptance: an element with `data-testid` offers that selector first. An element with none of the preferred hooks still offers CSS and XPath.

## FR-PICK-04 Choose and write

The studio shall show the screenshot with boxes. Choosing a box and a selector shall write that selector into the selected step's query or XPath field and mark the blueprint dirty. It shall not auto-save unless the user saves or runs.

Acceptance: after save and reload, the chosen selector is still in the step.

## FR-PICK-05 Probe remains

The user shall still be able to type a selector and probe it. Probe shall return match count, a text sample, an error if any, and a screenshot.

Acceptance: a bad XPath returns ok false and does not change the step.

## FR-PICK-06 Failure

If navigation or the grid fails, the picker shall return the error and write no selector.

Acceptance: with the hub stopped, the studio shows a grid error and the step fields are unchanged.

## FR-PICK-07 Proof

On the quotes page, picking the first quote text and running the blueprint shall pass the Find step that received that selector.

Acceptance: recorded once in [[04 Element picker]] when that epic is marked Done.
