from pathlib import Path

import cv2
import numpy as np
import pytest

#
#   Author: Junior
#

from services.cnn_service import CNNService

BASE_DIR = Path(__file__).resolve().parent
CNN_DIR = BASE_DIR / "cnn"
TEST_IMAGE = BASE_DIR / "dataset" / "test" / "test.jpg"


class FakeFeatureExtractor:
    def predict(self, image, verbose=0):
        return np.array([[1.0, 0.0, 0.0, 0.0]], dtype=np.float32)


def create_fake_service():
    service = CNNService()
    service.model = object()
    service.feature_extractor = FakeFeatureExtractor()
    service.feature_vectors = np.array(
        [[1.0, 0.0, 0.0, 0.0], [0.0, 1.0, 0.0, 0.0]],
        dtype=np.float32,
    )
    service.product_index = [
        {
            "productId": 1,
            "filename": "img (1).jpg",
            "gender": "MAN",
            "color": "RED",
            "category": "Shirt",
        },
        {
            "productId": 2,
            "filename": "img (2).jpg",
            "gender": "WOMAN",
            "color": "BLUE",
            "category": "Shirt",
        },
    ]
    return service


def test_cnn_service_initial_state():
    service = CNNService()
    assert service.image_size == 128
    assert service.model is None
    assert service.feature_extractor is None
    assert service.feature_vectors is None
    assert service.product_index is None


def test_preprocess_image():
    service = CNNService()
    image = np.zeros((300, 400, 3), dtype=np.uint8)
    processed = service.preprocess(image)
    assert processed.shape == (1, 128, 128, 3)
    assert processed.dtype == np.float32
    assert processed.min() >= 0.0
    assert processed.max() <= 1.0


def test_extract_feature_normalizes_vector():
    service = CNNService()

    class FakeExtractor:
        def predict(self, image, verbose=0):
            return np.array([[3.0, 4.0, 0.0, 0.0]], dtype=np.float32)

    service.feature_extractor = FakeExtractor()
    feature = service.extract_feature(np.zeros((1, 128, 128, 3), dtype=np.float32))
    assert feature.shape == (4,)
    assert np.isclose(np.linalg.norm(feature), 1.0)
    assert np.allclose(feature, [0.6, 0.8, 0.0, 0.0])


def test_search_without_loaded_model():
    service = CNNService()
    with pytest.raises(Exception, match="CNN model has not been loaded"):
        service.search(b"not-an-image")


def test_search_invalid_image():
    service = create_fake_service()
    with pytest.raises(Exception, match="Cannot decode uploaded image"):
        service.search(b"not-a-real-image")


def _patch_search_dependencies(monkeypatch):
    monkeypatch.setattr("services.cnn_service.detect_color", lambda image: "RED")
    monkeypatch.setattr(
        "services.cnn_service.get_color_bonus",
        lambda query_color, product_color: 1.0 if query_color == product_color else 0.0,
    )


def _make_test_image_bytes():
    image = np.zeros((128, 128, 3), dtype=np.uint8)
    image[:, :] = (0, 0, 255)
    success, encoded = cv2.imencode(".jpg", image)
    assert success
    return encoded.tobytes()


def test_search_returns_expected_structure(monkeypatch):
    service = create_fake_service()
    _patch_search_dependencies(monkeypatch)
    response = service.search(_make_test_image_bytes())

    assert "prediction" in response
    assert "query_color" in response
    assert "total" in response
    assert "results" in response
    assert response["query_color"] == "RED"
    assert response["total"] == 2
    assert len(response["results"]) == 2


def test_search_results_sorted_by_similarity(monkeypatch):
    service = create_fake_service()
    _patch_search_dependencies(monkeypatch)
    response = service.search(_make_test_image_bytes())
    similarities = [item["similarity"] for item in response["results"]]
    assert similarities == sorted(similarities, reverse=True)


def test_search_result_fields(monkeypatch):
    service = create_fake_service()
    _patch_search_dependencies(monkeypatch)
    response = service.search(_make_test_image_bytes())

    required = {
        "productId", "filename", "gender", "color", "category",
        "similarity", "cnn_score", "color_score",
    }
    for result in response["results"]:
        assert required.issubset(result.keys())


def test_prediction_format(monkeypatch):
    service = create_fake_service()
    _patch_search_dependencies(monkeypatch)
    response = service.search(_make_test_image_bytes())
    assert response["prediction"] == "MAN RED Shirt"


@pytest.mark.integration
def test_real_cnn_search():
    required_files = [
        CNN_DIR / "best_model.keras",
        CNN_DIR / "feature_vectors.npy",
        CNN_DIR / "product_index.pkl",
        TEST_IMAGE,
    ]
    if not all(path.exists() for path in required_files):
        pytest.skip("Real CNN integration files/image are missing")

    service = CNNService()
    service.load()
    response = service.search(TEST_IMAGE.read_bytes())

    assert isinstance(response, dict)
    assert isinstance(response["prediction"], str)
    assert isinstance(response["query_color"], str)
    assert isinstance(response["results"], list)
    assert len(response["results"]) <= 10

    for result in response["results"]:
        assert "productId" in result
        assert "filename" in result
        assert "gender" in result
        assert "color" in result
        assert "category" in result
        assert "similarity" in result
        assert "cnn_score" in result
        assert "color_score" in result


@pytest.mark.integration
def test_real_cnn_similarity_scores():
    required_files = [
        CNN_DIR / "best_model.keras",
        CNN_DIR / "feature_vectors.npy",
        CNN_DIR / "product_index.pkl",
        TEST_IMAGE,
    ]
    if not all(path.exists() for path in required_files):
        pytest.skip("Real CNN integration files/image are missing")

    service = CNNService()
    service.load()
    response = service.search(TEST_IMAGE.read_bytes())
    similarities = [result["similarity"] for result in response["results"]]

    assert similarities == sorted(similarities, reverse=True)
    for score in similarities:
        assert isinstance(score, float)
        assert np.isfinite(score)
