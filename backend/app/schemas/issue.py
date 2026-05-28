from pydantic import BaseModel


class IssueCreate(BaseModel):
    title: str
    description: str | None = None
    platform_id: str
    issue_type_id: str
    priority_id: str | None = None


class IssueUpdate(BaseModel):
    title: str | None = None
    description: str | None = None
    platform_id: str | None = None
    issue_type_id: str | None = None
    priority_id: str | None = None
    product_owner_id: str | None = None
    dev_owner_id: str | None = None
    test_owner_id: str | None = None
    expected_date: str | None = None


class TransitionRequest(BaseModel):
    target_status_code: str


class CommentCreate(BaseModel):
    content: str
