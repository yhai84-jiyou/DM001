from datetime import datetime, timezone

from sqlalchemy import event, inspect

from app.models.issue import Issue
from app.models.changelog import ChangeLog
from app.core.dependencies import current_user_id_var

DISPLAY_NAMES = {
    "title": "标题", "description": "描述", "platform_id": "平台",
    "issue_type_id": "问题类型", "priority_id": "优先级", "status_id": "状态",
    "product_owner_id": "产品跟进人", "dev_owner_id": "研发跟进人",
    "test_owner_id": "测试跟进人", "expected_date": "期望解决日期",
}

SKIP_FIELDS = {"updated_at", "created_at"}


def _get_display(instance, field_name, value):
    if value is None:
        return None
    if field_name.endswith("_id"):
        rel_name = field_name[:-3]
        rel_obj = getattr(instance, rel_name, None)
        if rel_obj and hasattr(rel_obj, "name"):
            return rel_obj.name
    return str(value)


@event.listens_for(Issue, "after_update")
def issue_after_update(mapper, connection, target):
    state = inspect(target)
    user_id = current_user_id_var.get()
    if not user_id:
        return

    for attr in state.attrs:
        if attr.key in SKIP_FIELDS:
            continue
        hist = attr.history
        if not hist.has_changes():
            continue

        old_val = hist.deleted[0] if hist.deleted else None
        new_val = hist.added[0] if hist.added else None

        if old_val == new_val:
            continue

        connection.execute(
            ChangeLog.__table__.insert().values(
                entity_type="issue",
                entity_id=target.id,
                field_name=attr.key,
                old_value=str(old_val) if old_val is not None else None,
                new_value=str(new_val) if new_val is not None else None,
                old_display=DISPLAY_NAMES.get(attr.key, attr.key),
                new_display=DISPLAY_NAMES.get(attr.key, attr.key),
                changed_by=user_id,
                changed_at=datetime.now(timezone.utc),
                change_source="MANUAL",
            )
        )


def register_hooks():
    pass
