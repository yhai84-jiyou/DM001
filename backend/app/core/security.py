from datetime import datetime, timedelta, timezone

import jwt as pyjwt
import hashlib
import secrets

from app.config import get_settings

settings = get_settings()


def hash_password(password: str) -> str:
    salt = secrets.token_hex(16)
    hashed = hashlib.pbkdf2_hmac("sha256", password.encode(), salt.encode(), 100000).hex()
    return f"{salt}${hashed}"


def verify_password(plain: str, stored: str) -> bool:
    salt, hashed = stored.split("$", 1)
    check = hashlib.pbkdf2_hmac("sha256", plain.encode(), salt.encode(), 100000).hex()
    return secrets.compare_digest(hashed, check)


def create_access_token(user_id: str) -> str:
    expire = datetime.now(timezone.utc) + timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    return pyjwt.encode({"sub": user_id, "exp": expire, "type": "access"}, settings.JWT_SECRET, algorithm=settings.JWT_ALGORITHM)


def create_refresh_token(user_id: str) -> str:
    expire = datetime.now(timezone.utc) + timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS)
    return pyjwt.encode({"sub": user_id, "exp": expire, "type": "refresh"}, settings.JWT_SECRET, algorithm=settings.JWT_ALGORITHM)


def decode_token(token: str) -> dict | None:
    try:
        return pyjwt.decode(token, settings.JWT_SECRET, algorithms=[settings.JWT_ALGORITHM])
    except pyjwt.PyJWTError:
        return None
