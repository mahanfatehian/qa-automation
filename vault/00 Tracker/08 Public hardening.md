# 08 Public hardening

Status: Not started

Spec: [[12 Quality and security]]. Register: [[Requirement register]]. Order: step 8 in [[Development sequence]].

## Outcome

The repo can stay public, and a deployment is not one leaked default password away from an open browser farm.

## Why this is a big task

The current defaults are for a local clone: database password `blueprint`, open ports, webhook tokens stored in the API response, no login. That is acceptable on localhost. It is not acceptable on a public host.

## Current evidence

- README says not to expose the ports as-is.
- There is no user auth.
- Schema is `create_all` only. A model change needs `make clean`.
- There is one Python validation test. No runner test. No studio test.
- Webhook tokens are returned with the blueprint payload.

## Work

- [ ] Write the local-only threat note in the README in plain language: who can run a browser, who can read screenshots.
- [ ] Add database migrations before any further model change that would require a volume wipe.
- [ ] Add a single-user gate before any non-local deploy: one admin token or a real login. Do not pretend multi-tenant SaaS is done.
- [ ] Stop returning webhook tokens on the list endpoint once a gate exists. The schedule modal can still reveal the token to the owner.
- [ ] Runner probe and picker endpoints are not reachable except through the control plane.
- [ ] Add tests that do not need the grid: graph validation, pin rules, webhook auth rejection.
- [ ] Add one grid test in CI only if a runner is available. Otherwise document that grid tests are local.
- [ ] Change-default-password path is documented and used by compose when env vars are set.

## Done when

A stranger with the GitHub repo cannot drive someone else's grid, and a documented local boot still works with no `.env`.

## Depends on

[[01 Prove the stack]]. Do this before any public host. Do not block the picker on a full auth system.

## Not in this task

Teams, billing, SSO, or a marketplace of steps.
