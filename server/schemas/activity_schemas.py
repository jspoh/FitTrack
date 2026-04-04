from datetime import datetime

from pydantic import BaseModel

class ActivityLogPayload(BaseModel):
  activity_name: str = "New Activity"
  start: datetime
  end: datetime
  activity_type: str
  steps_taken: int
  max_hr: int
  notes: str


class ActivityUpdatePayload(BaseModel):
  id: int
  activity_name: str | None = None
  start: datetime | None = None
  end: datetime | None = None
  activity_type: str | None = None
  steps_taken: int | None = None
  max_hr: int | None = None
  notes: str | None = None