---
type: srs
epic: "[[05 Browser actions]]"
---

# 09 Action requirements

Part of [[00 SRS index]]. Work lives in [[05 Browser actions]].

Every action below is a blueprint step. Each shall have execution pins, an inspector form, a Java handler, a validation entry, a pass on the fixture, and a fail that stops only that step with a screenshot. A step is not done when it only appears in the palette.

Click already scrolls its target into view. That does not satisfy Scroll.

## FR-ACT-01 Scroll

Type id `SCROLL`. Modes: pixels with direction, to an element pin, top, bottom. Pins: `exec_in`, `exec_out`, optional `element_in`.

Acceptance: the fixture's lower button is clicked after a scroll-to-element step.

## FR-ACT-02 Hover

Type id `HOVER`. Element pin or selector. Pins: `exec_in`, `exec_out`, optional `element_in`.

Acceptance: a CSS hover target becomes visible and the next Check passes.

## FR-ACT-03 Select

Type id `SELECT_OPTION`. Chooses an option by visible text or value. Pins: `exec_in`, `exec_out`, optional `element_in`.

Acceptance: the fixture select changes and Read returns the chosen text.

## FR-ACT-04 Press key

Type id `PRESS_KEY`. Sends a named key, optionally to an element. Pins: `exec_in`, `exec_out`, optional `element_in`.

Acceptance: Enter in a field submits the fixture form.

## FR-ACT-05 Frame

Type ids `ENTER_FRAME` and `LEAVE_FRAME`. Enter uses a selector. Leave returns to the top document. Pins: `exec_in`, `exec_out`.

Acceptance: a Click inside the frame hits the framed button, and a later step on the top page still works after Leave.

## FR-ACT-06 Tab

Type id `SWITCH_TAB`. Switches by index or URL fragment. Pins: `exec_in`, `exec_out`.

Acceptance: a blueprint opens a second tab and reads text from it.

## FR-ACT-07 Alert

Type id `ALERT`. Accept or dismiss. Optional text out for the alert message. Pins: `exec_in`, `exec_out`, `text_out`.

Acceptance: accept continues the fixture. The alert text is available on the text pin.

## FR-ACT-08 Upload

Type id `UPLOAD`. Sends a file path that exists inside the runner container to a file input. The README shall state that the file must be mounted into the runner. Pins: `exec_in`, `exec_out`, optional `element_in`.

Acceptance: the fixture shows the chosen file name. A missing path fails the step.

## FR-ACT-09 Go back

Type id `GO_BACK`. Browser back, then wait for the document to be complete. Pins: `exec_in`, `exec_out`.

Acceptance: URL returns to the previous fixture page.

## FR-ACT-10 Catalog gate

The palette shall show an action only after its handler and validation exist. Unknown type ids shall be rejected on save.

Acceptance: removing a handler type from the allowed set makes save fail with the type name.
