from fastapi import FastAPI

from server.api.router import api_router


def create_app() -> FastAPI:
    app = FastAPI(
        title="CoreMate AI Pipeline Server",
        description="Internal server for running CoreMate AI content pipeline jobs.",
        version="0.1.0",
    )
    app.include_router(api_router, prefix="/api/v1")
    return app


app = create_app()

