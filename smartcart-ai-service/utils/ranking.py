#
#   Author: Junior
#

from utils.color_similarity import COLOR_GROUPS


def get_color_bonus(query_color, candidate_color):

    if query_color == candidate_color:
        return 1.0

    if candidate_color in COLOR_GROUPS.get(query_color, []):
        return 0.8

    return 0.2