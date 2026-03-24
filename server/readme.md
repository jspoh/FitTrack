# Docs

## Routes

- /doc
- /redoc
- /openapi.json

## Migrations

On every db update,

1. `alembic revision --autogenerate -m "..."`
2. `alembic upgrade head`

- Migrations handled with `alembic`
- run `init.sh` on every db/dependency change to update environment

- ORM: SQLAlchemy
