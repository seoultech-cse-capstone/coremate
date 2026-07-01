from fastapi import APIRouter

from server.api.routes import health, pipeline, review

api_router = APIRouter()
api_router.include_router(health.router, tags=["health"])
api_router.include_router(pipeline.router, prefix="/pipeline", tags=["pipeline"])
api_router.include_router(review.router, prefix="/reviews", tags=["reviews"])
