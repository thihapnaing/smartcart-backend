#
#   Author: Junior
#   PLEASE DO NOT TOUCH THIS (FOR TRAINING ONLY)
#

import os
import cv2
import pickle
import numpy as np
import pandas as pd

from tensorflow.keras.models import load_model
from tensorflow.keras.models import Model
from color_mapping import COLOR_MAPPING

# =====================================
# Configuration
# =====================================

IMAGE_SIZE = 128

DATASET_PATH = "dataset/train"

CSV_FILE = "labels.csv"

MODEL_FILE = "best_model.keras"

# =====================================
# Load CNN
# =====================================

print("=" * 50)
print("Loading CNN")
print("=" * 50)

model = load_model(MODEL_FILE)

model.summary()

# =====================================
# Feature Extractor
# =====================================

feature_extractor = Model(
    inputs=model.input,
    outputs=model.get_layer("embedding").output
)

print()
print("Embedding Layer Ready")

# =====================================
# Read CSV
# =====================================

df = pd.read_csv(CSV_FILE)

# Merge similar colors into major colors
df["color"] = df["color"].replace(COLOR_MAPPING)

print(df.head())

# =====================================
# Storage
# =====================================

features = []

product_index = []

# =====================================
# Extract Features
# =====================================

print("=" * 50)
print("Extracting Features")
print("=" * 50)

for index, row in df.iterrows():

    image_path = os.path.join(
        DATASET_PATH,
        row["filename"]
    )

    img = cv2.imread(image_path)

    if img is None:

        print("Cannot read :", image_path)

        continue

    img = cv2.cvtColor(
        img,
        cv2.COLOR_BGR2RGB
    )

    img = cv2.resize(
        img,
        (IMAGE_SIZE, IMAGE_SIZE)
    )

    img = img.astype(np.float32)

    img /= 255.0

    img = np.expand_dims(
        img,
        axis=0
    )

    feature = feature_extractor.predict(img, verbose=0)[0]

    feature = feature / np.linalg.norm(feature)

    features.append(feature)

    product_index.append({

        "productId": int(row["id"]),

        "filename": row["filename"],

        "gender": row["gender"],

        "color": row["color"],

        "category": row["category"]

    })

    if (index + 1) % 50 == 0:

        print(f"{index + 1} images processed")

# =====================================
# Convert to NumPy
# =====================================

features = np.array(features)

print()

print("Feature Shape")

print(features.shape)

# =====================================
# Save Feature Vectors
# =====================================

np.save(
    "feature_vectors.npy",
    features
)

# =====================================
# Save Product
# =====================================

with open(
    "product_index.pkl",
    "wb"
) as f:

    pickle.dump(
        product_index,
        f
    )

# =====================================
# Finish
# =====================================

print()

print("=" * 50)
print("Feature Extraction Finished")
print("=" * 50)

print("Total Images :", len(features))

print("Feature Dimension :", features.shape[1])

print()

print("Files Created")

print("feature_vectors.npy")

print("filenames.pkl")

print("labels.pkl")