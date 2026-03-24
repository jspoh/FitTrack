from sqlalchemy import Column, Integer, String

from db import Base


class User(Base):
    __tablename__ = "users"
    id = Column(Integer, primary_key=True, index=True)
    username = Column(String, nullable=False)
    email = Column(String, unique=True, nullable=False)
    step_target = Column(Integer, nullable=False, default=10000)
    password_hash = Column(String, nullable=False)
