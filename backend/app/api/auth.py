import uuid

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.core.security import hash_password, verify_password, create_access_token, create_refresh_token, decode_token
from app.core.dependencies import get_current_user
from app.models.user import User
from app.schemas.auth import RegisterRequest, LoginRequest, TokenResponse, RefreshRequest
from app.schemas.common import ApiResponse

router = APIRouter()


@router.post("/register")
async def register(req: RegisterRequest, db: AsyncSession = Depends(get_db)):
    existing = await db.execute(select(User).where(User.phone == req.phone))
    if existing.scalar_one_or_none():
        raise HTTPException(status_code=400, detail="该手机号已注册")

    from app.models.organization import Organization
    if req.org_id:
        org_id = uuid.UUID(req.org_id)
    else:
        org_result = await db.execute(select(Organization).where(Organization.org_type == "CLIENT").limit(1))
        org = org_result.scalar_one_or_none()
        if not org:
            raise HTTPException(status_code=400, detail="系统尚未初始化组织数据")
        org_id = org.id

    user = User(
        name=req.name,
        phone=req.phone,
        password_hash=hash_password(req.password),
        org_id=org_id,
        user_type="CLIENT",
        status="PENDING",
    )
    db.add(user)
    await db.flush()
    return ApiResponse(data={"id": str(user.id)}, message="注册申请已提交，等待管理员审批")


@router.post("/login")
async def login(req: LoginRequest, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(User).where(User.phone == req.phone))
    user = result.scalar_one_or_none()
    if not user or not verify_password(req.password, user.password_hash):
        raise HTTPException(status_code=401, detail="手机号或密码错误")
    if user.status == "PENDING":
        raise HTTPException(status_code=403, detail="账号待审批，请联系管理员")
    if user.status == "DISABLED":
        raise HTTPException(status_code=403, detail="账号已禁用")

    token_data = TokenResponse(
        access_token=create_access_token(str(user.id)),
        refresh_token=create_refresh_token(str(user.id)),
    )
    return ApiResponse(data=token_data.model_dump())


@router.post("/refresh")
async def refresh_token(req: RefreshRequest):
    payload = decode_token(req.refresh_token)
    if not payload or payload.get("type") != "refresh":
        raise HTTPException(status_code=401, detail="无效的刷新令牌")

    user_id = payload.get("sub")
    token_data = TokenResponse(
        access_token=create_access_token(user_id),
        refresh_token=create_refresh_token(user_id),
    )
    return ApiResponse(data=token_data.model_dump())


@router.get("/me")
async def get_me(user: User = Depends(get_current_user)):
    return ApiResponse(data={
        "id": str(user.id),
        "name": user.name,
        "phone": user.phone,
        "email": user.email,
        "user_type": user.user_type,
        "vendor_role": user.vendor_role,
        "org_id": str(user.org_id),
        "status": user.status,
        "roles": [{"code": r.code, "name": r.name} for r in user.roles],
    })
