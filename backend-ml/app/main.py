from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from database import engine, Base
from app.config import IMAGE_FOLDER
from app.routes_predictions import router as predictions_router

app = FastAPI(title="QuickDraw Backend")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

Base.metadata.create_all(bind=engine)

app.mount("/saved_images", StaticFiles(directory=IMAGE_FOLDER), name="saved_images")

app.include_router(predictions_router)