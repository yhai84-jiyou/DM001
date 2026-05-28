import uuid
from datetime import datetime, timedelta, timezone, date

from fastapi import APIRouter, Depends, Query
from sqlalchemy import select, func, case, and_
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.core.dependencies import get_current_user
from app.models.user import User
from app.models.issue import Issue
from app.models.changelog import ChangeLog
from app.models.dictionary import DictionaryCategory, DictionaryItem
from app.schemas.common import ApiResponse

router = APIRouter()


async def _get_status_items(db: AsyncSession) -> dict[str, dict]:
    cat = (await db.execute(select(DictionaryCategory).where(DictionaryCategory.code == "ISSUE_STATUS"))).scalar_one()
    items = (await db.execute(select(DictionaryItem).where(DictionaryItem.category_id == cat.id))).scalars().all()
    return {str(i.id): {"code": i.code, "name": i.name, "color": i.color, "phase": (i.extra_config or {}).get("phase")} for i in items}


@router.get("/overview")
async def overview(db: AsyncSession = Depends(get_db), _user: User = Depends(get_current_user)):
    now = datetime.now(timezone.utc)
    today_start = now.replace(hour=0, minute=0, second=0, microsecond=0)

    status_map = await _get_status_items(db)
    closed_ids = [uid for uid, s in status_map.items() if s["phase"] == "closed"]

    today_new = (await db.execute(
        select(func.count()).select_from(Issue).where(Issue.submitted_at >= today_start)
    )).scalar()

    today_closed = 0
    if closed_ids:
        today_closed = (await db.execute(
            select(func.count()).select_from(Issue).where(
                Issue.status_id.in_([uuid.UUID(i) for i in closed_ids]),
                Issue.closed_at >= today_start,
            )
        )).scalar()

    today_changed_result = await db.execute(
        select(func.count(func.distinct(ChangeLog.entity_id))).where(
            ChangeLog.entity_type == "issue", ChangeLog.changed_at >= today_start,
        )
    )
    today_changed = today_changed_result.scalar()

    total_open = (await db.execute(
        select(func.count()).select_from(Issue).where(
            Issue.status_id.notin_([uuid.UUID(i) for i in closed_ids]) if closed_ids else True
        )
    )).scalar()

    two_days_ago = now - timedelta(days=2)
    stale_subq = (
        select(ChangeLog.entity_id, func.max(ChangeLog.changed_at).label("last_change"))
        .where(ChangeLog.entity_type == "issue")
        .group_by(ChangeLog.entity_id)
    ).subquery()

    stale_result = await db.execute(
        select(func.count()).select_from(Issue)
        .outerjoin(stale_subq, Issue.id == stale_subq.c.entity_id)
        .where(
            Issue.status_id.notin_([uuid.UUID(i) for i in closed_ids]) if closed_ids else True,
            (stale_subq.c.last_change < two_days_ago) | (stale_subq.c.last_change.is_(None)),
        )
    )
    total_stale = stale_result.scalar()

    return ApiResponse(data={
        "today_new": today_new, "today_closed": today_closed,
        "today_status_changed": today_changed, "total_open": total_open,
        "total_stale": total_stale,
    })


@router.get("/by-platform")
async def by_platform(db: AsyncSession = Depends(get_db), _user: User = Depends(get_current_user)):
    cat = (await db.execute(select(DictionaryCategory).where(DictionaryCategory.code == "PLATFORM"))).scalar_one()
    plats = (await db.execute(
        select(DictionaryItem).where(DictionaryItem.category_id == cat.id, DictionaryItem.is_active == True)
        .order_by(DictionaryItem.sort_order)
    )).scalars().all()

    status_map = await _get_status_items(db)
    closed_ids = [uuid.UUID(uid) for uid, s in status_map.items() if s["phase"] == "closed"]

    week_ago = datetime.now(timezone.utc) - timedelta(days=7)

    result = []
    for p in plats:
        total = (await db.execute(select(func.count()).select_from(Issue).where(Issue.platform_id == p.id))).scalar()
        open_count = (await db.execute(
            select(func.count()).select_from(Issue).where(
                Issue.platform_id == p.id,
                Issue.status_id.notin_(closed_ids) if closed_ids else True,
            )
        )).scalar()
        week_new = (await db.execute(
            select(func.count()).select_from(Issue).where(Issue.platform_id == p.id, Issue.submitted_at >= week_ago)
        )).scalar()
        week_closed = 0
        if closed_ids:
            week_closed = (await db.execute(
                select(func.count()).select_from(Issue).where(
                    Issue.platform_id == p.id, Issue.status_id.in_(closed_ids), Issue.closed_at >= week_ago,
                )
            )).scalar()

        status_dist_result = await db.execute(
            select(Issue.status_id, func.count()).where(Issue.platform_id == p.id).group_by(Issue.status_id)
        )
        status_dist = []
        for sid, cnt in status_dist_result:
            info = status_map.get(str(sid), {})
            status_dist.append({"name": info.get("name", "未知"), "color": info.get("color"), "count": cnt})

        result.append({
            "id": str(p.id), "name": p.name, "code": p.code, "color": p.color,
            "total": total, "open": open_count, "week_new": week_new, "week_closed": week_closed,
            "status_distribution": status_dist,
        })

    return ApiResponse(data=result)


