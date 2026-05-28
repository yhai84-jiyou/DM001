import uuid
from datetime import datetime, timezone

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.dictionary import DictionaryCategory, DictionaryItem, StatusTransition
from app.models.issue import Issue
from app.models.user import User
from app.core.exceptions import StateTransitionError

_transition_cache: dict | None = None


async def load_transition_matrix(db: AsyncSession) -> dict:
    global _transition_cache
    if _transition_cache is not None:
        return _transition_cache

    result = await db.execute(select(StatusTransition).where(StatusTransition.is_active == True))
    transitions = result.scalars().all()

    matrix = {}
    for t in transitions:
        from_code = t.from_status.code
        to_code = t.to_status.code
        matrix.setdefault(from_code, {})[to_code] = {
            "to_status_id": str(t.to_status.id),
            "to_status_name": t.to_status.name,
            "to_status_color": t.to_status.color,
            "allowed_roles": t.allowed_roles,
            "special_rule": t.special_rule,
        }

    _transition_cache = matrix
    return matrix


def invalidate_transition_cache():
    global _transition_cache
    _transition_cache = None


async def get_available_transitions(issue: Issue, current_user: User, db: AsyncSession) -> list[dict]:
    matrix = await load_transition_matrix(db)
    current_code = issue.status.code
    transitions = matrix.get(current_code, {})

    available = []
    user_roles = {r.code for r in current_user.roles}

    for to_code, rule in transitions.items():
        if rule["special_rule"] == "SUBMITTER_ONLY":
            if current_user.id == issue.submitter_id:
                available.append({
                    "code": to_code,
                    "name": rule["to_status_name"],
                    "color": rule["to_status_color"],
                })
        elif user_roles & set(rule["allowed_roles"]):
            available.append({
                "code": to_code,
                "name": rule["to_status_name"],
                "color": rule["to_status_color"],
            })

    return available


async def transition(issue: Issue, target_status_code: str, current_user: User, db: AsyncSession):
    matrix = await load_transition_matrix(db)
    current_code = issue.status.code
    transitions = matrix.get(current_code, {})

    rule = transitions.get(target_status_code)
    if not rule:
        raise StateTransitionError(f"不允许从「{issue.status.name}」流转到「{target_status_code}」")

    if rule["special_rule"] == "SUBMITTER_ONLY":
        if current_user.id != issue.submitter_id:
            raise StateTransitionError("只有问题提交人可以执行此操作")
    else:
        user_roles = {r.code for r in current_user.roles}
        if not user_roles & set(rule["allowed_roles"]):
            raise StateTransitionError("当前角色无权执行此状态流转")

    target_status_id = uuid.UUID(rule["to_status_id"])
    issue.status_id = target_status_id

    result = await db.execute(select(DictionaryItem).where(DictionaryItem.id == target_status_id))
    new_status = result.scalar_one()
    issue.status = new_status

    if target_status_code == "RESOLVED":
        issue.resolved_at = datetime.now(timezone.utc)
        issue.closed_by = current_user.id
        issue.closed_at = datetime.now(timezone.utc)
    elif target_status_code == "UNRESOLVED":
        issue.resolved_at = None
        issue.closed_by = None
        issue.closed_at = None
