# Author: Htet Nandar (Grace)
#
# Ensures `services.*` / `routers.*` imports resolve the same way they do when running
# `python main.py` from this directory - pytest's rootdir insertion isn't guaranteed to match,
# especially when pytest is invoked from a different working directory.
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
