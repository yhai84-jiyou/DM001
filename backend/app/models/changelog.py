import uuid
from datetime import datetime, timezone

from sqlalchemy import String, ForeignKey, Index
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


class ChangeLog(Base):
    __tablename__ = "change_logs"
    __table_args__ = (
        Index("idx_changelog_entity", "entity_type", "entity_id"),
        Index("idx_changelog_time", "changed_at"),
        Index("idx_changelog_person", "changed_by"),
    )

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    entity_type: Mapped[str] = mapped_column(String(50))
    entity_id: Mapped[uuid.UUID] = mapped_column()
    field_name: Mapped[str] = mapped_column(String(100))
    old_value: Mapped[str | None] = mapped_column()
    new_value: Mapped[str | None] = mapped_column()
    old_display: Mapped[str | None] = mapped_column()
    new_display: Mapped[str | None] = mapped_column()
    changed_by: Mapped[uuid.UUID] = mapped_column(ForeignKey("users.id"))
    changed_at: Mapped[datetime] = mapped_column(default=lambda: datetime.now(timezone.utc))
    change_source: Mapped[str] = mapped_column(String(20), default="MANUAL")

    changed_by_user: Mapped["User"] = relationship(lazy="selectin")
