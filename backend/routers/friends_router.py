from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import or_, select
from sqlalchemy.ext.asyncio import AsyncSession

from ..auth import get_current_user
from ..database import get_db
from ..models import Friendship, User
from ..schemas import (
    FriendActionRequest,
    FriendRequest,
    FriendshipResponse,
    UserInfo,
    UserSearchResponse,
)

router = APIRouter(prefix="/friends", tags=["friends"])



@router.get("/search", response_model=UserSearchResponse)
async def search_users(
    q: str,
    user: dict = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    if len(q) < 2:
        return UserSearchResponse(users=[])

    result = await db.execute(
        select(User)
        .where(User.username.ilike(f"%{q}%"))
        .where(User.id != user["user_id"])
        .limit(20)
    )
    users = result.scalars().all()
    return UserSearchResponse(
        users=[UserInfo(id=u.id, username=u.username, created_at=u.created_at) for u in users]
    )



@router.post("/request", response_model=FriendshipResponse)
async def send_friend_request(
    req: FriendRequest,
    user: dict = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(select(User).where(User.username == req.username))
    target = result.scalar_one_or_none()
    if not target:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "User not found")
    if target.id == user["user_id"]:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "Cannot add yourself")

    result = await db.execute(
        select(Friendship).where(
            or_(
                (Friendship.user_id == user["user_id"]) & (Friendship.friend_id == target.id),
                (Friendship.user_id == target.id) & (Friendship.friend_id == user["user_id"]),
            )
        )
    )
    existing = result.scalar_one_or_none()
    if existing:
        if existing.status == "accepted":
            raise HTTPException(status.HTTP_409_CONFLICT, "Already friends")
        if existing.status == "pending":
            raise HTTPException(status.HTTP_409_CONFLICT, "Request already pending")

    friendship = Friendship(
        user_id=user["user_id"],
        friend_id=target.id,
        status="pending",
    )
    db.add(friendship)
    await db.commit()
    await db.refresh(friendship)

    return FriendshipResponse(
        id=friendship.id,
        user_id=friendship.user_id,
        friend_id=friendship.friend_id,
        friend_username=target.username,
        status=friendship.status,
        created_at=friendship.created_at,
    )



@router.post("/accept", response_model=FriendshipResponse)
async def accept_friend(
    req: FriendActionRequest,
    user: dict = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(Friendship).where(
            Friendship.id == req.friendship_id,
            Friendship.friend_id == user["user_id"],
            Friendship.status == "pending",
        )
    )
    friendship = result.scalar_one_or_none()
    if not friendship:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Request not found")

    friendship.status = "accepted"
    await db.commit()
    await db.refresh(friendship)

    result = await db.execute(select(User).where(User.id == friendship.user_id))
    friend_user = result.scalar_one()

    return FriendshipResponse(
        id=friendship.id,
        user_id=friendship.user_id,
        friend_id=friendship.friend_id,
        friend_username=friend_user.username,
        status=friendship.status,
        created_at=friendship.created_at,
    )



@router.post("/reject")
async def reject_friend(
    req: FriendActionRequest,
    user: dict = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(Friendship).where(
            Friendship.id == req.friendship_id,
            or_(
                Friendship.user_id == user["user_id"],
                Friendship.friend_id == user["user_id"],
            ),
        )
    )
    friendship = result.scalar_one_or_none()
    if not friendship:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Request not found")

    await db.delete(friendship)
    await db.commit()
    return {"status": "removed"}



@router.get("/list", response_model=list[FriendshipResponse])
async def list_friends(
    user: dict = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(Friendship).where(
            or_(
                Friendship.user_id == user["user_id"],
                Friendship.friend_id == user["user_id"],
            )
        )
    )
    friendships = result.scalars().all()

    responses = []
    for f in friendships:
        other_id = f.friend_id if f.user_id == user["user_id"] else f.user_id
        result = await db.execute(select(User).where(User.id == other_id))
        other_user = result.scalar_one()
        responses.append(FriendshipResponse(
            id=f.id,
            user_id=f.user_id,
            friend_id=f.friend_id,
            friend_username=other_user.username,
            status=f.status,
            created_at=f.created_at,
        ))
    return responses



@router.get("/pending", response_model=list[FriendshipResponse])
async def pending_requests(
    user: dict = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(Friendship).where(
            Friendship.friend_id == user["user_id"],
            Friendship.status == "pending",
        )
    )
    friendships = result.scalars().all()

    responses = []
    for f in friendships:
        result = await db.execute(select(User).where(User.id == f.user_id))
        sender = result.scalar_one()
        responses.append(FriendshipResponse(
            id=f.id,
            user_id=f.user_id,
            friend_id=f.friend_id,
            friend_username=sender.username,
            status=f.status,
            created_at=f.created_at,
        ))
    return responses
