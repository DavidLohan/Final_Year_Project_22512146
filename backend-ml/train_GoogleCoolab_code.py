"""
import os
import io
import urllib.request
import urllib.parse
import numpy as np
import matplotlib.pyplot as plt

from tensorflow import keras
from tensorflow.keras import layers
from sklearn.model_selection import train_test_split
from sklearn.metrics import confusion_matrix, ConfusionMatrixDisplay, classification_report

from google.colab import drive
drive.mount('/content/drive')

BASE_URL = "https://storage.googleapis.com/quickdraw_dataset/full/numpy_bitmap/"

CLASSES = [
    "apple",
    "banana",
    "bed",
    "bird",
    "book",
    "bus",
    "car",
    "cat",
    "chair",
    "dog",
    "flower",
    "key",
    "mug",
    "umbrella",
    "tree",
    "fish",
    "star",
    "house",
    "bicycle",
    "clock",
    "sun",
    "cup",
    "pencil",
    "baseball",
    "airplane",
    "backpack",
    "candle",
    "camera",
    "door",
    "envelope",
    "guitar",
    "hammer",
    "hat",
    "leaf",
    "lightning",
    "moon",
    "sandwich",
    "shoe",
    "table",
    "toothbrush"
]

IMG_SIZE = 28
SAMPLES_PER_CLASS = 10000
EPOCHS = 10
BATCH_SIZE = 128

MODEL_DIR = "/content/model"
DRIVE_DIR = "/content/drive/MyDrive/quickdraw_project"

os.makedirs(MODEL_DIR, exist_ok=True)
os.makedirs(DRIVE_DIR, exist_ok=True)

def load_data():
    x_list = []
    y_list = []

    for idx, class_name in enumerate(CLASSES):
        print(f"Loading {class_name}...")

        encoded_name = urllib.parse.quote(class_name)
        url = BASE_URL + encoded_name + ".npy"

        response = urllib.request.urlopen(url)
        data = np.load(io.BytesIO(response.read()))

        data = data[:SAMPLES_PER_CLASS]

        x_list.append(data)
        y_list.append(np.full(data.shape[0], idx))

        print(f"Loaded {data.shape[0]} samples for class: {class_name}")

    x = np.concatenate(x_list, axis=0)
    y = np.concatenate(y_list, axis=0)

    return x, y

print("Loading dataset...")
X, y = load_data()

print("Preprocessing...")
X = X.astype("float32") / 255.0
X = X.reshape(-1, IMG_SIZE, IMG_SIZE, 1)

print(f"Total dataset shape: {X.shape}")
print(f"Labels shape: {y.shape}")

print("Splitting dataset...")
X_train, X_test, y_train, y_test = train_test_split(
    X,
    y,
    test_size=0.2,
    random_state=42,
    stratify=y
)

print(f"Train shape: {X_train.shape}")
print(f"Test shape: {X_test.shape}")

print("Building improved CNN model...")

model = keras.Sequential([
    layers.Input(shape=(IMG_SIZE, IMG_SIZE, 1)),

    layers.Conv2D(32, (3, 3), activation="relu", padding="same"),
    layers.BatchNormalization(),
    layers.MaxPooling2D((2, 2)),

    layers.Conv2D(64, (3, 3), activation="relu", padding="same"),
    layers.BatchNormalization(),
    layers.MaxPooling2D((2, 2)),

    layers.Conv2D(128, (3, 3), activation="relu", padding="same"),
    layers.BatchNormalization(),

    layers.Flatten(),

    layers.Dense(256, activation="relu"),
    layers.Dropout(0.4),

    layers.Dense(128, activation="relu"),
    layers.Dropout(0.3),

    layers.Dense(len(CLASSES), activation="softmax")
])

model.compile(
    optimizer="adam",
    loss="sparse_categorical_crossentropy",
    metrics=["accuracy"]
)

model.summary()

early_stopping = keras.callbacks.EarlyStopping(
    monitor="val_loss",
    patience=2,
    restore_best_weights=True
)

print("Training model...")
history = model.fit(
    X_train,
    y_train,
    validation_data=(X_test, y_test),
    epochs=EPOCHS,
    batch_size=BATCH_SIZE,
    callbacks=[early_stopping]
)

print("Evaluating model...")
loss, accuracy = model.evaluate(X_test, y_test, verbose=0)
print(f"Test Loss: {loss:.4f}")
print(f"Test Accuracy: {accuracy:.4f}")


plt.figure(figsize=(8, 5))
plt.plot(history.history["accuracy"], label="Training Accuracy")
plt.plot(history.history["val_accuracy"], label="Validation Accuracy")
plt.xlabel("Epoch")
plt.ylabel("Accuracy")
plt.title("Training vs Validation Accuracy")
plt.legend()
plt.show()

plt.figure(figsize=(8, 5))
plt.plot(history.history["loss"], label="Training Loss")
plt.plot(history.history["val_loss"], label="Validation Loss")
plt.xlabel("Epoch")
plt.ylabel("Loss")
plt.title("Training vs Validation Loss")
plt.legend()
plt.show()

print("Generating predictions...")
y_pred_probs = model.predict(X_test)
y_pred = np.argmax(y_pred_probs, axis=1)

cm = confusion_matrix(y_test, y_pred)

fig, ax = plt.subplots(figsize=(16, 16))
disp = ConfusionMatrixDisplay(confusion_matrix=cm, display_labels=CLASSES)
disp.plot(ax=ax, xticks_rotation=90, cmap="Blues", colorbar=False)
plt.title("Confusion Matrix")
plt.show()

print("Classification Report:")
print(classification_report(y_test, y_pred, target_names=CLASSES))

model_path = os.path.join(MODEL_DIR, "quickdraw_model.keras")
labels_path = os.path.join(MODEL_DIR, "labels.txt")

model.save(model_path)

with open(labels_path, "w", encoding="utf-8") as f:
    for label in CLASSES:
        f.write(label + "\n")

print("Model and labels saved successfully in Colab.")
print("Model path:", model_path)
print("Labels path:", labels_path)

drive_model_path = os.path.join(DRIVE_DIR, "quickdraw_model.keras")
drive_labels_path = os.path.join(DRIVE_DIR, "labels.txt")

model.save(drive_model_path)

with open(drive_labels_path, "w") as f:
    for label in CLASSES:
        f.write(label + "\n")

print("Model and labels saved successfully to Google Drive.")
print("Drive model path:", drive_model_path)
print("Drive labels path:", drive_labels_path)

from google.colab import files

files.download(model_path)
files.download(labels_path)
"""""