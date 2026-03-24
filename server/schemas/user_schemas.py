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
