import os
from dotenv import load_dotenv

load_dotenv()

HOST = os.getenv("HOST", "0.0.0.0")
PORT = int(os.getenv("PORT", "8000"))

MODEL_PATH = os.getenv("MODEL_PATH", "model/quickdraw_weights.weights.h5")
LABELS_PATH = os.getenv("LABELS_PATH", "model/labels.txt")

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
IMAGE_FOLDER = os.path.join(BASE_DIR, "..", "saved_images")
IMAGE_FOLDER = os.path.abspath(IMAGE_FOLDER)
PUBLIC_BASE_URL = os.getenv("PUBLIC_BASE_URL", "http://127.0.0.1:8000")