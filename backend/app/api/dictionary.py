import uuid

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.core.dependencies import get_current_user, require_roles
from app.models.dictionary import DictionaryCategory, DictionaryItem, StatusTransition
from app.models.user import User
from app.schemas.common import ApiResponse
from app.schemas.dictionary import DictItemCreate, DictItemUpdate

router = APIRouter()


@router.get("/categories")
async def list_categories(db: AsyncSession = Depends(get_db), _user: User = Depends(get_current_user)):
    result = await db.execute(select(DictionaryCategory).where(DictionaryCategory.is_active == True).order_by(DictionaryCategory.code))
    cats = result.scalars().all()
    return ApiResponse(data=[{
        "id": str(c.id), "name": c.name, "code": c.code,
        "description": c.description, "is_system": c.is_system, "is_active": c.is_active,
    } for c in cats])


@router.get("/{category_code}/items")
async def list_items_by_category(category_code: str, include_inactive: bool = False, db: AsyncSession = Depends(get_db)):
    cat_result = await db.execute(select(DictionaryCategory).where(DictionaryCategory.code == category_code))
    cat = cat_result.scalar_one_or_none()
    if not cat:
        raise HTTPException(status_code=404, detail=f"字典分类 {category_code} 不存在")

    query = select(DictionaryItem).where(DictionaryItem.category_id == cat.id)
    if not include_inactive:
        query = query.where(DictionaryItem.is_active == True)
    query = query.order_by(DictionaryItem.sort_order, DictionaryItem.created_at)
    result = await db.execute(query)
    items = result.scalars().all()

    return ApiResponse(data=[{
        "id": str(i.id), "name": i.name, "code": i.code, "color": i.color,
        "icon": i.icon, "sort_order": i.sort_order, "is_default": i.is_default,
        "is_active": i.is_active, "is_system": i.is_system, "extra_config": i.extra_config,
    } for i in items])


@router.post("/{category_code}/items")
async def create_item(
    category_code: str, req: DictItemCreate,
    db: AsyncSession = Depends(get_db), _user: User = Depends(require_roles("ADMIN")),
):
    cat_result = await db.execute(select(DictionaryCategory).where(DictionaryCategory.code == category_code))
    cat = cat_result.scalar_one_or_none()
    if not cat:
        raise HTTPException(status_code=404, detail=f"字典分类 {category_code} 不存在")

    existing = await db.execute(
        select(DictionaryItem).where(DictionaryItem.category_id == cat.id, DictionaryItem.code == req.code)
    )
    if existing.scalar_one_or_none():
        raise HTTPException(status_code=400, detail=f"编码 {req.code} 已存在")

    item = DictionaryItem(
        category_id=cat.id, name=req.name, code=req.code, color=req.color,
        icon=req.icon, sort_order=req.sort_order, is_default=req.is_default,
        extra_config=req.extra_config,
    )
    db.add(item)
    await db.flush()
    return ApiResponse(data={"id": str(item.id)}, message="字典项创建成功")


@router.put("/items/{item_id}")
async def update_item(
    item_id: str, req: DictItemUpdate,
    db: AsyncSession = Depends(get_db), _user: User = Depends(require_roles("ADMIN")),
):
    result = await db.execute(select(DictionaryItem).where(DictionaryItem.id == uuid.UUID(item_id)))
    item = result.scalar_one_or_none()
    if not item:
        raise HTTPException(status_code=404, detail="字典项不存在")

    for field, value in req.model_dump(exclude_unset=True).items():
        setattr(item, field, value)
    return ApiResponse(message="字典项更新成功")


@router.delete("/items/{item_id}")
async def delete_item(
    item_id: str,
    db: AsyncSession = Depends(get_db), _user: User = Depends(require_roles("ADMIN")),
):
    result = await db.execute(select(DictionaryItem).where(DictionaryItem.id == uuid.UUID(item_id)))
    item = result.scalar_one_or_none()
    if not item:
        raise HTTPException(status_code=404, detail="字典项不存在")
    if item.is_system:
        raise HTTPException(status_code=400, detail="系统内置项不可删除，只能禁用")

    await db.delete(item)
    return ApiResponse(message="字典项已删除")


@router.get("/transitions")
async def list_transitions(db: AsyncSession = Depends(get_db), _user: User = Depends(get_current_user)):
    result = await db.execute(select(StatusTransition).where(StatusTransition.is_active == True))
    transitions = result.scalars().all()
    return ApiResponse(data=[{
        "id": str(t.id),
        "from_status": {"id": str(t.from_status.id), "name": t.from_status.name, "code": t.from_status.code},
        "to_status": {"id": str(t.to_status.id), "name": t.to_status.name, "code": t.to_status.code},
        "allowed_roles": t.allowed_roles,
        "special_rule": t.special_rule,
    } for t in transitions])
