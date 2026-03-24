venv\Scripts\alembic.exe upgrade head
set ENV=DEV
venv\Scripts\uvicorn.exe main:app --reload --port 1363