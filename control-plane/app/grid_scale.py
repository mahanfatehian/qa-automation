import os
import subprocess
from pathlib import Path


def remember(count: int) -> None:
    path = Path(os.environ.get("BROWSER_COUNT_FILE", "/runtime/browsers.txt"))
    try:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(str(max(0, count)), encoding="utf-8")
    except OSError:
        return


def scale(count: int) -> str:
    remember(count)
    if count <= 0:
        return "Using every Chrome already on the grid."
    if not Path("/var/run/docker.sock").exists() or not Path("/usr/local/bin/docker").exists():
        return "Saved. The next start will open this many Chromes."
    compose = os.environ.get("COMPOSE_FILE", "/workspace/docker-compose.yml")
    if not Path(compose).exists():
        return "Saved. The grid file is not mounted, so the count applies on the next start."
    command = [
        "docker",
        "compose",
        "-f",
        compose,
        "-p",
        os.environ.get("COMPOSE_PROJECT_NAME", "blueprintqa"),
        "up",
        "-d",
        "--no-deps",
        "--scale",
        f"chrome-node={count}",
        "chrome-node",
    ]
    try:
        result = subprocess.run(command, capture_output=True, text=True, timeout=90, check=False)
    except (OSError, subprocess.TimeoutExpired) as exc:
        return f"Saved, but the grid did not scale: {exc}"
    if result.returncode != 0:
        detail = (result.stderr or result.stdout or "docker compose failed").strip().splitlines()
        return "Saved, but the grid did not scale: " + (detail[-1] if detail else "docker compose failed")
    return f"Opened {count} Chrome browsers."
