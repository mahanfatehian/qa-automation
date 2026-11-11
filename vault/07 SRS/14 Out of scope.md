---
type: srs
---

# 14 Out of scope

Part of [[00 SRS index]]. These are not gaps in the final product. They are refusals. Do not implement them under an epic without adding a new requirement here first.

## Product shape

- A general automation builder, including app-integration nodes, credential vaults for third-party SaaS, or a code step as the normal way to test a page.
- A second canvas beside the blueprint.
- A marketplace of steps.
- Billing, teams, SSO, and multi-tenant hosting. FR-SEC-02 is a single gate, not a SaaS.

## Browser tooling that is not required

- A Chrome extension. The picker in [[08 Picker requirements]] uses the grid and a screenshot with boxes.
- A full recorder that emits a blueprint from a browsing session. It may be proposed later. It is not in [[15 Final acceptance]].
- Video recording of the grid session.
- Pixel diff against the last passing screenshot.
- True Chrome DevTools network-idle. Wait's settle mode stays a heuristic.

## Proof limits

- Firefox is optional. Acceptance uses Chrome.
- Mobile device emulation is not required.
- Load testing the grid is not required. Two overlapping runs are required by NFR-03.

## Hosting

- The final product does not include a public hosted runner. CI calls a stack the operator already started.
