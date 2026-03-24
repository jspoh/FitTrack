import os

from fastapi import FastAPI
from dotenv import load_dotenv

from routes.users_routes import router as users_router
from routes.activity_routes import router as activity_router
from routes.steps_routes import router as steps_router

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
app.include_router(activity_router)
app.include_router(steps_router)
