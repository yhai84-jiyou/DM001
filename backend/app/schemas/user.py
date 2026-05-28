from pydantic import BaseModel


class UserOut(BaseModel):
    id: str
    name: str
    phone: str
    email: str | None = None
    org_id: str
    dept_id: str | None = None
    user_type: str
    vendor_role: str | None = None
    status: str
    org_name: str | None = None

    model_config = {"from_attributes": True}


class UserCreate(BaseModel):
    name: str
    phone: str
    password: str
    email: str | None = None
    org_id: str
    dept_id: str | None = None
    user_type: str
    vendor_role: str | None = None


class UserUpdate(BaseModel):
    name: str | None = None
    email: str | None = None
    dept_id: str | None = None
    vendor_role: str | None = None
