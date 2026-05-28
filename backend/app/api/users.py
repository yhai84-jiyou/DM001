from fastapi import APIRouter

router = APIRouter(tags=["用户管理"])

@router.get("")
async def placeholder():
    return {"message": "TODO"}
