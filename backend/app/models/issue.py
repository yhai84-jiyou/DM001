import uuid
from datetime import datetime, date, timezone

from sqlalchemy import String, ForeignKey, Index
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


class Issue(Base):
    __tablename__ = "issues"
    __table_args__ = (
        Index("idx_issues_status", "status_id"),
        Index("idx_issues_platform", "platform_id"),
        Index("idx_issues_submitter", "submitter_id"),
        Index("idx_issues_submitted_at", "submitted_at"),
    )

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    issue_no: Mapped[str] = mapped_column(String(30), unique=True)
    title: Mapped[str] = mapped_column(String(500))
    description: Mapped[str | None] = mapped_column()
    platform_id: Mapped[uuid.UUID] = mapped_column(ForeignKey("dictionary_items.id"))
    issue_type_id: Mapped[uuid.UUID] = mapped_column(ForeignKey("dictionary_items.id"))
    priority_id: Mapped[uuid.UUID | None] = mapped_column(ForeignKey("dictionary_items.id"))
    status_id: Mapped[uuid.UUID] = mapped_column(ForeignKey("dictionary_items.id"))
    submitter_id: Mapped[uuid.UUID] = mapped_column(ForeignKey("users.id"))
    submitted_at: Mapped[datetime] = mapped_column(default=lambda: datetime.now(timezone.utc))
    product_owner_id: Mapped[uuid.UUID | None] = mapped_column(ForeignKey("users.id"))
    dev_owner_id: Mapped[uuid.UUID | None] = mapped_column(ForeignKey("users.id"))
    test_owner_id: Mapped[uuid.UUID | None] = mapped_column(ForeignKey("users.id"))
    expected_date: Mapped[date | None] = mapped_column()
    resolved_at: Mapped[datetime | None] = mapped_column()
    closed_by: Mapped[uuid.UUID | None] = mapped_column(ForeignKey("users.id"))
    closed_at: Mapped[datetime | None] = mapped_column()
    created_at: Mapped[datetime] = mapped_column(default=lambda: datetime.now(timezone.utc))
    updated_at: Mapped[datetime] = mapped_column(
        default=lambda: datetime.now(timezone.utc),
        onupdate=lambda: datetime.now(timezone.utc),
    )

    platform: Mapped["DictionaryItem"] = relationship(foreign_keys=[platform_id], lazy="selectin")
    issue_type: Mapped["DictionaryItem"] = relationship(foreign_keys=[issue_type_id], lazy="selectin")
    priority: Mapped["DictionaryItem | None"] = relationship(foreign_keys=[priority_id], lazy="selectin")
    status: Mapped["DictionaryItem"] = relationship(foreign_keys=[status_id], lazy="selectin")
    submitter: Mapped["User"] = relationship(foreign_keys=[submitter_id], lazy="selectin")
    product_owner: Mapped["User | None"] = relationship(foreign_keys=[product_owner_id], lazy="selectin")
    dev_owner: Mapped["User | None"] = relationship(foreign_keys=[dev_owner_id], lazy="selectin")
    test_owner: Mapped["User | None"] = relationship(foreign_keys=[test_owner_id], lazy="selectin")
