# BlueprintQA

Draw a browser test. Connect the steps. Press run. A real Chrome on Selenium Grid does the clicks, and the studio shows each step, its branch, and a screenshot.

You do not write test code. You do not install Java, Node, or Python on the machine. Docker is the only requirement.

## What you need

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) on Windows or Mac, or Docker Engine plus the Compose plugin on Linux
- About 8 GB of free disk for the first image build
- These ports free: `3000`, `8000`, `4444`, `5432`, `6379`

On Windows, start Docker Desktop and wait until it says it is running.

## Start

From this folder.

Windows Command Prompt:

```bat
up.bat
```

Windows PowerShell:

```powershell
.\up.ps1
```

Mac, Linux, or Git Bash:

```bash
docker compose up --build -d --scale chrome-node=2
```

If `make` is installed, `make up` does the same thing.

The first start builds the Java runner and the studio. That can take several minutes. Later starts are faster. When the command returns, open:

| What | URL |
| --- | --- |
| Studio | http://localhost:3000 |
| API docs | http://localhost:8000/docs |
| Selenium Grid | http://localhost:4444 |

The studio is the app. Use http://localhost:3000. The page talks to the API for you. You do not open port 8000 to draw a blueprint.

Open a blueprint, or press **New**, then **Run blueprint**.

## If a port is already taken

The usual clash is port 8000. Leave the studio on 3000 and move only the API.

Windows Command Prompt:

```bat
set CONTROL_PLANE_PORT=8001
up.bat
```

PowerShell:

```powershell
$env:CONTROL_PLANE_PORT = "8001"
.\up.ps1
```

Bash:

```bash
CONTROL_PLANE_PORT=8001 docker compose up --build -d --scale chrome-node=2
```

The studio stays at http://localhost:3000. API docs move to http://localhost:8001/docs. Copy `.env.example` to `.env` only if you want that port saved. You do not need a `.env` file when the defaults are free.

If the studio page loads but every button fails, the studio proxy is pointing at an old API container. Restart just the studio:

```bash
docker compose restart frontend
```

## Stop, logs, reset

Stop the stack and keep your blueprints:

```bash
docker compose down
```

`make down` is the same.

Follow the API, the runner, and the grid:

```bash
docker compose logs -f --tail=200
```

`make logs` does the same.

Delete the database, including saved blueprints and screenshots:

```bash
docker compose down -v --remove-orphans
```

`make clean` is the same. Do not run it unless you mean to wipe local data.

## More than two browsers

A blueprint that splits into two roads needs two free Chromes. The next start reads `runtime/browsers.txt` when that file exists.

Start four Chromes now, without rebuilding:

```bash
docker compose up -d --scale chrome-node=4 --no-recreate --no-deps chrome-node
```

`make scale N=4` does the same. In the studio, **Settings → Browsers at once** asks the stack to match that number. `0` leaves the Chromes that are already up.

Watch a Chrome window at http://localhost:7900. The others are 7901, 7902, and 7903.

## How a blueprint runs

1. The studio saves the graph.
2. Run, a clock, an interval, or a webhook puts a job on Redis.
3. The runner walks the white pins, passes element and text pins in memory, and drives Chrome on the grid.
4. Each step reports passed or failed, with a duration and a screenshot.
5. A split gives each road its own Chrome, restored to the page where the road split.

White pins order the steps. Colored pins pass an element, text, or a yes/no. Do not mix those colors.

A click counts only when the page shows it. A find counts only when the element is on the page. If a step fails, the run stops on that step and the screenshot is the page it actually saw.

## Clocks and CI

In **Automation** you can add a cron or an interval. The shortest interval is 15 seconds. A scheduled run is skipped if that blueprint is already queued or running.

The webhook is on that same panel. From CI, with the stack already running:

```bash
curl -fsS -X POST "http://localhost:3000/api/v1/webhooks/trigger/WORKFLOW_ID?token=TOKEN&wait=true"
```

A failed blueprint returns 422. JUnit XML for a finished run is `GET /api/v1/runs/RUN_ID/junit`.

## Layout

```text
frontend/        studio
control-plane/   API, Postgres, schedules
runner-engine/   Java 21 runner and Selenium
e2e/             Playwright checks of the studio itself, not the product core
vault/           notes
```

The product core is Selenium Grid Chrome. Playwright in `e2e/` only clicks the studio. You do not need it to run blueprints.

Screenshots stay in the local Postgres volume until `make clean` or `docker compose down -v`.

## Before you put it on a network

The default database password is `blueprint`, there is no login, and the ports are published on the machine. This is a local tool. Do not expose it to the internet until you add auth and change that password. Webhook tokens are the only gate, and they are per blueprint.
