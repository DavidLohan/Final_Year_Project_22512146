from fastapi import FastAPI, File, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image, ImageOps
import numpy as np
from tensorflow import keras
import io

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

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

    # Debug save let me see the image
    ## img.save("debug_processed.png")

    arr = np.array(img).astype("float32") / 255.0
    arr = arr.reshape(1, 28, 28, 1)

    return arr


@app.get("/")
def root():
    return {"message": "QuickDraw backend is running"}


@app.post("/predict")
async def predict(file: UploadFile = File(...)):
    image_bytes = await file.read()
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

    print("Predictions:", predictions.tolist())
    print("Predicted label:", labels[predicted_index])
    print("Top 3:", top_3)

    return {
        "label": labels[predicted_index],
        "confidence": confidence,
        "top3": top_3
    }