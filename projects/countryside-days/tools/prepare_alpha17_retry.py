#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path

HERE = Path(__file__).resolve().parent
source = (HERE / "prepare_alpha17_exact.py").read_text("utf-8")
source = source.replace(
    'Blocks.COPPER_BLOCKS.getFirst().defaultBlockState()',
    'Blocks.RAW_COPPER_BLOCK.defaultBlockState()',
)
source = source.replace(
    'is(Blocks.COPPER_BLOCKS.getFirst())',
    'is(Blocks.RAW_COPPER_BLOCK)',
)
source = source.replace(
    'replace(tests, "Blocks.COPPER_BLOCKS.getFirst()", "Blocks.IRON_BLOCK", "estate marker test")',
    'replace(tests, "Blocks.RAW_COPPER_BLOCK", "Blocks.IRON_BLOCK", "estate marker test")',
)
namespace = {"__name__": "countryside_alpha17_retry", "__file__": str(HERE / "prepare_alpha17_exact.py")}
exec(compile(source, str(HERE / "prepare_alpha17_exact.py"), "exec"), namespace)
namespace["prepare"]()
