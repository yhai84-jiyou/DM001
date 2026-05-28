from pydantic import BaseModel


class DictCategoryOut(BaseModel):
    id: str
    name: str
    code: str
    description: str | None = None
    is_system: bool
    is_active: bool

    model_config = {"from_attributes": True}


class DictItemOut(BaseModel):
    id: str
    name: str
    code: str
    color: str | None = None
    icon: str | None = None
    sort_order: int
    is_default: bool
    is_active: bool
    is_system: bool
    extra_config: dict | None = None

    model_config = {"from_attributes": True}


class DictItemCreate(BaseModel):
    name: str
    code: str
    color: str | None = None
    icon: str | None = None
    sort_order: int = 0
    is_default: bool = False
    extra_config: dict | None = None


class DictItemUpdate(BaseModel):
    name: str | None = None
    color: str | None = None
    icon: str | None = None
    sort_order: int | None = None
    is_default: bool | None = None
    is_active: bool | None = None
    extra_config: dict | None = None
