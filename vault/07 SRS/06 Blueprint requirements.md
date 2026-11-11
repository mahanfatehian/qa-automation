---
type: srs
epic: "[[02 Studio you can trust]]"
---

# 06 Blueprint requirements

Part of [[00 SRS index]]. Work lives in [[02 Studio you can trust]].

## FR-BP-01 Canvas

The studio shall show a dark dotted blueprint canvas and custom steps with a colored header by category: flow, browser, find/read, check.

Acceptance: a loaded demo is readable as a left-to-right blueprint, not as a generic form list.

## FR-BP-02 Execution pins

Each step shall expose only the execution pins defined for its type. Execution pins shall be white diamonds. Connecting an execution pin to a data pin shall be rejected in the studio and again on save.

Acceptance: a refused wire cannot be saved by reloading a hand-edited payload either. The API returns the pin error.

## FR-BP-03 Data pins

Element, text, and boolean pins shall be colored circles. A data pin shall connect only to the same kind. The runner shall pass element values only inside the run, never in the saved graph or the telemetry payload as a live element.

Acceptance: telemetry contains a text summary of an element, not a WebElement. Reload does not pretend the element still exists.

## FR-BP-04 One Start

A blueprint shall have exactly one Start step before it can be saved or run. A new blueprint shall be created with that Start step already on the canvas.

Acceptance: zero Start steps and two Start steps are both rejected with a clear error.

## FR-BP-05 Palette

The user shall add a step by dragging it from the palette onto the canvas. The new step shall have the default fields for its type and a unique id.

Acceptance: each final step type in [[02 Definitions]] can be dropped once its epic is done. Types not yet implemented shall not appear in the palette.

## FR-BP-06 Inspector

Selecting a step shall open an inspector with that type's fields in plain language. Changing a field shall mark the blueprint dirty.

Acceptance: URL, selector, timeout, condition, and check fields round-trip through save and reload.

## FR-BP-07 Save fidelity

Save shall persist nodes, edges, handle ids, positions, and field values. Reload shall restore them. Ephemeral run color shall not be stored.

Acceptance: save, refresh the browser, reopen the blueprint, and the wires still hit the same handles.

## FR-BP-08 Dirty guard

Switching blueprints or loading the demo over unsaved edits shall ask for confirmation.

Acceptance: cancel leaves the current graph untouched.

## FR-BP-09 Delete

The user shall delete the open blueprint. The system shall delete its schedules, runs, and step results.

Acceptance: the blueprint is gone from the list and from Postgres. The grid is unaffected.

## FR-BP-10 Demo

The studio shall load a demo blueprint: open `https://quotes.toscrape.com`, find the first quote, read it, branch if it contains `world`, and check that the text is not empty. The element pin and the text pins shall be connected.

Acceptance: Load demo, save, run. The graph matches [[Pins and steps]].

## FR-BP-11 Errors and keys

Validation errors shall be shown in the studio in readable text. Ctrl+S shall save. Ctrl+Enter shall run.

Acceptance: a bad pin error is visible without opening the browser console.

## FR-BP-12 Grid pill

The studio shall show whether the API and the grid are up, including Chrome node count.

Acceptance: stopping the hub changes the pill to down within one refresh interval.
