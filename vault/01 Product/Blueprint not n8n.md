# Blueprint not n8n

## The one idea

A QA engineer builds a browser test by dropping steps and wiring pins. They should not have to write Selenium code.

The visual language is Unreal-style:

- white diamond **execution** pins decide what runs next
- colored round **data** pins pass an element, text, or boolean
- headers are colored by kind: flow, browser action, find/read, check
- the graph is a test procedure, not a bag of app integrations

## What we borrowed from n8n

Only the automation habits:

- run on a schedule without someone sitting at the canvas
- run from CI with a token
- keep a history of runs
- show pass/fail per step instead of a silent job

## What we are not building

- a marketplace of SaaS nodes
- generic HTTP/Slack/email workflows as the core
- a second canvas paradigm next to blueprints
- code nodes as the normal way to test a page

If a later feature feels like "make it more like n8n", stop and ask whether it still helps a browser blueprint.
