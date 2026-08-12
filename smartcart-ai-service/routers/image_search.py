#
#   Author: Junior
#

from fastapi import APIRouter
from fastapi import UploadFile
from fastapi import File
from fastapi import HTTPException

from services.cnn_service import CNNService

router = APIRouter(
    prefix="/api",
    tags=["Image Search"]
)

cnn_service: CNNService | None = None


@router.post("/image-search")
async def image_search(
    image: UploadFile = File(...)
):
    if cnn_service is None:
        raise HTTPException(
            status_code=500,
            detail="CNN Service is not initialized."
        )

    image_bytes = await image.read()

    return cnn_service.search(image_bytes)

