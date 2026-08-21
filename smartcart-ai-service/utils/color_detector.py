import cv2
import numpy as np


def detect_color(image):

    if image is None:
        return "unknown"

    h, w = image.shape[:2]

    # =====================================================
    # 1. Focus on the central object area
    # =====================================================

    y1 = int(h * 0.15)
    y2 = int(h * 0.85)

    x1 = int(w * 0.15)
    x2 = int(w * 0.85)

    region = image[y1:y2, x1:x2]

    if region.size == 0:
        return "unknown"

    # =====================================================
    # 2. Convert BGR -> HSV
    # =====================================================

    hsv = cv2.cvtColor(region, cv2.COLOR_BGR2HSV)

    H = hsv[:, :, 0]
    S = hsv[:, :, 1]
    V = hsv[:, :, 2]

    total_pixels = H.size

    if total_pixels == 0:
        return "unknown"

    # =====================================================
    # 3. Create masks
    # =====================================================

    # BLACK
    black_mask = (
        (V < 70)
    )

    # GRAY
    gray_mask = (
        (S < 45) &
        (V >= 70) &
        (V < 190)
    )

    # WHITE
    white_mask = (
        (S < 45) &
        (V >= 190)
    )

    # COLORFUL
    colorful_mask = (
        (S >= 50) &
        (V >= 50)
    )

    # =====================================================
    # 4. Calculate ratios
    # =====================================================

    black_ratio = (
        np.sum(black_mask) /
        total_pixels
    )

    gray_ratio = (
        np.sum(gray_mask) /
        total_pixels
    )

    white_ratio = (
        np.sum(white_mask) /
        total_pixels
    )

    colorful_ratio = (
        np.sum(colorful_mask) /
        total_pixels
    )

    print()
    print("========== COLOR DETECTION ==========")
    print("Black ratio   :", round(black_ratio, 4))
    print("Gray ratio    :", round(gray_ratio, 4))
    print("White ratio   :", round(white_ratio, 4))
    print("Colorful ratio:", round(colorful_ratio, 4))
    print("=====================================")

    # =====================================================
    # 5. IMPORTANT:
    #    Give colorful objects priority when they clearly
    #    dominate the image.
    # =====================================================

    if colorful_ratio >= 0.12:

        hue_values = H[colorful_mask]

        histogram = np.histogram(
            hue_values,
            bins=180,
            range=(0, 180)
        )[0]

        dominant_hue = int(
            np.argmax(histogram)
        )

        print(
            "Dominant hue:",
            dominant_hue
        )

        # =================================================
        # RED
        # =================================================

        if (
            dominant_hue < 8
            or dominant_hue >= 170
        ):

            color = "red"

        # =================================================
        # ORANGE
        # =================================================

        elif dominant_hue < 22:

            color = "orange"

        # =================================================
        # YELLOW
        # =================================================

        elif dominant_hue < 35:

            color = "yellow"

        # =================================================
        # GREEN
        # =================================================

        elif dominant_hue < 85:

            color = "green"

        # =================================================
        # BLUE
        # =================================================

        elif dominant_hue < 130:

            color = "blue"

        # =================================================
        # PURPLE
        # =================================================

        elif dominant_hue < 160:

            color = "purple"

        # =================================================
        # PINK
        # =================================================

        else:

            color = "pink"

        print(
            "Detected color:",
            color
        )

        return color

    # =====================================================
    # 6. If there is not enough color,
    #    check black / gray / white.
    # =====================================================

    if black_ratio >= 0.08:

        print(
            "Detected color: black"
        )

        return "black"

    if gray_ratio >= 0.10:

        print(
            "Detected color: gray"
        )

        return "gray"

    if white_ratio >= 0.15:

        print(
            "Detected color: white"
        )

        return "white"

    # =====================================================
    # 7. Fallback
    # =====================================================

    print(
        "Detected color: unknown"
    )

    return "unknown"