.PHONY: dev up down migrate seed

dev:
	docker compose up postgres redis -d

up:
	docker compose up -d --build

down:
	docker compose down

migrate:
	cd backend && alembic upgrade head

seed:
	cd backend && python -m app.seed
