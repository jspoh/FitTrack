from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from db import get_db
from models.UserModel import UserModel
from schemas.user_schemas import *
from utils.auth import hash_password, verify_password, create_access_token, get_current_user

router = APIRouter(prefix="/user", tags=["user"])


@router.post("/", response_model=GetUserResponse, status_code=201)
def create_user(body: CreateUserPayload, db: Session = Depends(get_db)):
    if db.query(UserModel).filter(UserModel.email == body.email).first():
        raise HTTPException(status_code=409, detail="Email already registered")
    db_user = UserModel(
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
    return db.query(UserModel).count()


@router.get("/me", response_model=GetUserResponse)
def get_me_for_testing_bearer_token(current_user: UserModel = Depends(get_current_user)):
    return current_user


@router.get("/{username}", response_model=GetUserResponse)
def read_user(username: str, db: Session = Depends(get_db)):
    return db.query(UserModel).filter(UserModel.username == username).first()


@router.post("/login", response_model=LoginResponse)
def login(body: LoginPayload, db: Session = Depends(get_db)):
    user = db.query(UserModel).filter(UserModel.username == body.username).first()
    if not user or not verify_password(body.password, str(user.password_hash)):
        raise HTTPException(status_code=401, detail="Invalid username or password")
    token = create_access_token(int(str(user.id)))
    return LoginResponse(access_token=token, user=user)

@router.patch("/", response_model=GetUserResponse)
def update_user(body: UpdateUserPayload, current_user: UserModel = Depends(get_current_user), db: Session = Depends(get_db)):
    if body.username is not None:
        if db.query(UserModel).filter(UserModel.username == body.username).first():
            raise HTTPException(status_code=409, detail="Username already taken")
        current_user.username = body.username
    if body.email is not None:
        if db.query(UserModel).filter(UserModel.email == body.email).first():
            raise HTTPException(status_code=409, detail="Email already in use")
        current_user.email = body.email
    if body.password is not None:
        current_user.password_hash = hash_password(body.password)
    if body.step_target is not None:
        current_user.step_target = body.step_target
    db.commit()
    db.refresh(current_user)
    return current_user

