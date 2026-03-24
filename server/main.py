import os

from fastapi import FastAPI
from dotenv import load_dotenv

from routes.users_routes import router as users_router

load_dotenv(".env")
ENV = os.environ["ENV"]
PROD = ENV == "PROD"

app = FastAPI(
    title="FitTrack API",
    docs_url=None if PROD else "/docs",
    redoc_url=None if PROD else "/redoc",
    openapi_url=None if PROD else "/openapi.json"
    )


@app.get("/")
def status():
    return {"STATUS": "RUNNING"}


app.include_router(users_router)
