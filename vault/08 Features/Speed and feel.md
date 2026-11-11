---
type: constraint
priority: highest
---

# Speed and feel

This is the priority. Every feature in [[00 Features before build]] loses to this note when they conflict.

The service must feel fast and must not annoy the person making a test. A slow correct tool will not be used.

## What fast means

- The studio shell is on screen without waiting for the grid. Grid status can arrive a moment later.
- Saving a blueprint is a short request. The button may say Saved. It must not open a dialog.
- A run colors steps as they finish. The user can still read the canvas.
- List calls do not send screenshot bytes. Images load only when a step is opened.
- Page opens use an eager load, then wait for the document. They do not sit on every ad or font.
- The only sleep in the runner is an explicit Wait step.
- Hover and selection, once the browser is open, answer in a fraction of a second. See [[Element selection engine]].
- Scheduled runs do not pile up behind a run that is already going.

## What annoying means

Do not ship these:

- A confirm dialog for ordinary save, run, or selection.
- A spinner with no text while a browser starts. Say what is happening: opening the page, replaying the steps, waiting for the grid.
- A retry loop the user did not ask for, especially one that lasts minutes.
- A picker that resets to the first URL after the user already clicked through the app.
- A palette or inspector that asks for an XPath before showing the element.
- Logs or toasts on every successful step.
- Forcing the user to name a selector strategy when the element parse already knows the control.

One confirm is allowed: leaving a dirty blueprint, and deleting a blueprint.

## Build rule

Before marking a feature done, use it once on the running stack and notice if you waited without knowing why. If you did, it is not done. Fix the wait or the message first. Do not add the next feature on top of a stall.
