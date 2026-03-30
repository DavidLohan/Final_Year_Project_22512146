import io
import os
import uuid

import numpy as np
from fastapi import FastAPI, File, UploadFile, Form, Depends, HTTPException, Body
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from PIL import Image, ImageOps
from scipy import ndimage
from sqlalchemy.orm import Session
from tensorflow import keras

from database import engine, Base, get_db
from models import PredictionRecord

app = FastAPI()

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
IMAGE_FOLDER = os.path.join(BASE_DIR, "saved_images")
os.makedirs(IMAGE_FOLDER, exist_ok=True)
app.mount("/saved_images", StaticFiles(directory=IMAGE_FOLDER), name="saved_images")
print("Serving images from:", IMAGE_FOLDER)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

Base.metadata.create_all(bind=engine)

MODEL_PATH = os.path.join("model", "quickdraw_model.keras")
LABELS_PATH = os.path.join("model", "labels.txt")
IMG_SIZE = 28

if not os.path.exists(MODEL_PATH):
    raise FileNotFoundError(f"Model file not found: {MODEL_PATH}")

if not os.path.exists(LABELS_PATH):
    raise FileNotFoundError(f"Labels file not found: {LABELS_PATH}")

model = keras.models.load_model(MODEL_PATH)

with open(LABELS_PATH, "r", encoding="utf-8") as f:
    labels = [line.strip() for line in f.readlines() if line.strip()]

model_output_size = model.output_shape[-1]
if len(labels) != model_output_size:
    raise ValueError(
        f"Label count ({len(labels)}) does not match model output size ({model_output_size})."
    )


def preprocess_image(image_bytes):
    img = Image.open(io.BytesIO(image_bytes)).convert("L")
    img = ImageOps.invert(img)

    arr = np.array(img)

    binary = arr > 20

    ink_pixels = np.sum(binary)
    if ink_pixels < 50:
        return None

    labeled, num_features = ndimage.label(binary)

    if num_features > 0:
        component_sizes = ndimage.sum(binary, labeled, range(1, num_features + 1))

        valid_components = [
            i + 1 for i, size in enumerate(component_sizes) if size >= 25
        ]

        if not valid_components:
            return None

        largest_component = max(
            valid_components,
            key=lambda comp_id: component_sizes[comp_id - 1]
        )

        mask = labeled == largest_component
        arr = arr * mask

    coords = np.argwhere(arr > 20)
    if coords.size == 0:
        return None

    y0, x0 = coords.min(axis=0)
    y1, x1 = coords.max(axis=0) + 1
    arr = arr[y0:y1, x0:x1]

    h, w = arr.shape
    size = max(h, w)
    square = np.zeros((size, size), dtype=np.uint8)

    y_offset = (size - h) // 2
    x_offset = (size - w) // 2
    square[y_offset:y_offset + h, x_offset:x_offset + w] = arr

    pad = 12
    square = np.pad(
        square,
        ((pad, pad), (pad, pad)),
        mode="constant",
        constant_values=0
    )

    img = Image.fromarray(square)
    img = img.resize((IMG_SIZE, IMG_SIZE))

    arr = np.array(img).astype("float32") / 255.0
    arr = arr.reshape(1, IMG_SIZE, IMG_SIZE, 1)

    return arr


@app.get("/")
def root():
    return {
        "message": "QuickDraw backend is running",
        "num_classes": len(labels),
        "classes": labels
    }


@app.post("/predict")
async def predict(
    file: UploadFile = File(...),
    session_id: str = Form(...),
    db: Session = Depends(get_db)
):
    session_id = session_id.strip()
    image_bytes = await file.read()

    if not image_bytes:
        raise HTTPException(status_code=400, detail="Empty file uploaded")

    filename = f"{uuid.uuid4()}.png"
    filepath = os.path.join(IMAGE_FOLDER, filename)

    with open(filepath, "wb") as f:
        f.write(image_bytes)

    processed = preprocess_image(image_bytes)

    if processed is None:
        if os.path.exists(filepath):
            os.remove(filepath)
        raise HTTPException(
            status_code=400,
            detail="Drawing too small or unclear. Please draw more clearly."
        )

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
            "imageUrl": f"/saved_images/{os.path.basename(item.image_path)}",
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