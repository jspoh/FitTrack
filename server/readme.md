# Docs

## Quickstart

### Windows

```bash
python -m venv venv
./venv/Scripts/pip.exe install -r requirements.txt
alembic upgrade head
```

### Linux

```bash
./init.sh
```

## Routes

- /docs
- /redoc
- /openapi.json

## Migrations

On every db update,

1. `alembic revision --autogenerate -m "..."`
2. `alembic upgrade head`

- Migrations handled with `alembic`
- run `init.sh` on every db/dependency change to update environment

- ORM: SQLAlchemy

## Authentication

- Hashing handled with `passlib[bcrypt]`
- JWT (infinite lifespan to prevent relogins)
