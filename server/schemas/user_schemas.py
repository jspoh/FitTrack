from pydantic import BaseModel

class CreateUserPayload(BaseModel):
  username: str
  email: str
  password: str
  
  
class GetUserResponse(BaseModel):
  id: int
  username: str
  email: str
  
  # for sqlalchemy
  class Config:
    from_attributes = True    # previously orm_mode
    
class LoginPayload(BaseModel):
  username: str
  password: str

class LoginResponse(BaseModel):
  access_token: str
  token_type: str = "bearer"
  user: GetUserResponse

class UpdateUserPayload(BaseModel):
  username: str | None = None
  email: str | None = None
  password: str | None = None
  step_target: int | None = None
