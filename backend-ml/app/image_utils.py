import io
import numpy as np
from PIL import Image, ImageOps
from scipy import ndimage

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
    square = np.pad(square, ((pad, pad), (pad, pad)), mode="constant", constant_values=0)

    img = Image.fromarray(square)
    img = img.resize((28, 28))

    arr = np.array(img).astype("float32") / 255.0
    arr = arr.reshape(1, 28, 28, 1)

    return arr