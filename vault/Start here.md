# Start here

BlueprintQA is a **blueprint** for browser QA. Steps connect pin to pin, like Unreal Blueprints. It is not an n8n-style integration product.

n8n was only a reference for **automatic runs**: cron, interval, webhook, unattended execution, and a result you can read later. The canvas, the node set, and the data model stay blueprint.

## Read next

- [[Blueprint not n8n]]
- [[System map]]
- [[Pins and steps]]
- [[Automatic runs]]
- [[Step feedback]]
- [[Later - element picker]]
- [[Later - more browser actions]]
- [[Backlog]]
- [[Direction correction]]

## What exists now

- Visual blueprint studio on a dark grid
- White execution pins and colored data pins
- Python control plane, Java runner, Selenium Grid 4
- Manual run, cron, interval, webhook, JUnit, screenshots
- One demo blueprint against `quotes.toscrape.com`
- Selector probe: type an XPath and ask the grid if it matches

## What is explicitly later

- Click-to-pick element engine
- Recorder that emits a blueprint from clicks
- Scroll, hover, select, iframe, tab, upload, alert steps
- Resilient selector chains (`data-testid`, id, css, xpath fallback)
- Multi-user auth

Do not grow this into a generic workflow tool. If a step is not a browser-test action, it does not belong on the canvas.
