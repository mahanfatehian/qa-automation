# Start here

BlueprintQA is a **blueprint** for browser QA. Steps connect pin to pin, like Unreal Blueprints. It is not an n8n-style integration product.

n8n was only a reference for **automatic runs**: cron, interval, webhook, unattended execution, and a result you can read later. The canvas, the node set, and the data model stay blueprint.

## Read next

The final product is specified. The current branch is not that product.

- [[00 SRS index]] is the requirement baseline.
- [[Requirement register]] is every requirement and its status.
- [[Development sequence]] is the only build order.
- [[00 Board]] is the work board for those steps.
- [[Map.canvas]] is the same map on a canvas.
- [[15 Final acceptance]] is what "full ready" means.
- [[00 Features before build]] freezes the features asked for before more coding.
- [[Speed and feel]] wins when a feature would be slow or nagging.
- [[Element selection engine]] is the DevTools-style picker. It is built.
- [[Healenium decision]] rejects a healing database as the picker.
- [[QA node taxonomy]] is the needed consideration: assertions, inputs, verify, and search, not only procedure steps.
- [[Given When Then]] is that consideration as a scenario. Cucumber is not a dependency.
- [[Blueprint not n8n]]
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
