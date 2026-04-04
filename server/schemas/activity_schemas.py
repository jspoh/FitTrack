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