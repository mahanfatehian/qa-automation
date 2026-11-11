# Direction correction

The first architecture note treated n8n as a product shape. That was wrong.

Correction, in the builder's words: n8n was only a sightline for automations and automatic runs. The product stays one idea: a blueprint. Node-to-node connections are execution pins and data pins for a browser test, not a general workflow builder.

## What that means in this repo

- Studio copy says blueprint, not workflow automation.
- Schedules, intervals, and webhooks are how a blueprint runs when nobody is at the canvas.
- Element picking, scroll, and extra browser actions are later notes in this vault, not a second product.
- A future agent should read [[Start here]] before adding a node.

## Origin

The stack split still stands: React blueprint canvas, Python control plane, Java runner, Selenium Grid. That came from the first design pass and is implemented. The correction only limits scope. It does not replace the runner or the pin model.
