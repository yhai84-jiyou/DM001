import uuid
from datetime import datetime, timezone

from sqlalchemy import String, ForeignKey, Integer
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


class Organization(Base):
    __tablename__ = "organizations"

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    name: Mapped[str] = mapped_column(String(200))
    org_type: Mapped[str] = mapped_column(String(20))
    parent_id: Mapped[uuid.UUID | None] = mapped_column(ForeignKey("organizations.id"))
    code: Mapped[str] = mapped_column(String(50), unique=True)
    status: Mapped[str] = mapped_column(String(20), default="ACTIVE")
    created_at: Mapped[datetime] = mapped_column(default=lambda: datetime.now(timezone.utc))

    parent: Mapped["Organization | None"] = relationship(remote_side=[id], lazy="selectin")
    departments: Mapped[list["Department"]] = relationship(back_populates="organization", lazy="selectin")


class Department(Base):
    __tablename__ = "departments"

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    name: Mapped[str] = mapped_column(String(200))
    org_id: Mapped[uuid.UUID] = mapped_column(ForeignKey("organizations.id"))
    parent_id: Mapped[uuid.UUID | None] = mapped_column(ForeignKey("departments.id"))
    sort_order: Mapped[int] = mapped_column(Integer, default=0)

    organization: Mapped["Organization"] = relationship(back_populates="departments", lazy="selectin")
    parent: Mapped["Department | None"] = relationship(remote_side=[id], lazy="selectin")
