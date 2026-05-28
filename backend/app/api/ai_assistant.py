from fastapi import APIRouter

router = APIRouter(tags=["AI助手"])

@router.get("")
async def placeholder():
    return {"message": "TODO"}
