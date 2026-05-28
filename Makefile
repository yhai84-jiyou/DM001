.PHONY: dev up down logs seed status

dev:
	docker compose up pm-postgres pm-redis -d

up:
	docker compose up -d --build

down:
	docker compose down

logs:
	docker compose logs -f --tail=50

seed:
	docker compose exec pm-backend python -m app.seed

status:
	docker compose ps
