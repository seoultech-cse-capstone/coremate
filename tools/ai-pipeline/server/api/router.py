from fastapi import APIRouter

from server.api.routes import content_import, health, pipeline, review, weakness

api_router = APIRouter()
api_router.include_router(health.router, tags=["health"])
api_router.include_router(pipeline.router, prefix="/pipeline", tags=["pipeline"])
api_router.include_router(review.router, prefix="/reviews", tags=["reviews"])
api_router.include_router(content_import.router, prefix="/imports", tags=["imports"])
api_router.include_router(weakness.router, prefix="/weakness", tags=["weakness"])
