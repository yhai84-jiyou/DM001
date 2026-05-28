from fastapi import APIRouter

router = APIRouter(tags=["变更日志"])

@router.get("")
async def placeholder():
    return {"message": "TODO"}
