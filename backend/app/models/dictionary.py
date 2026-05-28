import uuid
from datetime import datetime, timezone

from sqlalchemy import String, Integer, Boolean, ForeignKey, UniqueConstraint
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


class DictionaryCategory(Base):
    __tablename__ = "dictionary_categories"

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    name: Mapped[str] = mapped_column(String(100))
    code: Mapped[str] = mapped_column(String(50), unique=True)
    description: Mapped[str | None] = mapped_column(String(500))
    is_system: Mapped[bool] = mapped_column(Boolean, default=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)
    created_at: Mapped[datetime] = mapped_column(default=lambda: datetime.now(timezone.utc))

    items: Mapped[list["DictionaryItem"]] = relationship(back_populates="category", lazy="selectin")


class DictionaryItem(Base):
    __tablename__ = "dictionary_items"
    __table_args__ = (UniqueConstraint("category_id", "code", name="uq_dict_item_category_code"),)

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    category_id: Mapped[uuid.UUID] = mapped_column(ForeignKey("dictionary_categories.id"))
    name: Mapped[str] = mapped_column(String(100))
    code: Mapped[str] = mapped_column(String(50))
    color: Mapped[str | None] = mapped_column(String(20))
    icon: Mapped[str | None] = mapped_column(String(50))
    sort_order: Mapped[int] = mapped_column(Integer, default=0)
    is_default: Mapped[bool] = mapped_column(Boolean, default=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)
    is_system: Mapped[bool] = mapped_column(Boolean, default=False)
    extra_config: Mapped[dict | None] = mapped_column(JSONB)
    created_at: Mapped[datetime] = mapped_column(default=lambda: datetime.now(timezone.utc))

    category: Mapped["DictionaryCategory"] = relationship(back_populates="items", lazy="selectin")


class StatusTransition(Base):
    __tablename__ = "status_transitions"
    __table_args__ = (UniqueConstraint("from_status_id", "to_status_id", name="uq_status_transition"),)

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    from_status_id: Mapped[uuid.UUID] = mapped_column(ForeignKey("dictionary_items.id"))
    to_status_id: Mapped[uuid.UUID] = mapped_column(ForeignKey("dictionary_items.id"))
    allowed_roles: Mapped[list] = mapped_column(JSONB)
    special_rule: Mapped[str | None] = mapped_column(String(50))
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)

    from_status: Mapped["DictionaryItem"] = relationship(foreign_keys=[from_status_id], lazy="selectin")
    to_status: Mapped["DictionaryItem"] = relationship(foreign_keys=[to_status_id], lazy="selectin")
