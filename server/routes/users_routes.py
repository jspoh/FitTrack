from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from db import get_db
from models.UserModel import User
from schemas.UserSchema import GetUserResponse, CreateUserPayload

router = APIRouter(prefix="/users", tags=["users"])


@router.post("/", response_model=GetUserResponse)
def create_user(body: CreateUserPayload, db: Session = Depends(get_db)):
    db_user = User(username=body.username, email=body.email)
    db.add(db_user)
    db.commit()
    db.refresh(db_user)
    return db_user


@router.get("/", response_model=int)
def get_user_count(db: Session = Depends(get_db)):
    return db.query(User).count()


@router.get("/{username}", response_model=GetUserResponse)
def read_user(username: str, db: Session = Depends(get_db)):
    return db.query(User).filter(User.username == username).first()
