import numpy as np
import os
from tensorflow import keras
from tensorflow.keras import layers
from sklearn.model_selection import train_test_split

DATA_DIR = "data"

CLASSES = [
    "apple",
    "dog",
    "bus",
    "cat",
    "bird",
    "mug"
]

SAMPLES_PER_CLASS = 10000

def load_data():
    X_list = []
    y_list = []

    for idx, name in enumerate(CLASSES):
        path = os.path.join(DATA_DIR, f"full_numpy_bitmap_{name}.npy")

        data = np.load(path)

        data = data[:SAMPLES_PER_CLASS]

        X_list.append(data)
        y_list.append(np.full(len(data), idx))

    X = np.concatenate(X_list)
    y = np.concatenate(y_list)

    return X, y


print("Loading data...")

X, y = load_data()

X = X / 255.0
X = X.reshape(-1, 28, 28, 1)

print("Splitting dataset...")

X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42
)

print("Building model...")

model = keras.Sequential([
    layers.Conv2D(32, (3,3), activation='relu', input_shape=(28,28,1)),
    layers.MaxPooling2D(),

    layers.Conv2D(64, (3,3), activation='relu'),
    layers.MaxPooling2D(),

    layers.Flatten(),

    layers.Dense(128, activation='relu'),

    layers.Dense(len(CLASSES), activation='softmax')
])

model.compile(
    optimizer="adam",
    loss="sparse_categorical_crossentropy",
    metrics=["accuracy"]
)

print("Training model...")

model.fit(
    X_train,
    y_train,
    epochs=10,
    batch_size=128,
    validation_data=(X_test, y_test)
)

os.makedirs("model", exist_ok=True)

model.save("model/quickdraw_model.h5")

with open("model/labels.txt", "w") as f:
    for c in CLASSES:
        f.write(c + "\n")

print("Model saved to /model")