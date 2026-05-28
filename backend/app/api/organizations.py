import uuid

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.core.dependencies import require_roles
from app.models.organization import Organization, Department
from app.models.user import User
from app.schemas.common import ApiResponse

router = APIRouter()


class OrgCreate(BaseModel):
    name: str
    code: str
    org_type: str
    parent_id: str | None = None


class DeptCreate(BaseModel):
    name: str
    org_id: str
    parent_id: str | None = None
    sort_order: int = 0


@router.get("")
async def list_organizations(db: AsyncSession = Depends(get_db), _user: User = Depends(require_roles("ADMIN", "VENDOR_MANAGER"))):
    result = await db.execute(select(Organization).order_by(Organization.created_at))
    orgs = result.scalars().all()
    return ApiResponse(data=[{
        "id": str(o.id), "name": o.name, "code": o.code, "org_type": o.org_type,
        "parent_id": str(o.parent_id) if o.parent_id else None, "status": o.status,
    } for o in orgs])


@router.post("")
async def create_organization(req: OrgCreate, db: AsyncSession = Depends(get_db), _user: User = Depends(require_roles("ADMIN"))):
    org = Organization(name=req.name, code=req.code, org_type=req.org_type, parent_id=uuid.UUID(req.parent_id) if req.parent_id else None)
    db.add(org)
    await db.flush()
    return ApiResponse(data={"id": str(org.id)}, message="组织创建成功")


@router.get("/{org_id}/departments")
async def list_departments(org_id: str, db: AsyncSession = Depends(get_db), _user: User = Depends(require_roles("ADMIN", "VENDOR_MANAGER"))):
    result = await db.execute(select(Department).where(Department.org_id == uuid.UUID(org_id)).order_by(Department.sort_order))
    depts = result.scalars().all()
    return ApiResponse(data=[{
        "id": str(d.id), "name": d.name, "org_id": str(d.org_id),
        "parent_id": str(d.parent_id) if d.parent_id else None, "sort_order": d.sort_order,
    } for d in depts])


@router.post("/departments")
async def create_department(req: DeptCreate, db: AsyncSession = Depends(get_db), _user: User = Depends(require_roles("ADMIN"))):
    dept = Department(name=req.name, org_id=uuid.UUID(req.org_id), parent_id=uuid.UUID(req.parent_id) if req.parent_id else None, sort_order=req.sort_order)
    db.add(dept)
    await db.flush()
    return ApiResponse(data={"id": str(dept.id)}, message="部门创建成功")


@router.delete("/departments/{dept_id}")
async def delete_department(dept_id: str, db: AsyncSession = Depends(get_db), _user: User = Depends(require_roles("ADMIN"))):
    result = await db.execute(select(Department).where(Department.id == uuid.UUID(dept_id)))
    dept = result.scalar_one_or_none()
    if not dept:
        raise HTTPException(status_code=404, detail="部门不存在")
    await db.delete(dept)
    return ApiResponse(message="部门已删除")
