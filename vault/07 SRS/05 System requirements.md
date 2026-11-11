---
type: srs
epic: "[[01 Prove the stack]]"
---

# 05 System requirements

Part of [[00 SRS index]]. Work lives in [[01 Prove the stack]].

## FR-SYS-01 One command, no required env file

The system shall start Postgres, Redis, Selenium Hub, at least two Chrome nodes, the control plane, the runner, and the studio with one command and no `.env` file.

Acceptance: `up.bat` on Windows, or the compose command in the README, reaches a usable studio. Defaults live in compose.

## FR-SYS-02 Published surfaces

After startup the system shall serve the studio on port 3000, the API on port 8000, and the grid console on port 4444, unless those host ports are overridden.

Acceptance: all three URLs respond. The studio calls the API through the same origin proxy, not a hardcoded container hostname.

## FR-SYS-03 Health

The control plane shall expose `/health`. The runner shall expose `/health`. The grid shall expose `/status`. Compose shall wait on those checks before dependent services are treated as ready.

Acceptance: a cold start does not mark the runner healthy before it can answer `/health`.

## FR-SYS-04 Browser cluster

The default cluster shall be Chrome. Chrome nodes shall set shared memory to 2 GB. Session capacity shall be configurable. Firefox shall be available only with the compose profile `firefox` and shall not be required to start the default stack.

Acceptance: `docker compose ps` shows hub and chrome nodes. Firefox containers are absent unless the profile is requested.

## FR-SYS-05 Scale

The operator shall be able to change the number of Chrome nodes without editing compose, using `docker compose up -d --scale chrome-node=N` or `make scale N=N`.

Acceptance: raising the count registers more nodes on the hub. Fixed container names are not used on the scalable service.

## FR-SYS-06 First-run data

On an empty database the control plane shall create tables and seed the quotes demo blueprint, including its webhook token.

Acceptance: a new volume shows one blueprint in the studio without a manual insert.

## FR-SYS-07 Logs

The operator shall be able to follow control plane, runner, and hub logs with one documented command.

Acceptance: `docker compose logs -f control-plane runner-engine selenium-hub` shows a run moving from queued to a terminal status.

## FR-SYS-08 Language split

The studio shall not open a browser. The control plane shall not open a browser. Only the runner shall create `RemoteWebDriver` sessions.

Acceptance: a repo search shows Selenium driver creation only under `runner-engine/`.
