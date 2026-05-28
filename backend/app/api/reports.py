from fastapi import APIRouter

router = APIRouter(tags=["统计报表"])

@router.get("")
async def placeholder():
    return {"message": "TODO"}
