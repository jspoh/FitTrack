from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from db import get_db
from models.UserModel import User
from schemas.user_schemas import GetUserResponse, CreateUserPayload, LoginPayload
from utils.auth import hash_password, verify_password

router = APIRouter(prefix="/users", tags=["users"])


@router.post("/", response_model=GetUserResponse)
def create_user(body: CreateUserPayload, db: Session = Depends(get_db)):
    if db.query(User).filter(User.email == body.email).first():
        raise HTTPException(status_code=409, detail="Email already registered")
    db_user = User(
        username=body.username,
        email=body.email,
        password_hash=hash_password(body.password)
    )
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

@router.post("/login", response_model=GetUserResponse)
def login(body: LoginPayload, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.username == body.username).first()
    if not user or not verify_password(body.password, str(user.password_hash)):
        raise HTTPException(status_code=401, detail="Invalid username or password")
    return user
