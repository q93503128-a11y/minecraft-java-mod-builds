#!/usr/bin/env python3
"""Compatibility runner for the reproducible v0.18.27 source migration."""
import importlib.util
from pathlib import Path

HERE = Path(__file__).resolve().parent
TARGET = HERE / "apply_v01827_aerial_combat.py"
spec = importlib.util.spec_from_file_location("v01827_patch", TARGET)
module = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(module)
original = module.replace_once


def normalized_replace(text: str, old: str, new: str, label: str) -> str:
    # Importing the authored patch evaluates Java backslash-n literals inside Python strings.
    # Reconstruct only the two affected Java source anchors exactly.
    if label == "intel air detail":
        old = '                    + "\\n" + direction\n                    + "\\n공성 병과: "\n'
        new = ('                    + "\\n" + direction\n'
               '                    + "\\n공중 위협: " + air\n'
               '                    + "\\n공성 병과: "\n')
    elif label == "intel report":
        old = ('                + players + "명\\n주공·별동대·전장 상황·웨이브 특성·병과·수량은 낮에 미리 공개됩니다."\n')
        new = ('                + players + "명\\n주공·별동대·공중 위협·전장 상황·웨이브 특성·병과·수량은 낮에 미리 공개됩니다."\n')
    return original(text, old, new, label)


module.replace_once = normalized_replace
module.main()
