from fastapi import APIRouter

router = APIRouter(tags=["数据字典"])

@router.get("")
async def placeholder():
    return {"message": "TODO"}
