import uuid
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import select, func
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.core.security import hash_password
from app.core.dependencies import get_current_user, require_roles
from app.models.user import User
from app.models.role import Role
from app.schemas.common import ApiResponse
from app.schemas.user import UserCreate, UserUpdate

router = APIRouter()


@router.get("")
async def list_users(
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    user_type: str | None = None,
    status: str | None = None,
    org_id: str | None = None,
    db: AsyncSession = Depends(get_db),
    _user: User = Depends(require_roles("ADMIN", "VENDOR_MANAGER")),
):
    query = select(User)
    if user_type:
        query = query.where(User.user_type == user_type)
    if status:
        query = query.where(User.status == status)
    if org_id:
        query = query.where(User.org_id == uuid.UUID(org_id))

    total_result = await db.execute(select(func.count()).select_from(query.subquery()))
    total = total_result.scalar()

    query = query.offset((page - 1) * page_size).limit(page_size).order_by(User.created_at.desc())
    result = await db.execute(query)
    users = result.scalars().all()

    return ApiResponse(data={
        "items": [{
            "id": str(u.id), "name": u.name, "phone": u.phone, "email": u.email,
            "user_type": u.user_type, "vendor_role": u.vendor_role, "status": u.status,
            "org_id": str(u.org_id), "org_name": u.organization.name if u.organization else None,
            "roles": [{"code": r.code, "name": r.name} for r in u.roles],
            "created_at": u.created_at.isoformat(),
        } for u in users],
        "total": total,
        "page": page,
        "page_size": page_size,
    })


@router.post("")
async def create_user(
    req: UserCreate,
    db: AsyncSession = Depends(get_db),
    _user: User = Depends(require_roles("ADMIN")),
):
    existing = await db.execute(select(User).where(User.phone == req.phone))
    if existing.scalar_one_or_none():
        raise HTTPException(status_code=400, detail="该手机号已存在")

    user = User(
        name=req.name, phone=req.phone, password_hash=hash_password(req.password),
        email=req.email, org_id=uuid.UUID(req.org_id),
        dept_id=uuid.UUID(req.dept_id) if req.dept_id else None,
        user_type=req.user_type, vendor_role=req.vendor_role, status="ACTIVE",
    )
    db.add(user)
    await db.flush()
    return ApiResponse(data={"id": str(user.id)}, message="用户创建成功")


@router.post("/{user_id}/approve")
async def approve_user(
    user_id: str, db: AsyncSession = Depends(get_db),
    current_user: User = Depends(require_roles("ADMIN")),
):
    result = await db.execute(select(User).where(User.id == uuid.UUID(user_id)))
    user = result.scalar_one_or_none()
    if not user:
        raise HTTPException(status_code=404, detail="用户不存在")
    if user.status != "PENDING":
        raise HTTPException(status_code=400, detail="该用户不在待审批状态")

    user.status = "ACTIVE"
    user.approved_by = current_user.id
    user.approved_at = datetime.now(timezone.utc)

    default_role_code = "CLIENT_USER" if user.user_type == "CLIENT" else f"VENDOR_{user.vendor_role or 'DEV'}"
    role_result = await db.execute(select(Role).where(Role.code == default_role_code))
    role = role_result.scalar_one_or_none()
    if role:
        user.roles.append(role)

    return ApiResponse(message="用户已审批通过")


@router.post("/{user_id}/disable")
async def disable_user(
    user_id: str, db: AsyncSession = Depends(get_db),
    _user: User = Depends(require_roles("ADMIN")),
):
    result = await db.execute(select(User).where(User.id == uuid.UUID(user_id)))
    user = result.scalar_one_or_none()
    if not user:
        raise HTTPException(status_code=404, detail="用户不存在")
    user.status = "DISABLED"
    return ApiResponse(message="用户已禁用")


@router.put("/{user_id}")
async def update_user(
    user_id: str, req: UserUpdate, db: AsyncSession = Depends(get_db),
    _user: User = Depends(require_roles("ADMIN")),
):
    result = await db.execute(select(User).where(User.id == uuid.UUID(user_id)))
    user = result.scalar_one_or_none()
    if not user:
        raise HTTPException(status_code=404, detail="用户不存在")

    for field, value in req.model_dump(exclude_unset=True).items():
        setattr(user, field, value)
    return ApiResponse(message="用户信息已更新")
