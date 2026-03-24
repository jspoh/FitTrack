from datetime import date

from pydantic import BaseModel


class StepsSyncPayload(BaseModel):
    date: date
    steps: int


class DailyStepsResponse(BaseModel):
    date: date
    steps: int

    class Config:
        from_attributes = True
