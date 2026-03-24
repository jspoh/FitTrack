from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from sqlalchemy.dialects.sqlite import insert

from db import get_db
from models.UserModel import UserModel
from models.DailyStepsModel import DailyStepsModel
from schemas.steps_schemas import StepsSyncPayload, DailyStepsResponse
from utils.auth import get_current_user

router = APIRouter(prefix="/steps", tags=["steps"])


@router.post("/sync", response_model=DailyStepsResponse, status_code=201)
def sync_steps(body: StepsSyncPayload, current_user: UserModel = Depends(get_current_user), db: Session = Depends(get_db)):
    stmt = (
        insert(DailyStepsModel)
        .values(user_id=current_user.id, date=body.date, steps=body.steps)
        .on_conflict_do_update(
            index_elements=["user_id", "date"],
            set_={"steps": body.steps}
        )
    )
    db.execute(stmt)
    db.commit()

    record = db.query(DailyStepsModel).filter_by(user_id=current_user.id, date=body.date).first()
    return record


@router.get("/", response_model=list[DailyStepsResponse])
def get_steps(current_user: UserModel = Depends(get_current_user), db: Session = Depends(get_db)):
    return db.query(DailyStepsModel).filter_by(user_id=current_user.id).order_by(DailyStepsModel.date).all()
