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
    if label in {"intel air detail", "intel report"}:
        old = old.replace('"\n', '"\\n').replace('명\n주공', '명\\n주공')
        new = new.replace('"\n', '"\\n').replace('명\n주공', '명\\n주공')
    return original(text, old, new, label)


module.replace_once = normalized_replace
module.main()