@router.get("/by-person")
async def by_person(
    role_type: str = Query("dev", regex="^(product|dev|test)$"),
    db: AsyncSession = Depends(get_db), _user: User = Depends(get_current_user),
):
    status_map = await _get_status_items(db)
    closed_ids = [uuid.UUID(uid) for uid, s in status_map.items() if s["phase"] == "closed"]

    if role_type == "product":
        owner_col = Issue.product_owner_id
    elif role_type == "test":
        owner_col = Issue.test_owner_id
    else:
        owner_col = Issue.dev_owner_id

    person_counts = await db.execute(
        select(owner_col, Issue.status_id, func.count())
        .where(owner_col.isnot(None))
        .group_by(owner_col, Issue.status_id)
    )

    person_data: dict[str, dict] = {}
    for person_id, status_id, cnt in person_counts:
        pid = str(person_id)
        if pid not in person_data:
            user_result = await db.execute(select(User).where(User.id == person_id))
            u = user_result.scalar_one_or_none()
            person_data[pid] = {"id": pid, "name": u.name if u else "未知", "statuses": {}, "total": 0, "stale": 0}

        info = status_map.get(str(status_id), {})
        person_data[pid]["statuses"][info.get("name", "未知")] = cnt
        person_data[pid]["total"] += cnt

    return ApiResponse(data=list(person_data.values()))


@router.get("/trend")
async def trend(
    days: int = Query(30, ge=7, le=90),
    db: AsyncSession = Depends(get_db), _user: User = Depends(get_current_user),
):
    now = datetime.now(timezone.utc)
    status_map = await _get_status_items(db)
    closed_ids = [uuid.UUID(uid) for uid, s in status_map.items() if s["phase"] == "closed"]

    result = []
    for i in range(days, 0, -1):
        day = (now - timedelta(days=i)).date()
        day_start = datetime(day.year, day.month, day.day, tzinfo=timezone.utc)
        day_end = day_start + timedelta(days=1)

        new_count = (await db.execute(
            select(func.count()).select_from(Issue).where(Issue.submitted_at >= day_start, Issue.submitted_at < day_end)
        )).scalar()

        closed_count = 0
        if closed_ids:
            closed_count = (await db.execute(
                select(func.count()).select_from(Issue).where(
                    Issue.closed_at >= day_start, Issue.closed_at < day_end,
                    Issue.status_id.in_(closed_ids),
                )
            )).scalar()

        result.append({"date": day.isoformat(), "new": new_count, "closed": closed_count})

    return ApiResponse(data=result)


@router.get("/stale-issues")
async def stale_issues(
    threshold_days: int = Query(2, ge=1),
    db: AsyncSession = Depends(get_db), _user: User = Depends(get_current_user),
):
    status_map = await _get_status_items(db)
    closed_ids = [uuid.UUID(uid) for uid, s in status_map.items() if s["phase"] == "closed"]
    cutoff = datetime.now(timezone.utc) - timedelta(days=threshold_days)

    stale_subq = (
        select(ChangeLog.entity_id, func.max(ChangeLog.changed_at).label("last_change"))
        .where(ChangeLog.entity_type == "issue")
        .group_by(ChangeLog.entity_id)
    ).subquery()

    query = (
        select(Issue)
        .outerjoin(stale_subq, Issue.id == stale_subq.c.entity_id)
        .where(
            Issue.status_id.notin_(closed_ids) if closed_ids else True,
            (stale_subq.c.last_change < cutoff) | (stale_subq.c.last_change.is_(None)),
        )
        .order_by(Issue.submitted_at.desc())
        .limit(100)
    )

    result = await db.execute(query)
    issues = result.scalars().all()

    return ApiResponse(data=[{
        "id": str(i.id), "issue_no": i.issue_no, "title": i.title,
        "platform": i.platform.name if i.platform else None,
        "status": i.status.name if i.status else None,
        "submitter": i.submitter.name if i.submitter else None,
        "submitted_at": i.submitted_at.isoformat() if i.submitted_at else None,
    } for i in issues])
