import uuid
from datetime import datetime, timezone

from sqlalchemy import String, ForeignKey
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


class User(Base):
    __tablename__ = "users"

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    name: Mapped[str] = mapped_column(String(100))
    phone: Mapped[str] = mapped_column(String(20), unique=True)
    email: Mapped[str | None] = mapped_column(String(200))
    password_hash: Mapped[str] = mapped_column(String(256))
    org_id: Mapped[uuid.UUID] = mapped_column(ForeignKey("organizations.id"))
    dept_id: Mapped[uuid.UUID | None] = mapped_column(ForeignKey("departments.id"))
    user_type: Mapped[str] = mapped_column(String(20))
    vendor_role: Mapped[str | None] = mapped_column(String(20))
    status: Mapped[str] = mapped_column(String(20), default="PENDING")
    created_at: Mapped[datetime] = mapped_column(default=lambda: datetime.now(timezone.utc))
    approved_by: Mapped[uuid.UUID | None] = mapped_column(ForeignKey("users.id"))
    approved_at: Mapped[datetime | None] = mapped_column()

    organization: Mapped["Organization"] = relationship(lazy="selectin", foreign_keys=[org_id])
    roles: Mapped[list["Role"]] = relationship(secondary="user_roles", lazy="selectin")
