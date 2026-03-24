python3 -m venv venv
venv/bin/pip3 install -r requirements.txt
venv/bin/alembic init migrations
venv/bin/alembic revision --autogenerate -m "initial migration"
venv/bin/alembic upgrade head
