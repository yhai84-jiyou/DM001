import uuid
from datetime import date

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import select, func
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.core.dependencies import get_current_user
from app.core.state_machine import get_available_transitions, transition
from app.core.exceptions import StateTransitionError
from app.models.user import User
from app.models.issue import Issue
from app.models.changelog import ChangeLog
from app.models.comment import Comment
from app.models.dictionary import DictionaryCategory, DictionaryItem
from app.schemas.common import ApiResponse
from app.schemas.issue import IssueCreate, IssueUpdate, TransitionRequest, CommentCreate
from app.utils.id_generator import generate_issue_no

router = APIRouter()


def _issue_to_dict(i: Issue) -> dict:
    return {
        "id": str(i.id), "issue_no": i.issue_no, "title": i.title,
        "description": i.description,
        "platform": {"id": str(i.platform.id), "name": i.platform.name, "code": i.platform.code, "color": i.platform.color} if i.platform else None,
        "issue_type": {"id": str(i.issue_type.id), "name": i.issue_type.name, "code": i.issue_type.code, "color": i.issue_type.color} if i.issue_type else None,
        "priority": {"id": str(i.priority.id), "name": i.priority.name, "code": i.priority.code, "color": i.priority.color} if i.priority else None,
        "status": {"id": str(i.status.id), "name": i.status.name, "code": i.status.code, "color": i.status.color} if i.status else None,
        "submitter": {"id": str(i.submitter.id), "name": i.submitter.name} if i.submitter else None,
        "submitted_at": i.submitted_at.isoformat() if i.submitted_at else None,
        "product_owner": {"id": str(i.product_owner.id), "name": i.product_owner.name} if i.product_owner else None,
        "dev_owner": {"id": str(i.dev_owner.id), "name": i.dev_owner.name} if i.dev_owner else None,
        "test_owner": {"id": str(i.test_owner.id), "name": i.test_owner.name} if i.test_owner else None,
        "expected_date": i.expected_date.isoformat() if i.expected_date else None,
        "resolved_at": i.resolved_at.isoformat() if i.resolved_at else None,
        "updated_at": i.updated_at.isoformat() if i.updated_at else None,
    }


