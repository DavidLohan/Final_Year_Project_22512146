import os
import numpy as np
from tensorflow import keras
from tensorflow.keras import layers
from sklearn.model_selection import train_test_split

DATA_DIR = "data"
MODEL_DIR = "model"

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
    "umbrella"
]

SAMPLES_PER_CLASS = 15000
IMG_SIZE = 28
EPOCHS = 10
BATCH_SIZE = 128


def load_data():
    x_list = []
    y_list = []

    for idx, class_name in enumerate(CLASSES):
        file_path = os.path.join(DATA_DIR, f"full_numpy_bitmap_{class_name}.npy")
        data = np.load(file_path)

        # Limit samples per class
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

print("Training model...")
history = model.fit(
    X_train,
    y_train,
    validation_data=(X_test, y_test),
    epochs=EPOCHS,
    batch_size=BATCH_SIZE
)

print("Evaluating model...")
loss, accuracy = model.evaluate(X_test, y_test, verbose=0)
print(f"Test Loss: {loss:.4f}")
print(f"Test Accuracy: {accuracy:.4f}")

os.makedirs(MODEL_DIR, exist_ok=True)

model.save(os.path.join(MODEL_DIR, "quickdraw_model.h5"))

with open(os.path.join(MODEL_DIR, "labels.txt"), "w") as f:
    for label in CLASSES:
        f.write(label + "\n")

print("Model and labels saved successfully.")