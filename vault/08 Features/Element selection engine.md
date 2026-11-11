---
type: feature-spec
status: not-built
priority: high
---

# Element selection engine

Specified before implementation. This replaces the idea of "type an XPath and hope." A basic click-on-screenshot pick already exists. This note is the engine that still has to be built.

Parent: [[00 Features before build]]. Constraint: [[Speed and feel]]. Locator healing is [[Healenium decision]]: not used.

## Problem

People stall when they must write or debug an XPath. They also stall when the picker opens the start URL instead of the page as the test has left it: after login, after a menu opens, after a scroll. The selection has to happen on that context, with the cursor, and it has to say what the element is so the next step is obvious.

## User flow

1. The user is on a When or Then step, or about to add one.
2. They choose Select on page.
3. The runner opens the Start browser and runs the blueprint steps before this step. That is the context. It does not run the step being filled.
4. The studio shows the current viewport. The user moves the cursor over that picture.
5. The element under the cursor highlights, the way DevTools element selection does.
6. The side panel shows the element, not a raw DOM dump:
   - kind: text, link, button, text input, password, checkbox, radio, select, textarea, file, image, other
   - visible text, placeholder, name, id, aria-label, test id
   - if it is a select: the options, text and value
   - a short ranked selector list, best first
7. The user locks the element with a click.
8. The panel offers the next steps that match the kind, already filled:
   - select: Select
   - checkbox or radio: Checkbox
   - text input, password, textarea: Type
   - button or link: Click
   - any kind: Verify visible, and Find element
9. Use on this step writes the selector into the open step. Add next step inserts a new step to the right and connects the white pin.
10. Close quits the browser immediately.

## Context rule

If the blueprint has already opened a page, typed, or clicked, selection starts from that state. If replay of those steps fails, show the error and write nothing. Do not fall back to a blank page and pretend it is the context.

## Parse rule

The kind comes from the tag, input type, and role. Do not ask the user to classify it. A select must include its options so a later Select step can offer them. A checkbox must not be offered as Type.

## Speed rule

This feature is annoying if it is slow. See [[Speed and feel]].

- One screenshot when the context is ready. Do not take a new screenshot on every mouse move.
- Hover asks only for the element under the point. Target under 150 ms on a local grid after the session exists.
- Debounce cursor moves. Do not send a request per pixel.
- Keep at most one inspect session. A new selection closes the previous one.
- Close and a failed start always quit the browser. A session dies after two minutes if the user walks away.
- The canvas stays usable. Selection is a modal, not a frozen studio.
- If the grid is down, fail in one message. Do not retry for half a minute.

## Not this feature

- A Chrome extension.
- A full recorder that emits a whole scenario from browsing.
- noVNC as a requirement. The viewport image plus cursor hit-testing is enough.
- Cucumber. The result is still a blueprint step.
