import cv2
import numpy as np

#
#   Author: Junior
#

def detect_color(image):

    # ==============================
    # Crop shirt region
    # ==============================

    h, w = image.shape[:2]

    shirt = image[
        int(h * 0.18):int(h * 0.55),
        int(w * 0.25):int(w * 0.75)
    ]

    # ==============================
    # Convert to HSV
    # ==============================

    hsv = cv2.cvtColor(
        shirt,
        cv2.COLOR_BGR2HSV
    )

    # ==============================
    # Remove background
    # ==============================

    mask = (

        (hsv[:, :, 1] > 40) &
        (hsv[:, :, 2] > 40)

    )

    pixels = hsv[mask]

    if len(pixels) == 0:
        return "unknown"

    pixels = np.float32(pixels)

    # ==============================
    # KMeans
    # ==============================

    criteria = (
        cv2.TERM_CRITERIA_EPS +
        cv2.TERM_CRITERIA_MAX_ITER,
        20,
        0.2
    )

    K = 3

    _, labels, centers = cv2.kmeans(

        pixels,

        K,

        None,

        criteria,

        10,

        cv2.KMEANS_RANDOM_CENTERS

    )

    # ==============================
    # Largest Cluster
    # ==============================

    counts = np.bincount(labels.flatten())

    dominant = centers[np.argmax(counts)]

    hue = dominant[0]

    sat = dominant[1]

    val = dominant[2]

    print()

    print("Dominant HSV")

    print(hue, sat, val)

    # ==============================
    # Very dark
    # ==============================

    if val < 55:
        return "black"

    # ==============================
    # White / Gray
    # ==============================

    if sat < 30:

        if val > 190:
            return "white"

        elif val > 90:
            return "gray"

        else:
            return "black"

    # ==============================
    # Hue Mapping
    # ==============================
    # ==============================
    # Brown
    # ==============================

    if (
            8 <= hue <= 22
            and sat > 60
            and val < 150
    ):
        return "brown"

    # ==============================
    # Red
    # ==============================

    if hue < 8 or hue >= 170:
        return "red"

    # ==============================
    # Orange
    # ==============================

    if (
            8 <= hue <= 22
            and val >= 150
    ):
        return "orange"

    # ==============================
    # Yellow
    # ==============================

    if 22 <= hue < 35:
        return "yellow"

    # ==============================
    # Green
    # ==============================

    if 35 <= hue < 85:
        return "green"

    # ==============================
    # Blue
    # ==============================

    if 85 <= hue < 130:
        return "blue"

    # ==============================
    # Purple
    # ==============================

    if 130 <= hue < 160:
        return "purple"

    # ==============================
    # Pink
    # ==============================

    return "pink"

