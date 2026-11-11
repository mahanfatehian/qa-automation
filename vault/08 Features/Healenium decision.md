---
type: decision
status: accepted
---

# Healenium decision

Asked as "helenium database" that makes element selection better over time. The product that matches that description is **Healenium**, not Helium. Helium is a thinner Selenium wrapper and has no learning database.

## What Healenium actually does

It wraps Selenium. When a saved locator fails, its backend searches the DOM for a similar element and stores the healed locator in Postgres. The database gets better after many runs. It does not open the page for a cursor pick, and it does not tell the author that the control is a select, a text input, or a checkbox.

## Decision

Do not add Healenium.

Build [[Element selection engine]] ourselves. The problem we have now is authoring: the person cannot find the element. Healenium's problem is maintenance: a locator that used to work broke later. Those are different.

## Why not

- It sits on the find path. Every lookup can pay for a proxy and a heal. That fights [[Speed and feel]].
- It is another service and another database beside the grid, the runner, and our Postgres.
- It does not produce the DevTools cursor flow or the control-kind parse.
- Adopting it would not remove the need to build selection.

## What we keep from the idea

After selection exists and tests are actually running, store successful finds in our own Postgres: page URL, kind, visible text, and the selector that worked. If a later run misses, suggest the stored selector. Do that only after a miss, not on every find. No extra service.

Revisit Healenium only if that small history is not enough and broken locators are the main support cost. Until then it stays out.
