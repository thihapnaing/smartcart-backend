#
#   Author: Junior
#   PLEASE DO NOT TOUCH THIS (FOR TRAINING ONLY)
#

# ============================================================
# SmartCart CNN Training
# Gender + Category
# MobileNetV2 Embedding Network
# ============================================================

import os
import cv2
import pickle
import random
import warnings

import numpy as np
import pandas as pd
import tensorflow as tf

from tensorflow.keras.applications import MobileNetV2

from tensorflow.keras.layers import (
    Input,
    Dense,
    Dropout,
    BatchNormalization,
    GlobalAveragePooling2D,
    RandomFlip,
    RandomRotation,
    RandomZoom,
    RandomContrast
)

from tensorflow.keras.models import Model

from tensorflow.keras.optimizers import Adam

from tensorflow.keras.regularizers import l2

from tensorflow.keras.callbacks import (
    EarlyStopping,
    ModelCheckpoint,
    ReduceLROnPlateau
)

from sklearn.model_selection import train_test_split

from sklearn.preprocessing import LabelEncoder

warnings.filterwarnings("ignore")

# ============================================================
# TensorFlow Information
# ============================================================

print("=" * 60)
print("TensorFlow Version :", tf.__version__)
print("=" * 60)

gpus = tf.config.list_physical_devices("GPU")

if len(gpus) > 0:
    print("Running on GPU")
else:
    print("Running on CPU")

print("=" * 60)

# ============================================================
# Random Seed
# ============================================================

SEED = 42

random.seed(SEED)

np.random.seed(SEED)

tf.random.set_seed(SEED)

# ============================================================
# Configuration
# ============================================================

IMAGE_SIZE = 128

DATASET_FOLDER = "dataset/train"

LABEL_FILE = "labels.csv"

MODEL_FILE = "best_model.keras"

FINAL_MODEL = "final_model.keras"

BATCH_SIZE = 16

EPOCHS = 20

# ============================================================
# Verify Paths
# ============================================================

if not os.path.exists(DATASET_FOLDER):
    raise Exception(f"Dataset folder not found : {DATASET_FOLDER}")

if not os.path.exists(LABEL_FILE):
    raise Exception(f"Cannot find {LABEL_FILE}")

print("Dataset Folder :", DATASET_FOLDER)
print("Label File     :", LABEL_FILE)

# ============================================================
# Read labels.csv
# ============================================================

df = pd.read_csv(LABEL_FILE)

print()
print(df.head())

df = df.dropna()

df = df.reset_index(drop=True)

print()
print("Total Images :", len(df))

# ============================================================
# Verify Images
# ============================================================

valid_rows = []

missing = 0

for _, row in df.iterrows():

    image_path = os.path.join(
        DATASET_FOLDER,
        row["filename"]
    )

    if os.path.exists(image_path):
        valid_rows.append(row)
    else:
        missing += 1

df = pd.DataFrame(valid_rows)

df = df.reset_index(drop=True)

print()

print("Missing Images :", missing)

print("Valid Images :", len(df))

# ============================================================
# Load Images
# ============================================================

print()
print("=" * 60)
print("Loading Images")
print("=" * 60)

images = []

gender_labels = []

category_labels = []

filenames = []

for _, row in df.iterrows():

    image_path = os.path.join(
        DATASET_FOLDER,
        row["filename"]
    )

    image = cv2.imread(image_path)

    if image is None:
        continue

    image = cv2.cvtColor(
        image,
        cv2.COLOR_BGR2RGB
    )

    image = cv2.resize(
        image,
        (IMAGE_SIZE, IMAGE_SIZE)
    )

    image = image.astype(np.float32)

    image /= 255.0

    images.append(image)

    gender_labels.append(row["gender"])

    category_labels.append(row["category"])

    filenames.append(row["filename"])

# ============================================================
# Convert to NumPy
# ============================================================

X = np.array(images, dtype=np.float32)

gender_labels = np.array(gender_labels)

category_labels = np.array(category_labels)

filenames = np.array(filenames)

print()

print("Images Loaded :", len(X))

print("Image Shape :", X.shape)

# ============================================================
# Verify Dataset
# ============================================================

assert len(X) == len(gender_labels)

assert len(X) == len(category_labels)

assert len(X) == len(filenames)

print()

print("Dataset Verification Passed")

# ============================================================
# Encode Labels
# ============================================================

gender_encoder = LabelEncoder()

category_encoder = LabelEncoder()

gender_labels = gender_encoder.fit_transform(
    gender_labels
)

category_labels = category_encoder.fit_transform(
    category_labels
)

print()

print("Gender Classes")

print(gender_encoder.classes_)

print()

print("Category Classes")

print(category_encoder.classes_)

# ============================================================
# Save Encoders
# ============================================================

with open("gender_encoder.pkl", "wb") as f:
    pickle.dump(
        gender_encoder,
        f
    )

with open("category_encoder.pkl", "wb") as f:
    pickle.dump(
        category_encoder,
        f
    )

print()

print("Label Encoders Saved")

