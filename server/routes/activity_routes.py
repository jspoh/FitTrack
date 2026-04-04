from datetime import date, timedelta

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from db import get_db
from models.UserModel import UserModel
from models.ActivityModel import ActivityModel
from schemas.activity_schemas import ActivityLogPayload, ActivityUpdatePayload
from utils.auth import get_current_user

router = APIRouter(prefix="/activity", tags=["activity"])


@router.post("/log", status_code=201)
def create_activity(body: ActivityLogPayload, current_user: UserModel = Depends(get_current_user), db: Session = Depends(get_db)):
  
  new_activity = ActivityModel(
    user_id=current_user.id, 
    activity_type=body.activity_type,
    start=body.start,
    end=body.end,
    notes=body.notes,
    steps_taken=body.steps_taken,
    max_hr=body.max_hr,
    activity_name=body.activity_name
    )
  db.add(new_activity)
  db.commit()
  db.refresh(new_activity)
  
  return new_activity

@router.patch("/")
def update_activity(body: ActivityUpdatePayload, current_user: UserModel = Depends(get_current_user), db: Session = Depends(get_db)):
  activity = db.query(ActivityModel).filter_by(id=body.id, user_id=current_user.id).first()
  if not activity:
    raise HTTPException(status_code=404, detail="Activity not found")

  for field, value in body.model_dump(exclude={"id"}, exclude_none=True).items():
    setattr(activity, field, value)

  db.commit()
  db.refresh(activity)
  return activity



@router.get("/date/{day}")
def get_activities_by_date(day: date, current_user: UserModel = Depends(get_current_user), db: Session = Depends(get_db)):
  return (
    db.query(ActivityModel)
    .filter(
      ActivityModel.user_id == current_user.id,
      ActivityModel.start >= f"{day} 00:00:00",
      ActivityModel.start <= f"{day} 23:59:59"
    )
    .order_by(ActivityModel.start)
    .all()
  )


@router.get("/last/{n}")
def get_activities_last_n_days(n: int, current_user: UserModel = Depends(get_current_user), db: Session = Depends(get_db)):
  since = date.today() - timedelta(days=n - 1)
  return (
    db.query(ActivityModel)
    .filter(
      ActivityModel.user_id == current_user.id,
      ActivityModel.start >= f"{since} 00:00:00"
    )
    .order_by(ActivityModel.start)
    .all()
  )


@router.get("/range")
def get_activities_range(start: date = Query(...), end: date = Query(...), current_user: UserModel = Depends(get_current_user), db: Session = Depends(get_db)):
  return (
    db.query(ActivityModel)
    .filter(
      ActivityModel.user_id == current_user.id,
      ActivityModel.start >= f"{start} 00:00:00",
      ActivityModel.start <= f"{end} 23:59:59"
    )
    .order_by(ActivityModel.start)
    .all()
  )


@router.delete("/{activity_id}", status_code=204)
def delete_activity(activity_id: int, current_user: UserModel = Depends(get_current_user), db: Session = Depends(get_db)):
  deleted = db.query(ActivityModel).filter_by(id=activity_id, user_id=current_user.id).delete()
  if not deleted:
    raise HTTPException(status_code=404, detail="Activity not found")
  db.commit()
