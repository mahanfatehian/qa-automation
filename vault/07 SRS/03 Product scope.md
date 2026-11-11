---
type: srs
---

# 03 Product scope

Part of [[00 SRS index]].

## In the final product

- A blueprint studio for browser tests.
- A Selenium Grid cluster started with the same command as the app.
- Python as the API, store, scheduler, and feedback bridge.
- Java as the only component that opens browsers.
- Manual run, cron, interval, and webhook.
- Step feedback: status, duration, output, error, URL, screenshot, history.
- An element picker that writes a selector into an existing step.
- The browser actions in [[09 Action requirements]].
- A quotes demo and a local fixture so proof does not depend on the public internet.
- One-command startup with no required `.env`.
- A public-repo safety bar: localhost is usable with defaults; a non-local host requires a gate.

## Not in the final product

Listed in full in [[14 Out of scope]]. Short version: no general integration builder, no Slack or email nodes, no marketplace, no billing, no multi-tenant SaaS, no Chrome extension as a requirement, no video as a requirement.

## Assumptions

- The user has Docker Desktop and can run Linux containers.
- The first supported desktop is Windows, because that is the machine this repo is built on. `up.bat` is the primary command there. `make up` is the same command where `make` exists.
- Chrome is the default browser. Firefox is optional and not required for acceptance.
- A blueprint runs against pages the user is allowed to test.

## Success for the owner

A QA engineer who cannot write Selenium can clone the repo, start it, build or load a blueprint, pick an element, run it, read the failure, and schedule that same blueprint.
