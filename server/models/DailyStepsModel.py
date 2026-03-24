from sqlalchemy import Column, Integer, Date, ForeignKey, UniqueConstraint
from sqlalchemy.orm import relationship

from db import Base


class DailyStepsModel(Base):
    __tablename__ = "daily_steps"

    id      = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    date    = Column(Date, nullable=False)
    steps   = Column(Integer, nullable=False, default=0)

    user = relationship("UserModel")

    __table_args__ = (UniqueConstraint("user_id", "date"),)
