from fastapi import APIRouter

router = APIRouter(tags=["问题管理"])

@router.get("")
async def placeholder():
    return {"message": "TODO"}
