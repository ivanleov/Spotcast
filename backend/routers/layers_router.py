from fastapi import APIRouter, Depends
from sqlalchemy import distinct, select
from sqlalchemy.ext.asyncio import AsyncSession

from ..auth import get_current_user
from ..database import get_db
from ..models import Capsule
from ..schemas import LayerResponse

router = APIRouter(prefix="/layers", tags=["layers"])

DEFAULT_LAYERS = ["personal", "work", "city", "logistics", "social"]


@router.get("", response_model=LayerResponse)
async def get_layers(
    user: dict = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(distinct(Capsule.layer)).where(Capsule.user_id == user["user_id"])
    )
    user_layers = [row[0] for row in result.all()]
    all_layers = sorted(set(DEFAULT_LAYERS + user_layers))
    return LayerResponse(layers=all_layers)
