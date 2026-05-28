from fastapi import APIRouter

router = APIRouter(tags=["组织管理"])

@router.get("")
async def placeholder():
    return {"message": "TODO"}
