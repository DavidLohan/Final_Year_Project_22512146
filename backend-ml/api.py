import io
import os
import uuid

import numpy as np
from fastapi import FastAPI, File, UploadFile, Form, Depends, HTTPException, Body
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image, ImageOps
from sqlalchemy.orm import Session
from tensorflow import keras

from database import engine, Base, get_db
from models import PredictionRecord
from fastapi.staticfiles import StaticFiles

app = FastAPI()

app.mount("/saved_images", StaticFiles(directory="saved_images"), name="saved_images")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

Base.metadata.create_all(bind=engine)

IMAGE_FOLDER = "saved_images"
os.makedirs(IMAGE_FOLDER, exist_ok=True)

model = keras.models.load_model("model/quickdraw_model.h5")

with open("model/labels.txt", "r") as f:
    labels = [line.strip() for line in f.readlines()]


def preprocess_image(image_bytes):
    img = Image.open(io.BytesIO(image_bytes)).convert("L")
    img = ImageOps.invert(img)

    arr = np.array(img)
    coords = np.argwhere(arr > 20)

    if coords.size > 0:
        y0, x0 = coords.min(axis=0)
        y1, x1 = coords.max(axis=0) + 1
        arr = arr[y0:y1, x0:x1]

        pad = 20
        arr = np.pad(arr, ((pad, pad), (pad, pad)), mode="constant", constant_values=0)

    img = Image.fromarray(arr)
    img = img.resize((28, 28))

    arr = np.array(img).astype("float32") / 255.0
    arr = arr.reshape(1, 28, 28, 1)

    return arr


@app.get("/")
def root():
    return {"message": "QuickDraw backend is running"}


@app.post("/predict")
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

    processed = preprocess_image(image_bytes)

    predictions = model.predict(processed, verbose=0)[0]
    predicted_index = int(np.argmax(predictions))
    confidence = float(predictions[predicted_index])

    top_3_indices = predictions.argsort()[-3:][::-1]
    top_3 = [
        {
            "label": labels[int(i)],
            "confidence": float(predictions[int(i)])
        }
        for i in top_3_indices
    ]

    second_choice = top_3[1]["label"] if len(top_3) > 1 else None
    third_choice = top_3[2]["label"] if len(top_3) > 2 else None

    record = PredictionRecord(
        session_id=session_id,
        predicted_label=labels[predicted_index],
        confidence=confidence,
        second_choice=second_choice,
        third_choice=third_choice,
        image_path=filepath
    )

    db.add(record)
    db.commit()
    db.refresh(record)

    print("Predictions:", predictions.tolist())
    print("Predicted label:", labels[predicted_index])
    print("Top 3:", top_3)

    return {
        "id": record.id,
        "label": labels[predicted_index],
        "confidence": confidence,
        "top3": top_3
    }


@app.get("/predictions/{session_id}")
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
            "imageUrl": f"http://127.0.0.1:8000/{item.image_path.replace(os.sep, '/')}",
            "createdAt": item.created_at.isoformat() if item.created_at else None,
        }
        for item in results
    ]
@app.put("/predictions/{prediction_id}/label")
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

@app.delete("/predictions/{prediction_id}")
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

@app.delete("/predictions/session/{session_id}")
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