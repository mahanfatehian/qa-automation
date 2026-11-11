# Step feedback

A blueprint run is not done if the only result is a green job. QA needs to see which pin failed and what the page looked like.

## Already stored per step

- status: running, passed, failed
- duration
- short message
- error text
- output data (text and booleans only; elements become a short tag/text summary)
- PNG screenshot, base64 in Postgres, fetched as an image

The studio paints the node yellow, green, or red and shows the screenshot in the bottom drawer.

Websocket: `/ws/runs/{run_id}`. On connect, stored steps are replayed so a late subscriber still sees the run.

## Still weak

- no video sidecar
- no DOM snapshot beside the screenshot
- no diff against the previous passing screenshot
- large screenshots can be omitted from the socket and must be opened from the API
- network-idle is a document-settle heuristic, not Chrome DevTools network idle

Full feedback later means: step log, screenshot, selector used, current URL, and a link to the grid session. Video is optional, not the core.
