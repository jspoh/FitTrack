from sqlalchemy import Column, Integer, String, DateTime, ForeignKey
from sqlalchemy.orm import relationship

from db import Base

class ActivityModel(Base):
  __tablename__ = "activities"
  id = Column(Integer, primary_key=True, index=True)
  user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
  activity_type = Column(String, nullable=False)
  start = Column(DateTime, nullable=False)
  end = Column(DateTime, nullable=False)
  notes = Column(String, nullable=True)
  steps_taken = Column(Integer, nullable=True)
  max_hr = Column(Integer, nullable=True)
  activity_name = Column(String, nullable=True)

  user = relationship("UserModel")