# ============================================================
# Train Test Split
# ============================================================

(
    X_train,
    X_test,
    gender_train,
    gender_test,
    category_train,
    category_test
) = train_test_split(

    X,

    gender_labels,

    category_labels,

    test_size=0.20,

    random_state=SEED,

    shuffle=True

)

print()

print("=" * 60)

print("Dataset Split")

print("=" * 60)

print()

print("Training Images :", len(X_train))

print("Testing Images :", len(X_test))

# ============================================================
# Data Augmentation
# ============================================================

data_augmentation = tf.keras.Sequential([

    RandomFlip("horizontal"),

    RandomRotation(0.10),

    RandomZoom(0.15),

    RandomContrast(0.10)

])

# ============================================================
# Build MobileNetV2
# ============================================================

print()
print("=" * 60)
print("Building MobileNetV2")
print("=" * 60)

base_model = MobileNetV2(

    input_shape=(IMAGE_SIZE, IMAGE_SIZE, 3),

    include_top=False,

    weights="imagenet"

)

# Freeze pretrained layers
base_model.trainable = False

# ============================================================
# Input
# ============================================================

inputs = Input(
    shape=(IMAGE_SIZE, IMAGE_SIZE, 3)
)

# ============================================================
# Data Augmentation
# ============================================================

x = data_augmentation(inputs)

# ============================================================
# MobileNetV2 Preprocessing
# ============================================================

x = tf.keras.applications.mobilenet_v2.preprocess_input(
    x * 255.0
)

# ============================================================
# CNN Backbone
# ============================================================

x = base_model(
    x,
    training=False
)

# ============================================================
# Global Pooling
# ============================================================

x = GlobalAveragePooling2D()(x)

# ============================================================
# Dense Layer
# ============================================================

x = Dense(

    512,

    activation="relu",

    kernel_regularizer=l2(0.0005)

)(x)

x = BatchNormalization()(x)

x = Dropout(0.50)(x)

# ============================================================
# Embedding Layer
# ============================================================

embedding = Dense(

    256,

    activation="relu",

    name="embedding"

)(x)

# ============================================================
# Output 1
# Gender
# ============================================================

gender_output = Dense(

    len(gender_encoder.classes_),

    activation="softmax",

    name="gender"

)(embedding)

# ============================================================
# Output 2
# Category
# ============================================================

category_output = Dense(

    len(category_encoder.classes_),

    activation="softmax",

    name="category"

)(embedding)

# ============================================================
# Final Model
# ============================================================

model = Model(

    inputs=inputs,

    outputs=[

        gender_output,

        category_output

    ]

)

model.summary()

# ============================================================
# Compile
# ============================================================

model.compile(

    optimizer=Adam(
        learning_rate=0.0003
    ),

    loss={

        "gender":"sparse_categorical_crossentropy",

        "category":"sparse_categorical_crossentropy"

    },

    metrics={

        "gender":["accuracy"],

        "category":["accuracy"]

    }

)

print()

print("=" * 60)

print("Model Compiled")

print("=" * 60)

# ============================================================
# Callbacks
# ============================================================

checkpoint = ModelCheckpoint(

    MODEL_FILE,

    monitor="val_loss",

    save_best_only=True,

    verbose=1

)

earlystop = EarlyStopping(

    monitor="val_loss",

    patience=8,

    restore_best_weights=True,

    verbose=1

)

reduce_lr = ReduceLROnPlateau(

    monitor="val_loss",

    factor=0.2,

    patience=3,

    min_lr=1e-6,

    verbose=1

)

callbacks = [

    checkpoint,

    earlystop,

    reduce_lr

]

# ============================================================
# Training
# ============================================================

print()

print("=" * 60)
print("Training Started")
print("=" * 60)

history = model.fit(

    X_train,

    {

        "gender": gender_train,

        "category": category_train

    },

    validation_data=(

        X_test,

        {

            "gender": gender_test,

            "category": category_test

        }

    ),

    epochs=EPOCHS,

    batch_size=BATCH_SIZE,

    callbacks=callbacks,

    shuffle=True,

    verbose=1

)

# ============================================================
# Evaluation
# ============================================================

print()

print("=" * 60)
print("Evaluating Model")
print("=" * 60)

results = model.evaluate(

    X_test,

    {

        "gender": gender_test,

        "category": category_test

    },

    verbose=1

)

print()

print("Evaluation Results")

for name, value in zip(model.metrics_names, results):
    print(f"{name}: {value:.4f}")

# ============================================================
# Save Final Model
# ============================================================

print()

print("=" * 60)
print("Saving Model")
print("=" * 60)

model.save(FINAL_MODEL)

print()

print("Saved:")

print("Best Model :", MODEL_FILE)

print("Final Model:", FINAL_MODEL)

# ============================================================
# Finish
# ============================================================

print()

print("=" * 60)
print("Training Finished")
print("=" * 60)

print()

print("Embedding Dimension : 256")

print("Outputs")

print("- Gender")

print("- Category")

print()

print("Color detection is handled separately using OpenCV (HSV + KMeans).")