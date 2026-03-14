from __future__ import annotations

from datetime import datetime
from enum import Enum
from typing import Optional

from pydantic import BaseModel, Field



class CapsuleType(str, Enum):
    TEXT = "TEXT"
    AUDIO = "AUDIO"



class RegisterRequest(BaseModel):
    username: str = Field(min_length=3, max_length=100)
    password: str = Field(min_length=6, max_length=128)


class LoginRequest(BaseModel):
    username: str
    password: str


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"



class UserInfo(BaseModel):
    id: int
    username: str
    created_at: datetime



class FriendRequest(BaseModel):
    username: str


class FriendActionRequest(BaseModel):
    friendship_id: int


class FriendshipResponse(BaseModel):
    id: int
    user_id: int
    friend_id: int
    friend_username: str
    status: str
    created_at: datetime


class UserSearchResponse(BaseModel):
    users: list[UserInfo]



class CapsuleCreate(BaseModel):
    latitude: float = Field(ge=-90, le=90)
    longitude: float = Field(ge=-180, le=180)
    radius: float = Field(default=50.0, gt=0, le=5000)
    capsule_type: CapsuleType
    text_content: Optional[str] = None
    layer: str = Field(default="personal", max_length=50)
    ttl_hours: Optional[int] = Field(default=None, gt=0)
    recipient_username: Optional[str] = None


class CapsuleResponse(BaseModel):
    id: int
    latitude: float
    longitude: float
    radius: float
    capsule_type: str
    text_content: Optional[str] = None
    audio_url: Optional[str] = None
    layer: str
    is_active: bool
    is_completed: bool
    created_at: datetime
    owner_username: Optional[str] = None
    recipient_username: Optional[str] = None


class NearbyRequest(BaseModel):
    latitude: float = Field(ge=-90, le=90)
    longitude: float = Field(ge=-180, le=180)
    radius: float = Field(default=500.0, gt=0, le=10000)
    layers: Optional[list[str]] = None


class CompleteRequest(BaseModel):
    capsule_id: int



class LayerResponse(BaseModel):
    layers: list[str]
