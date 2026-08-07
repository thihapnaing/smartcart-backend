#
#   Author: Junior
#

from pathlib import Path
import pickle

import cv2
import numpy as np

from sklearn.metrics.pairwise import cosine_similarity

from tensorflow.keras.models import load_model
from tensorflow.keras.models import Model

from utils.color_detector import detect_color
from utils.ranking import get_color_bonus


class CNNService:

    def __init__(self):

        self.image_size = 128

        self.model = None
        self.feature_extractor = None

        self.feature_vectors = None
        self.product_index = None

        self.base_path = Path(__file__).resolve().parent.parent / "cnn"

    # ======================================================
    # Load CNN
    # ======================================================

    def load(self):

        print("=" * 60)
        print("Loading CNN Service")
        print("=" * 60)

        model_file = self.base_path / "best_model.keras"
        feature_file = self.base_path / "feature_vectors.npy"
        product_index_file = self.base_path / "product_index.pkl"

        # Check files exist
        if not model_file.exists():
            raise FileNotFoundError(f"Cannot find {model_file}")

        if not feature_file.exists():
            raise FileNotFoundError(f"Cannot find {feature_file}")

        if not product_index_file.exists():
            raise FileNotFoundError(f"Cannot find {product_index_file}")

        print("Loading model...")

        self.model = load_model(model_file)

        self.feature_extractor = Model(
            inputs=self.model.input,
            outputs=self.model.get_layer("embedding").output
        )

        print("Loading feature database...")

        self.feature_vectors = np.load(feature_file)

        print("Loading product index...")

        with open(product_index_file, "rb") as f:
            self.product_index = pickle.load(f)

        print(f"Images Loaded : {len(self.product_index)}")

        print("CNN Ready")
        print("=" * 60)

    # ======================================================
    # Preprocess Image
    # ======================================================

    def preprocess(self, image):

        image = cv2.cvtColor(
            image,
            cv2.COLOR_BGR2RGB
        )

        image = cv2.resize(
            image,
            (
                self.image_size,
                self.image_size
            )
        )

        image = image.astype(np.float32)

        image /= 255.0

        image = np.expand_dims(
            image,
            axis=0
        )

        return image

    # ======================================================
    # Extract Embedding
    # ======================================================

    def extract_feature(self, image):

        feature = self.feature_extractor.predict(
            image,
            verbose=0
        )[0]

        norm = np.linalg.norm(feature)

        if norm != 0:
            feature = feature / norm

        return feature

    # ======================================================
    # Search
    # ======================================================

    def search(self, image_bytes):

        if self.model is None:
            raise Exception("CNN model has not been loaded.")

        image = np.frombuffer(
            image_bytes,
            np.uint8
        )

        image = cv2.imdecode(
            image,
            cv2.IMREAD_COLOR
        )

        if image is None:
            raise Exception("Cannot decode uploaded image.")

        # Detect dominant clothing color
        query_color = detect_color(image)

        # CNN embedding
        processed = self.preprocess(image)

        query_feature = self.extract_feature(processed)

        cnn_scores = cosine_similarity(
            [query_feature],
            self.feature_vectors
        )[0]

        results = []

        for i in range(len(self.product_index)):
            product = self.product_index[i]

            cnn_score = float(cnn_scores[i])

            color_bonus = get_color_bonus(
                query_color,
                product["color"]
            )

            # Calculate final score BEFORE using it
            final_score = cnn_score * 0.8 + color_bonus * 0.2

            results.append({

                "productId": product["productId"],

                "filename": product["filename"],

                "gender": product["gender"],

                "color": product["color"],

                "category": product["category"],

                "similarity": round(final_score, 4),

                "cnn_score": round(cnn_score, 4),

                "color_score": round(color_bonus, 4)

            })

        results.sort(
            key=lambda x: x["similarity"],
            reverse=True
        )

        if len(results) == 0:

            return {
                "prediction": "",
                "results": []
            }

        prediction = (
            f"{results[0]['gender']} "
            f"{results[0]['color']} "
            f"{results[0]['category']}"
        )

        return {

            "prediction": prediction,

            "query_color": query_color,

            "total": min(10, len(results)),

            "results": results[:10]

        }