# BlueprintQA

Node-to-node browser tests. You draw a blueprint, connect white execution pins and colored data pins, and a Selenium Grid runs it. No test code required.

This is not a general automation builder. The graph is a QA blueprint: start a browser, open a page, find an element, click, type, read, branch, check.

## Run without a `.env`

```bat
up.bat
```

or:

```bash
docker compose up --build -d --scale chrome-node=2
```

Defaults are already in `docker-compose.yml`. Copy `.env.example` only if you want to override them.

| Surface | URL |
| --- | --- |
| Blueprint studio | http://localhost:3000 |
| API docs | http://localhost:8000/docs |
| Selenium Grid | http://localhost:4444 |

First boot builds the Java runner and the UI. Open the studio and press **Load demo**, then **Run blueprint**.

`make up` / `make down` / `make logs` / `make scale N=4` / `make clean` do the same thing when `make` is installed.

Firefox nodes are optional:

```bash
docker compose --profile firefox up -d
```

## How a blueprint runs

1. The studio saves the graph to the Python control plane.
2. Run, cron, interval, or webhook puts a JSON job on Redis.
3. The Java 21 runner walks execution pins, passes element/text pins in memory, and drives `RemoteWebDriver` on the grid.
4. Each step reports running, passed, or failed, plus duration, output, and a screenshot.
5. The studio paints the active node and keeps the screenshot in the bottom drawer.

White pins order steps. Colored pins pass an element, text, or boolean. Do not mix those pin types.

## Automatic runs

A blueprint can run on its own without becoming a different kind of product:

- cron, for example `0 */6 * * *`
- fixed interval, minimum 15 seconds
- webhook for CI: `POST /api/v1/webhooks/trigger/{id}?token=...&wait=true`
- JUnit XML: `GET /api/v1/runs/{id}/junit`

Scheduled runs skip if that blueprint is already queued or running. Manual and webhook runs always enqueue.

## Layout

```text
frontend/        React blueprint studio
control-plane/   FastAPI, Postgres, schedules, websocket feedback
runner-engine/   Java 21 virtual threads + Selenium 4.52
vault/           Obsidian notes for this map and later work
```

The product dogfoods itself with Playwright. With the stack already running:

```bat
cd e2e
npm install
npm test
```

The suite uses the Chrome already installed on the machine (`channel: chrome`), so it does not need the Playwright browser download. `STUDIO_URL` defaults to `http://127.0.0.1:3000`. The tests click the studio the way a QA user would: open the canvas, load the fixture blueprint, save it, and run it on the grid.

Open `vault/` as an Obsidian vault. `vault/07 SRS/` is the software requirements baseline for the final product. `vault/07 SRS/Development sequence.md` is the step order. `vault/00 Tracker/` is the work board. The product on `main` is not that final product yet.

Screenshots stay in the local Postgres volume until `make clean` or `docker compose down -v`.

A running stack can be called from CI:

```yaml
- run: curl -fsS -X POST "$BLUEPRINT_URL/api/v1/webhooks/trigger/$WORKFLOW_ID?token=$TOKEN&wait=true"
```

`$BLUEPRINT_URL` is the control plane, for example `http://localhost:8000`. A failed blueprint returns 422 and fails the job. The stack must already be running.

## Notes before you expose it

Local defaults use password `blueprint` and publish ports on the machine. Do not put this on the public internet until you add auth and change those passwords. Webhook tokens are per blueprint.
