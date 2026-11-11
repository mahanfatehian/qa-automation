# Later - more browser actions

Only add a step if a QA engineer can explain it in one sentence and it still uses execution pins plus data pins.

## Candidates

- Scroll: pixels, to element, top, bottom
- Hover
- Select dropdown
- Check / uncheck
- Press key
- Switch tab
- Switch iframe
- Accept or dismiss alert
- Upload file
- Go back
- Set cookie

Click already scrolls the target into view. That is not a Scroll step.

## Not candidates

- Slack, email, or generic API nodes
- a code step as the normal path
- a second canvas beside the blueprint

Scroll and the picker are the first two to add when this comes back. See [[Element picker]].
