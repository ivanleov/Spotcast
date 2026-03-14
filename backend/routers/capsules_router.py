from __future__ import annotations

import os
import uuid
from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile, status
from geoalchemy2.functions import ST_X, ST_Y
from sqlalchemy import select, text
from sqlalchemy.ext.asyncio import AsyncSession

from ..auth import get_current_user
from ..config import AUDIO_UPLOAD_DIR
from ..database import get_db
from ..models import Capsule, Friendship, User
from ..schemas import (
    CapsuleCreate,
    CapsuleResponse,
    CompleteRequest,
    NearbyRequest,
)

router = APIRouter(prefix="/capsules", tags=["capsules"])



def _row_to_response(row) -> dict:
    return dict(
        id=row.id,
        latitude=row.lat,
        longitude=row.lon,
        radius=row.radius,
        capsule_type=row.capsule_type,
        text_content=row.text_content,
        audio_url=f"/audio/{os.path.basename(row.audio_path)}" if row.audio_path else None,
        layer=row.layer,
        is_active=row.is_active,
        is_completed=row.is_completed,
        created_at=row.created_at,
        owner_username=getattr(row, "owner_username", None),
        recipient_username=getattr(row, "recipient_username", None),
    )


async def _fetch_capsule(db: AsyncSession, capsule_id: int):
    result = await db.execute(
        select(
            Capsule.id,
            ST_Y(Capsule.location).label("lat"),
            ST_X(Capsule.location).label("lon"),
            Capsule.radius,
            Capsule.capsule_type,
            Capsule.text_content,
            Capsule.audio_path,
            Capsule.layer,
            Capsule.is_active,
            Capsule.is_completed,
            Capsule.created_at,
        ).where(Capsule.id == capsule_id)
    )
    return result.one()



@router.post("/create", response_model=CapsuleResponse)
async def create_capsule(
    req: CapsuleCreate,
    user: dict = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    ttl = None
    if req.ttl_hours:
        ttl = datetime.now(timezone.utc) + timedelta(hours=req.ttl_hours)

    recipient_id = None
    recipient_username = None
    if req.recipient_username:
        result = await db.execute(select(User).where(User.username == req.recipient_username))
        recipient = result.scalar_one_or_none()
        if not recipient:
            from fastapi import HTTPException as _H
            raise _H(status_code=404, detail="Recipient user not found")
        recipient_id = recipient.id
        recipient_username = recipient.username

    point_wkt = f"SRID=4326;POINT({req.longitude} {req.latitude})"
    capsule = Capsule(
        user_id=user["user_id"],
        recipient_id=recipient_id,
        location=point_wkt,
        radius=req.radius,
        capsule_type=req.capsule_type.value,
        text_content=req.text_content,
        layer=req.layer,
        ttl=ttl,
    )
    db.add(capsule)
    await db.commit()
    await db.refresh(capsule)

    row = await _fetch_capsule(db, capsule.id)
    resp = CapsuleResponse(**_row_to_response(row))
    resp.owner_username = user["username"]
    resp.recipient_username = recipient_username
    return resp



@router.post("/create_audio", response_model=CapsuleResponse)
async def create_audio_capsule(
    latitude: float = Form(...),
    longitude: float = Form(...),
    radius: float = Form(50.0),
    layer: str = Form("personal"),
    ttl_hours: int | None = Form(None),
    audio: UploadFile = File(...),
    user: dict = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    filename = f"{uuid.uuid4().hex}_{audio.filename}"
    filepath = os.path.join(AUDIO_UPLOAD_DIR, filename)
    with open(filepath, "wb") as f:
        f.write(await audio.read())

    ttl = None
    if ttl_hours:
        ttl = datetime.now(timezone.utc) + timedelta(hours=ttl_hours)

    point_wkt = f"SRID=4326;POINT({longitude} {latitude})"
    capsule = Capsule(
        user_id=user["user_id"],
        location=point_wkt,
        radius=radius,
        capsule_type="AUDIO",
        audio_path=filepath,
        layer=layer,
        ttl=ttl,
    )
    db.add(capsule)
    await db.commit()
    await db.refresh(capsule)

    row = await _fetch_capsule(db, capsule.id)
    return CapsuleResponse(**_row_to_response(row))



@router.post("/nearby", response_model=list[CapsuleResponse])
async def get_nearby(
    req: NearbyRequest,
    user: dict = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    base_query = """
        SELECT c.id,
               ST_Y(c.location::geometry) AS lat,
               ST_X(c.location::geometry) AS lon,
               c.radius, c.capsule_type, c.text_content, c.audio_path,
               c.layer, c.is_active, c.is_completed, c.created_at,
               u.username AS owner_username,
               r.username AS recipient_username
        FROM capsules c
        JOIN users u ON u.id = c.user_id
        LEFT JOIN users r ON r.id = c.recipient_id
        WHERE c.is_active = true
          AND c.is_completed = false
          AND (c.ttl IS NULL OR c.ttl > NOW())
          AND (
                -- свои капсулы всегда видны
                c.user_id = :user_id
                -- капсулы мне всегда видны
                OR c.recipient_id = :user_id
                -- чужие в радиусе
                OR (
                    c.recipient_id IS NULL
                    AND c.layer != 'personal'
                    AND ST_DWithin(
                          c.location::geography,
                          ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                          :radius
                    )
                )
              )
    """
    params: dict = dict(
        user_id=user["user_id"],
        lat=req.latitude,
        lon=req.longitude,
        radius=req.radius,
    )

    if req.layers:
        placeholders = ", ".join(f":layer_{i}" for i in range(len(req.layers)))
        base_query += f" AND c.layer IN ({placeholders})"
        for i, lyr in enumerate(req.layers):
            params[f"layer_{i}"] = lyr

    result = await db.execute(text(base_query), params)
    return [CapsuleResponse(**_row_to_response(row)) for row in result.all()]



@router.get("/my", response_model=list[CapsuleResponse])
async def get_my_capsules(
    user: dict = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    query = """
        SELECT c.id,
               ST_Y(c.location::geometry) AS lat,
               ST_X(c.location::geometry) AS lon,
               c.radius, c.capsule_type, c.text_content, c.audio_path,
               c.layer, c.is_active, c.is_completed, c.created_at,
               u.username AS owner_username,
               r.username AS recipient_username
        FROM capsules c
        JOIN users u ON u.id = c.user_id
        LEFT JOIN users r ON r.id = c.recipient_id
        WHERE (c.user_id = :user_id OR c.recipient_id = :user_id)
        ORDER BY c.created_at DESC
    """
    result = await db.execute(text(query), {"user_id": user["user_id"]})
    return [CapsuleResponse(**_row_to_response(row)) for row in result.all()]



@router.post("/complete")
async def complete_capsule(
    req: CompleteRequest,
    user: dict = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    from sqlalchemy import or_
    result = await db.execute(
        select(Capsule).where(
            Capsule.id == req.capsule_id,
            or_(
                Capsule.user_id == user["user_id"],
                Capsule.recipient_id == user["user_id"],
            ),
        )
    )
    capsule = result.scalar_one_or_none()
    if not capsule:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Capsule not found")

    capsule.is_completed = True
    await db.commit()
    return {"status": "completed", "capsule_id": req.capsule_id}
