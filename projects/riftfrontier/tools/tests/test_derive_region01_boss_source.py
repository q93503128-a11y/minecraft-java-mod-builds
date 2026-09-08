from __future__ import annotations

import hashlib
import importlib.util
import json
import sys
import tempfile
import unittest
from pathlib import Path

MODULE_PATH = Path(__file__).resolve().parents[1] / "derive_region01_boss_source.py"
SPEC = importlib.util.spec_from_file_location("derive_region01_boss_source", MODULE_PATH)
assert SPEC and SPEC.loader
derive_source = importlib.util.module_from_spec(SPEC)
sys.modules["derive_region01_boss_source"] = derive_source
SPEC.loader.exec_module(derive_source)


def fixture_document() -> dict:
    return {
        "asset": {"version": "2.0", "generator": "fixture"},
        "nodes": [
            {"name": "Root", "children": [1], "translation": [0, 0, 0]},
            {"name": "Body", "children": [2], "mesh": 0, "skin": 0},
            {"name": "Head", "rotation": [0, 0, 0, 1]},
        ],
        "meshes": [{"name": "Dragon", "primitives": [{"material": 0}]}],
        "materials": [{"name": "Atlas"}],
        "textures": [{"source": 0}],
        "images": [{"uri": "Atlas.png"}],
        "buffers": [{"uri": "Dragon_Evolved.bin", "byteLength": 1}],
        "skins": [{"name": "CharacterArmature", "skeleton": 0, "joints": [0, 1, 2]}],
        "accessors": [
            {"min": [0.0], "max": [0.5], "count": 2, "type": "SCALAR"},
            {"count": 2, "type": "VEC4"},
            {"min": [0.0], "max": [1.25], "count": 2, "type": "SCALAR"},
            {"count": 2, "type": "VEC3"},
        ],
        "animations": [
            {
                "name": "Headbutt",
                "samplers": [{"input": 0, "output": 1, "interpolation": "LINEAR"}],
                "channels": [{"sampler": 0, "target": {"node": 2, "path": "rotation"}}],
            },
            {
                "name": "Punch",
                "samplers": [{"input": 2, "output": 3}],
                "channels": [{"sampler": 0, "target": {"node": 1, "path": "translation"}}],
            },
        ],
    }


def write_source(path: Path, document: dict) -> str:
    raw = json.dumps(document, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    path.write_bytes(raw)
    return hashlib.sha256(raw).hexdigest()


def write_contract(path: Path, source_sha: str, *, mesh_count: int = 1, required=None) -> None:
    if required is None:
        required = ["Headbutt", "Punch"]
    path.write_text(
        json.dumps(
            {
                "kind": "riftfrontier:boss_source_contract",
                "schema_version": 1,
                "source": {
                    "id": "fixture:dragon_evolved",
                    "filename": "Dragon_Evolved.gltf",
                    "sha256": source_sha,
                    "license_record": "docs/THIRD_PARTY_ASSETS.md#dragon-evolved",
                },
                "expect": {
                    "gltf_version": "2.0",
                    "mesh_count": mesh_count,
                    "skin_count": 1,
                    "joint_count": 3,
                    "animation_count": 2,
                    "required_animations": required,
                },
            },
            separators=(",", ":"),
        ),
        encoding="utf-8",
    )


class DeriveRegion01BossSourceTest(unittest.TestCase):
    def test_derivation_is_byte_deterministic_and_strips_unapproved_art_payloads(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "Dragon_Evolved.gltf"
            contract = root / "contract.json"
            source_sha = write_source(source, fixture_document())
            write_contract(contract, source_sha)
            first = derive_source.derive(source, contract)
            second = derive_source.derive(source, contract)
            first_bytes = derive_source.canonical_bytes(first)
            second_bytes = derive_source.canonical_bytes(second)

        self.assertEqual(first_bytes, second_bytes)
        self.assertEqual(first["source"]["sha256"], source_sha)
        self.assertEqual(first["rig"]["skins"][0]["joints"], [0, 1, 2])
        self.assertEqual([a["name"] for a in first["animations"]], ["Headbutt", "Punch"])
        self.assertEqual(first["animations"][0]["duration_seconds"], 0.5)
        self.assertEqual(first["animations"][1]["duration_seconds"], 1.25)
        text = first_bytes.decode("utf-8")
        self.assertNotIn("Atlas.png", text)
        self.assertNotIn("Dragon_Evolved.bin", text)
        self.assertNotIn("materials", first)
        self.assertNotIn("textures", first)
        self.assertNotIn("images", first)
        self.assertNotIn("buffers", first)
        self.assertIn("final_art_approval", first["scope"]["excluded"])
        self.assertIn("hit_timing", first["scope"]["excluded"])

    def test_exact_source_hash_is_fail_closed(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "Dragon_Evolved.gltf"
            contract = root / "contract.json"
            source_sha = write_source(source, fixture_document())
            write_contract(contract, source_sha)
            source.write_bytes(source.read_bytes() + b"\n")
            with self.assertRaises(derive_source.DerivationError) as caught:
                derive_source.derive(source, contract)
        self.assertEqual(caught.exception.code, "BOSS_SOURCE_SHA256_MISMATCH")

    def test_structural_contract_drift_has_stable_issue_code(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "Dragon_Evolved.gltf"
            contract = root / "contract.json"
            source_sha = write_source(source, fixture_document())
            write_contract(contract, source_sha, mesh_count=2)
            with self.assertRaises(derive_source.DerivationError) as caught:
                derive_source.derive(source, contract)
        self.assertEqual(caught.exception.code, "BOSS_SOURCE_MESH_COUNT_MISMATCH")

    def test_required_clip_contract_is_exact_not_fuzzy(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "Dragon_Evolved.gltf"
            contract = root / "contract.json"
            source_sha = write_source(source, fixture_document())
            write_contract(contract, source_sha, required=["Headbutt", "Punch", "Flying_Idle"])
            with self.assertRaises(derive_source.DerivationError) as caught:
                derive_source.derive(source, contract)
        self.assertEqual(caught.exception.code, "BOSS_SOURCE_REQUIRED_ANIMATION_MISSING")

    def test_invalid_node_reference_is_rejected_before_output(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "Dragon_Evolved.gltf"
            contract = root / "contract.json"
            document = fixture_document()
            document["nodes"][0]["children"] = [99]
            source_sha = write_source(source, document)
            write_contract(contract, source_sha)
            with self.assertRaises(derive_source.DerivationError) as caught:
                derive_source.derive(source, contract)
        self.assertEqual(caught.exception.code, "BOSS_SOURCE_REFERENCE_INVALID")


if __name__ == "__main__":
    unittest.main()
