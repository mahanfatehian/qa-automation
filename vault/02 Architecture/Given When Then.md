---
type: architecture
---

# Given When Then

Cucumber is not part of this product. The blueprint is the step language. Cucumber's useful part is the procedure shape of a QA test: setup, action, check. That shape is now the palette.

## What we took

A Selenium BDD scenario is three clauses, then wires:

- **Given** the browser, the page, a cookie, a frame, or a wait. The world is ready.
- **When** the user clicks, types, selects, checks, hovers, scrolls, uploads, or navigates.
- **Then** the page is checked: text, visibility, enabled, selected, attribute, count, URL, title.

`And` is not a new kind of step. It is another white pin of the same clause, or a branch (`If / else`, `Repeat`).

The node handlers are the step definitions. The canvas is the feature file. A run is one scenario.

## What we did not take

- No `cucumber-java` dependency.
- No `.feature` files as the source of truth. The saved blueprint JSON is the scenario.
- No separate step-definition classes. Adding a QA action means a blueprint step, not an annotation.
- Scenario Outline is the Examples button. Each row queues one run and fills `{{column}}`. It is still the same blueprint, not a Cucumber table.

## How a scenario reads on the canvas

1. Given Start browser, then Given Open page.
2. When Find element, wire the green pin into When Click or When Type.
3. Then Verify visible, or Then Check the text.
4. A failed Then stops the scenario and keeps the screenshot. That is the assertion, not a log line.

See [[QA node taxonomy]] for the full list of inputs, searches, and checks this shape must hold.
