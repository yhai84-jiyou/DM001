from fastapi import APIRouter

router = APIRouter(tags=["附件"])

@router.get("")
async def placeholder():
    return {"message": "TODO"}
