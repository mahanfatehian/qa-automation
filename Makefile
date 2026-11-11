.PHONY: up down logs scale clean ps

up:
	docker compose up --build -d --scale chrome-node=$$(if [ -f runtime/browsers.txt ]; then cat runtime/browsers.txt; else echo 2; fi)

down:
	docker compose down

logs:
	docker compose logs -f --tail=200 control-plane runner-engine selenium-hub

scale:
	docker compose up -d --scale chrome-node=$(N)

clean:
	docker compose down -v --remove-orphans

ps:
	docker compose ps
