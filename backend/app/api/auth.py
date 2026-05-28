from fastapi import APIRouter

router = APIRouter(tags=["认证"])

@router.get("")
async def placeholder():
    return {"message": "TODO"}
