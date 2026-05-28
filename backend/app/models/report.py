import uuid
from datetime import datetime, date, timezone

from sqlalchemy import String, Date
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import Mapped, mapped_column

from app.database import Base


class DailyReport(Base):
    __tablename__ = "daily_reports"

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    report_date: Mapped[date] = mapped_column(Date)
    report_type: Mapped[str] = mapped_column(String(20))
    summary_json: Mapped[dict] = mapped_column(JSONB)
    ai_narrative: Mapped[str | None] = mapped_column()
    generated_at: Mapped[datetime] = mapped_column(default=lambda: datetime.now(timezone.utc))
