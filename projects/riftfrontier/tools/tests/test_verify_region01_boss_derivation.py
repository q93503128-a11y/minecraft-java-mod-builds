import base64
import hashlib
import importlib.util
import json
from pathlib import Path

import pytest


MODULE_PATH = Path(__file__).resolve().parents[1] / "verify_region01_boss_derivation.py"
SPEC = importlib.util.spec_from_file_location("verify_region01_boss_derivation", MODULE_PATH)
VERIFY = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(VERIFY)


def _canonical(value):
    return (json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n").encode("utf-8")


def _fixture(tmp_path: Path):
    payload = b"\x00\x01\x02\x03"
    geometry = {
        "asset": {"version": "2.0", "generator": "Riftfrontier deterministic boss geometry sanitizer v1"},
        "scene": 0,
        "scenes": [{"nodes": [0]}],
        "nodes": [{"name": "Root", "mesh": 0, "skin": 0}],
        "meshes": [{"primitives": [{"attributes": {"POSITION": 0}, "indices": 1}]}],
        "skins": [{"joints": [0]}],
        "animations": [{"name": "Punch", "channels": [], "samplers": []}],
        "accessors": [
            {"bufferView": 0, "componentType": 5126, "count": 1, "type": "VEC3", "min": [-1.0, 0.0, -2.0], "max": [1.0, 3.0, 2.0]},
            {"bufferView": 0, "componentType": 5123, "count": 3, "type": "SCALAR"},
        ],
        "bufferViews": [{"buffer": 0, "byteOffset": 0, "byteLength": 4}],
        "buffers": [{"byteLength": 4, "uri": "data:application/octet-stream;base64," + base64.b64encode(payload).decode("ascii")}],
    }
    geometry_path = tmp_path / "geometry.gltf"
    geometry_path.write_bytes(_canonical(geometry))
    geometry_sha = hashlib.sha256(geometry_path.read_bytes()).hexdigest()

    provenance = {
        "kind": "riftfrontier:boss_geometry_derivation",
        "schema_version": 1,
        "source_id": "fixture",
        "source_sha256": "a" * 64,
        "output_sha256": geometry_sha,
    }
    provenance_path = tmp_path / "provenance.json"
    provenance_path.write_bytes(_canonical(provenance))
    provenance_sha = hashlib.sha256(provenance_path.read_bytes()).hexdigest()

    receipt = {
        "kind": "riftfrontier:boss_geometry_acceptance",
        "schema_version": 1,
        "source": {"sha256": "a" * 64},
        "output": {"geometry_sha256": geometry_sha, "provenance_sha256": provenance_sha},
        "expect": {
            "mesh_count": 1,
            "skin_count": 1,
            "joint_count": 1,
            "node_count": 1,
            "accessor_count": 2,
            "buffer_view_count": 1,
            "embedded_buffer_bytes": 4,
            "bounds_min": [-1.0, 0.0, -2.0],
            "bounds_max": [1.0, 3.0, 2.0],
            "animations": ["Punch"],
        },
    }
    receipt_path = tmp_path / "receipt.json"
    receipt_path.write_bytes(_canonical(receipt))
    return receipt_path, geometry_path, provenance_path, geometry


def test_accepts_pinned_art_neutral_derivation(tmp_path):
    receipt, geometry, provenance, _ = _fixture(tmp_path)

    result = VERIFY.verify(receipt, geometry, provenance)

    assert result["art_payload_stripped"] is True
    assert result["mesh_count"] == 1
    assert result["joint_count"] == 1
    assert result["animation_count"] == 1


def test_rejects_material_payload_even_when_hashes_are_repinned(tmp_path):
    receipt_path, geometry_path, provenance_path, geometry = _fixture(tmp_path)
    geometry["materials"] = [{"name": "Forbidden"}]
    geometry["meshes"][0]["primitives"][0]["material"] = 0
    geometry_path.write_bytes(_canonical(geometry))

    receipt = json.loads(receipt_path.read_text(encoding="utf-8"))
    provenance = json.loads(provenance_path.read_text(encoding="utf-8"))
    geometry_sha = hashlib.sha256(geometry_path.read_bytes()).hexdigest()
    provenance["output_sha256"] = geometry_sha
    provenance_path.write_bytes(_canonical(provenance))
    receipt["output"]["geometry_sha256"] = geometry_sha
    receipt["output"]["provenance_sha256"] = hashlib.sha256(provenance_path.read_bytes()).hexdigest()
    receipt_path.write_bytes(_canonical(receipt))

    with pytest.raises(VERIFY.VerificationError) as exc:
        VERIFY.verify(receipt_path, geometry_path, provenance_path)

    assert exc.value.code == "BOSS_DERIVATION_ART_POLICY_VIOLATION"


def test_rejects_geometry_sha_drift(tmp_path):
    receipt, geometry, provenance, _ = _fixture(tmp_path)
    geometry.write_bytes(geometry.read_bytes() + b" ")

    with pytest.raises(VERIFY.VerificationError) as exc:
        VERIFY.verify(receipt, geometry, provenance)

    assert exc.value.code == "BOSS_DERIVATION_SHA256_MISMATCH"
