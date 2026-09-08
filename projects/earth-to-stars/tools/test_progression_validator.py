#!/usr/bin/env python3
from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from validate_progression import ProgressionValidationError, load_graph, validate_main_path


class ProgressionValidatorTest(unittest.TestCase):
    def write_graph(self, payload: dict) -> Path:
        temp = tempfile.NamedTemporaryFile("w", encoding="utf-8", suffix=".json", delete=False)
        with temp:
            json.dump(payload, temp)
        self.addCleanup(lambda: Path(temp.name).unlink(missing_ok=True))
        return Path(temp.name)

    @staticmethod
    def base(nodes: list[dict], milestones: list[str]) -> dict:
        return {
            "schema": 1,
            "forbidden_required_dimensions": ["minecraft:the_nether", "minecraft:the_end"],
            "main_milestones": milestones,
            "nodes": nodes,
        }

    def test_clean_overworld_route_passes_even_with_optional_nether_shortcut(self) -> None:
        path = self.write_graph(self.base([
            {"id": "earth", "dimension": "minecraft:overworld", "requires_any": [[]]},
            {"id": "launch", "dimension": "minecraft:overworld", "requires_any": [["earth"]]},
            {"id": "nether_shortcut", "dimension": "minecraft:the_nether", "requires_any": [["earth"]], "optional": True},
        ], ["launch"]))
        validate_main_path(path)

    def test_alternative_clean_route_keeps_main_progression_valid(self) -> None:
        path = self.write_graph(self.base([
            {"id": "earth", "dimension": "minecraft:overworld", "requires_any": [[]]},
            {"id": "nether", "dimension": "minecraft:the_nether", "requires_any": [["earth"]], "optional": True},
            {"id": "heat", "dimension": "minecraft:overworld", "requires_any": [["nether"], ["earth"]]},
            {"id": "launch", "dimension": "minecraft:overworld", "requires_any": [["heat"]]},
        ], ["launch"]))
        validate_main_path(path)

    def test_nether_only_required_chain_fails(self) -> None:
        path = self.write_graph(self.base([
            {"id": "earth", "dimension": "minecraft:overworld", "requires_any": [[]]},
            {"id": "blaze_gate", "dimension": "minecraft:the_nether", "requires_any": [["earth"]]},
            {"id": "launch", "dimension": "minecraft:overworld", "requires_any": [["blaze_gate"]]},
        ], ["launch"]))
        with self.assertRaisesRegex(ProgressionValidationError, "launch"):
            validate_main_path(path)

    def test_end_milestone_itself_fails(self) -> None:
        path = self.write_graph(self.base([
            {"id": "earth", "dimension": "minecraft:overworld", "requires_any": [[]]},
            {"id": "final_drive", "dimension": "minecraft:the_end", "requires_any": [["earth"]]},
        ], ["final_drive"]))
        with self.assertRaisesRegex(ProgressionValidationError, "minecraft:the_end"):
            validate_main_path(path)

    def test_unknown_dependency_is_rejected(self) -> None:
        path = self.write_graph(self.base([
            {"id": "earth", "dimension": "minecraft:overworld", "requires_any": [["missing"]]},
        ], ["earth"]))
        with self.assertRaisesRegex(ProgressionValidationError, "unknown dependency"):
            load_graph(path)

    def test_dependency_cycle_is_rejected(self) -> None:
        path = self.write_graph(self.base([
            {"id": "a", "dimension": "minecraft:overworld", "requires_any": [["b"]]},
            {"id": "b", "dimension": "minecraft:overworld", "requires_any": [["a"]]},
        ], ["a"]))
        with self.assertRaisesRegex(ProgressionValidationError, "cycle"):
            load_graph(path)


if __name__ == "__main__":
    unittest.main()