@router.post("")
async def create_issue(
    req: IssueCreate, db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    cat_result = await db.execute(
        select(DictionaryCategory).where(DictionaryCategory.code == "ISSUE_STATUS")
    )
    cat = cat_result.scalar_one()
    status_result = await db.execute(
        select(DictionaryItem).where(DictionaryItem.category_id == cat.id, DictionaryItem.code == "SUBMITTED")
    )
    initial_status = status_result.scalar_one()

    issue_no = await generate_issue_no(db)
    issue = Issue(
        issue_no=issue_no, title=req.title, description=req.description,
        platform_id=uuid.UUID(req.platform_id), issue_type_id=uuid.UUID(req.issue_type_id),
        priority_id=uuid.UUID(req.priority_id) if req.priority_id else None,
        status_id=initial_status.id, submitter_id=current_user.id,
    )
    db.add(issue)
    await db.flush()
    return ApiResponse(data={"id": str(issue.id), "issue_no": issue_no}, message="问题创建成功")


@router.get("")
async def list_issues(
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    platform_id: str | None = None,
    issue_type_id: str | None = None,
    status_id: str | None = None,
    priority_id: str | None = None,
    submitter_id: str | None = None,
    product_owner_id: str | None = None,
    dev_owner_id: str | None = None,
    test_owner_id: str | None = None,
    keyword: str | None = None,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    query = select(Issue)

    if current_user.user_type == "CLIENT":
        query = query.where(Issue.submitter_id == current_user.id)

    if platform_id:
        query = query.where(Issue.platform_id == uuid.UUID(platform_id))
    if issue_type_id:
        query = query.where(Issue.issue_type_id == uuid.UUID(issue_type_id))
    if status_id:
        query = query.where(Issue.status_id == uuid.UUID(status_id))
    if priority_id:
        query = query.where(Issue.priority_id == uuid.UUID(priority_id))
    if submitter_id:
        query = query.where(Issue.submitter_id == uuid.UUID(submitter_id))
    if product_owner_id:
        query = query.where(Issue.product_owner_id == uuid.UUID(product_owner_id))
    if dev_owner_id:
        query = query.where(Issue.dev_owner_id == uuid.UUID(dev_owner_id))
    if test_owner_id:
        query = query.where(Issue.test_owner_id == uuid.UUID(test_owner_id))
    if keyword:
        query = query.where(Issue.title.ilike(f"%{keyword}%"))

    total_result = await db.execute(select(func.count()).select_from(query.subquery()))
    total = total_result.scalar()

    query = query.order_by(Issue.submitted_at.desc()).offset((page - 1) * page_size).limit(page_size)
    result = await db.execute(query)
    issues = result.scalars().all()

    return ApiResponse(data={
        "items": [_issue_to_dict(i) for i in issues],
        "total": total, "page": page, "page_size": page_size,
    })


@router.get("/{issue_id}")
async def get_issue(
    issue_id: str, db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(select(Issue).where(Issue.id == uuid.UUID(issue_id)))
    issue = result.scalar_one_or_none()
    if not issue:
        raise HTTPException(status_code=404, detail="问题不存在")

    comments_result = await db.execute(
        select(Comment).where(Comment.issue_id == issue.id).order_by(Comment.created_at)
    )
    comments = comments_result.scalars().all()

    logs_result = await db.execute(
        select(ChangeLog).where(ChangeLog.entity_type == "issue", ChangeLog.entity_id == issue.id)
        .order_by(ChangeLog.changed_at.desc())
    )
    logs = logs_result.scalars().all()

    transitions = await get_available_transitions(issue, current_user, db)

    data = _issue_to_dict(issue)
    data["comments"] = [{
        "id": str(c.id), "content": c.content,
        "author": {"id": str(c.author.id), "name": c.author.name},
        "created_at": c.created_at.isoformat(),
    } for c in comments]
    data["changelog"] = [{
        "id": str(l.id), "field_name": l.field_name,
        "old_value": l.old_value, "new_value": l.new_value,
        "old_display": l.old_display, "new_display": l.new_display,
        "changed_by": {"id": str(l.changed_by_user.id), "name": l.changed_by_user.name},
        "changed_at": l.changed_at.isoformat(), "change_source": l.change_source,
    } for l in logs]
    data["available_transitions"] = transitions

    return ApiResponse(data=data)


@router.put("/{issue_id}")
async def update_issue(
    issue_id: str, req: IssueUpdate, db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(select(Issue).where(Issue.id == uuid.UUID(issue_id)))
    issue = result.scalar_one_or_none()
    if not issue:
        raise HTTPException(status_code=404, detail="问题不存在")

    for field, value in req.model_dump(exclude_unset=True).items():
        if value is not None and field.endswith("_id"):
            value = uuid.UUID(value)
        if field == "expected_date" and value:
            value = date.fromisoformat(value)
        setattr(issue, field, value)

    return ApiResponse(message="问题已更新")


@router.post("/{issue_id}/transition")
async def transition_issue(
    issue_id: str, req: TransitionRequest, db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(select(Issue).where(Issue.id == uuid.UUID(issue_id)))
    issue = result.scalar_one_or_none()
    if not issue:
        raise HTTPException(status_code=404, detail="问题不存在")

    try:
        await transition(issue, req.target_status_code, current_user, db)
    except StateTransitionError as e:
        raise HTTPException(status_code=400, detail=str(e))

    return ApiResponse(message=f"状态已更新为「{issue.status.name}」")


@router.get("/{issue_id}/transitions")
async def get_transitions(
    issue_id: str, db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(select(Issue).where(Issue.id == uuid.UUID(issue_id)))
    issue = result.scalar_one_or_none()
    if not issue:
        raise HTTPException(status_code=404, detail="问题不存在")

    transitions = await get_available_transitions(issue, current_user, db)
    return ApiResponse(data=transitions)


@router.post("/{issue_id}/comments")
async def add_comment(
    issue_id: str, req: CommentCreate, db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(select(Issue).where(Issue.id == uuid.UUID(issue_id)))
    if not result.scalar_one_or_none():
        raise HTTPException(status_code=404, detail="问题不存在")

    comment = Comment(issue_id=uuid.UUID(issue_id), author_id=current_user.id, content=req.content)
    db.add(comment)
    await db.flush()
    return ApiResponse(data={"id": str(comment.id)}, message="评论已添加")


@router.get("/{issue_id}/changelog")
async def get_changelog(
    issue_id: str, page: int = Query(1, ge=1), page_size: int = Query(50, ge=1, le=200),
    db: AsyncSession = Depends(get_db), _user: User = Depends(get_current_user),
):
    query = select(ChangeLog).where(
        ChangeLog.entity_type == "issue", ChangeLog.entity_id == uuid.UUID(issue_id)
    ).order_by(ChangeLog.changed_at.desc())

    total_result = await db.execute(select(func.count()).select_from(query.subquery()))
    total = total_result.scalar()

    result = await db.execute(query.offset((page - 1) * page_size).limit(page_size))
    logs = result.scalars().all()

    return ApiResponse(data={
        "items": [{
            "id": str(l.id), "field_name": l.field_name,
            "old_value": l.old_value, "new_value": l.new_value,
            "old_display": l.old_display, "new_display": l.new_display,
            "changed_by": {"id": str(l.changed_by_user.id), "name": l.changed_by_user.name},
            "changed_at": l.changed_at.isoformat(), "change_source": l.change_source,
        } for l in logs],
        "total": total,
    })
