import os
import uuid
from fastapi import APIRouter, File, UploadFile, Form, Depends, HTTPException, Body
from sqlalchemy.orm import Session

from database import get_db
from models import PredictionRecord
from app.config import IMAGE_FOLDER, PUBLIC_BASE_URL
from app.model_service import ModelService

router = APIRouter()
model_service = ModelService()

os.makedirs(IMAGE_FOLDER, exist_ok=True)

@router.get("/")
def root():
    return {"message": "QuickDraw backend is running"}

@router.get("/health")
def health():
    return {"status": "ok", "model_loaded": True}

@router.post("/predict")
async def predict(
    file: UploadFile = File(...),
    session_id: str = Form(...),
    db: Session = Depends(get_db)
):
    image_bytes = await file.read()

    filename = f"{uuid.uuid4()}.png"
    filepath = os.path.join(IMAGE_FOLDER, filename)

    with open(filepath, "wb") as f:
        f.write(image_bytes)

    result = model_service.predict_from_bytes(image_bytes)

    if result is None:
        if os.path.exists(filepath):
            os.remove(filepath)
        raise HTTPException(
            status_code=400,
            detail="Drawing too small or unclear. Please draw more clearly."
        )

    second_choice = result["top3"][1]["label"] if len(result["top3"]) > 1 else None
    third_choice = result["top3"][2]["label"] if len(result["top3"]) > 2 else None

    record = PredictionRecord(
        session_id=session_id,
        predicted_label=result["predicted_label"],
        confidence=result["confidence"],
        second_choice=second_choice,
        third_choice=third_choice,
        image_path=filepath
    )

    db.add(record)
    db.commit()
    db.refresh(record)

    return {
        "id": record.id,
        "label": result["predicted_label"],
        "confidence": result["confidence"],
        "top3": result["top3"]
    }

@router.get("/predictions/{session_id}")
def get_predictions(session_id: str, db: Session = Depends(get_db)):
    results = (
        db.query(PredictionRecord)
        .filter(PredictionRecord.session_id == session_id)
        .order_by(PredictionRecord.created_at.desc())
        .all()
    )

    return [
        {
            "id": item.id,
            "label": item.predicted_label,
            "confidence": item.confidence,
            "imageUrl": f"{PUBLIC_BASE_URL}/{item.image_path.replace(os.sep, '/')}",
            "createdAt": item.created_at.isoformat() if item.created_at else None,
        }
        for item in results
    ]

@router.put("/predictions/{prediction_id}/label")
def update_prediction_label(
    prediction_id: int,
    new_label: str = Body(..., embed=True),
    db: Session = Depends(get_db)
):
    record = (
        db.query(PredictionRecord)
        .filter(PredictionRecord.id == prediction_id)
        .first()
    )

    if not record:
        raise HTTPException(status_code=404, detail="Prediction not found")

    record.predicted_label = new_label
    db.commit()
    db.refresh(record)

    return {
        "id": record.id,
        "label": record.predicted_label
    }

@router.delete("/predictions/{prediction_id}")
def delete_prediction(prediction_id: int, db: Session = Depends(get_db)):
    record = (
        db.query(PredictionRecord)
        .filter(PredictionRecord.id == prediction_id)
        .first()
    )

    if not record:
        raise HTTPException(status_code=404, detail="Prediction not found")

    if record.image_path and os.path.exists(record.image_path):
        os.remove(record.image_path)

    db.delete(record)
    db.commit()

    return {"message": "Prediction deleted successfully"}

@router.delete("/predictions/session/{session_id}")
def delete_all_predictions_for_session(session_id: str, db: Session = Depends(get_db)):
    records = (
        db.query(PredictionRecord)
        .filter(PredictionRecord.session_id == session_id)
        .all()
    )

    for record in records:
        if record.image_path and os.path.exists(record.image_path):
            os.remove(record.image_path)
        db.delete(record)

    db.commit()

    return {"message": f"Deleted {len(records)} predictions for session {session_id}"}