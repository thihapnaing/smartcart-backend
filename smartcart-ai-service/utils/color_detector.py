import cv2
import numpy as np


def detect_color(image):

    h, w = image.shape[:2]

    # =====================================================
    # 1. Focus on the central object area.
    #
    # Product images usually place the clothing/shoe
    # near the center. This reduces the influence of
    # large backgrounds around the object.
    # =====================================================

    y1 = int(h * 0.18)
    y2 = int(h * 0.82)

    x1 = int(w * 0.18)
    x2 = int(w * 0.82)

    region = image[y1:y2, x1:x2]

    hsv = cv2.cvtColor(region, cv2.COLOR_BGR2HSV)

    H = hsv[:, :, 0]
    S = hsv[:, :, 1]
    V = hsv[:, :, 2]

    total_pixels = H.size

    # =====================================================
    # 2. Calculate candidate pixel masks.
    #
    # We specifically handle black/gray/white BEFORE
    # colorful pixels.
    #
    # This is important for black products on colorful
    # backgrounds.
    # =====================================================

    black_mask = (
        (V < 75)
    )

    gray_mask = (
        (S < 40) &
        (V >= 75) &
        (V < 190)
    )

    white_mask = (
        (S < 40) &
        (V >= 190)
    )

    # =====================================================
    # 3. Calculate the amount of dark pixels.
    # =====================================================

    black_ratio = np.sum(black_mask) / total_pixels

    gray_ratio = np.sum(gray_mask) / total_pixels

    white_ratio = np.sum(white_mask) / total_pixels

    print()
    print("========== COLOR DETECTION ==========")
    print("Black ratio:", round(black_ratio, 4))
    print("Gray ratio :", round(gray_ratio, 4))
    print("White ratio:", round(white_ratio, 4))
    print("=====================================")

    # =====================================================
    # 4. Black detection
    #
    # If a significant amount of dark pixels exists in
    # the central region, prefer BLACK over colorful
    # background pixels.
    # =====================================================

    if black_ratio >= 0.08:
        print("Detected color: black")
        return "black"

    # =====================================================
    # 5. Gray detection
    # =====================================================

    if gray_ratio >= 0.10:
        print("Detected color: gray")
        return "gray"

    # =====================================================
    # 6. White detection
    # =====================================================

    if white_ratio >= 0.15:
        print("Detected color: white")
        return "white"

    # =====================================================
    # 7. For colorful objects, use HSV pixels.
    #
    # Ignore very dark pixels and very bright pixels.
    # =====================================================

    colorful_mask = (
        (S > 45) &
        (V > 50) &
        (V < 245)
    )

    if not np.any(colorful_mask):

        print("Detected color: unknown")
        return "unknown"

    hue_values = H[colorful_mask]

    # =====================================================
    # 8. Hue histogram
    # =====================================================

    histogram = np.histogram(
        hue_values,
        bins=180,
        range=(0, 180)
    )[0]

    dominant_hue = int(
        np.argmax(histogram)
    )

    print("Dominant hue:", dominant_hue)

    # =====================================================
    # 9. Convert hue to color.
    # =====================================================

    if dominant_hue < 8 or dominant_hue >= 170:
        color = "red"

    elif dominant_hue < 22:
        color = "orange"

    elif dominant_hue < 35:
        color = "yellow"

    elif dominant_hue < 85:
        color = "green"

    elif dominant_hue < 130:
        color = "blue"

    elif dominant_hue < 160:
        color = "purple"

    else:
        color = "pink"

    print("Detected color:", color)

    return color