import os
import numpy as np
from tensorflow import keras

from app.config import MODEL_PATH, LABELS_PATH
from app.image_utils import preprocess_image


class ModelService:
    def __init__(self):
        print("MODEL_PATH =", MODEL_PATH)
        print("LABELS_PATH =", LABELS_PATH)

        if not os.path.exists(MODEL_PATH):
            raise FileNotFoundError(f"Model file not found: {MODEL_PATH}")

        if not os.path.exists(LABELS_PATH):
            raise FileNotFoundError(f"Labels file not found: {LABELS_PATH}")

        with open(LABELS_PATH, "r", encoding="utf-8") as f:
            self.labels = [line.strip() for line in f.readlines() if line.strip()]

        print("Loaded labels:", len(self.labels))

        self.model = keras.models.load_model(MODEL_PATH, compile=False)
        print("Model loaded successfully.")

    def predict_from_bytes(self, image_bytes):
        processed = preprocess_image(image_bytes)

        if processed is None:
            return None

        predictions = self.model.predict(processed, verbose=0)[0]
        predicted_index = int(np.argmax(predictions))
        confidence = float(predictions[predicted_index])

        top_3_indices = predictions.argsort()[-3:][::-1]
        top_3 = [
            {
                "label": self.labels[int(i)],
                "confidence": float(predictions[int(i)])
            }
            for i in top_3_indices
        ]

        return {
            "predicted_label": self.labels[predicted_index],
            "confidence": confidence,
            "top3": top_3
        }