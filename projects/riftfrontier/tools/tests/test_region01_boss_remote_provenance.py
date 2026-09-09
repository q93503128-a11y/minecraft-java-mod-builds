from __future__ import annotations

import hashlib
import json
import subprocess
import sys
import tempfile
import unittest
import urllib.error
import urllib.request
from pathlib import Path


SOURCE_CANDIDATES = {
    "flawlesshappiness/EmotionCreatures@215ae451": (
        "https://raw.githubusercontent.com/flawlesshappiness/EmotionCreatures/"
        "215ae451ba7690a3f765b01eea5295f37c120e5a/"
        "Assets/Quaternius/Ultimate%20Monsters/Flying/glTF/Dragon_Evolved.gltf"
    ),
    "laoniutoushx/TD-demo-2024-04-03@87051774": (
        "https://raw.githubusercontent.com/laoniutoushx/TD-demo-2024-04-03/"
        "87051774343f2a0df215639e8674178437228b71/"
        "Asserts/Models/ulimate%20monster/glTF/Dragon_Evolved.gltf"
    ),
    "mlflabs/brain@54b3258a": (
        "https://raw.githubusercontent.com/mlflabs/brain/"
        "54b3258a7ab6558fee969100aafb24ff285e0f0e/"
        "assets/Ultimate%20Monsters/Flying/glTF/Dragon_Evolved.gltf"
    ),
    "320trankt/warcell@60080076": (
        "https://raw.githubusercontent.com/320trankt/warcell/"
        "600800760d91da4070520d83cd860ab07b2892d3/"
        "assets/3d/Ultimate%20Monsters/Flying/glTF/Dragon_Evolved.gltf"
    ),
    "AlejandroMonteseirin/Godot_Monster_Ranger@13939757": (
        "https://raw.githubusercontent.com/AlejandroMonteseirin/Godot_Monster_Ranger/"
        "13939757cdfd59cbf0dbe5cce5dd32210b908f51/"
        "monsters/dragon2/Dragon_Evolved.gltf"
    ),
    "Letanyan/Spell-Magic@446a6104": (
        "https://raw.githubusercontent.com/Letanyan/Spell-Magic/"
        "446a6104b4a7bf4df09708a84bae656b0e20ba2f/"
        "Characters/Enemy/Flying/Dragoon/Dragon_Evolved.gltf"
    ),
}
EXPECTED_SOURCE_SHA256 = "39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c"
EXPECTED_SANITIZED_SHA256 = "ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac"
EXPECTED_PROVENANCE_SHA256 = "3e16877a0043cf980ac8de05bb518834c5e77bd72d96103b4984c65a2a5a4c6c"


class Region01BossRemoteProvenanceTest(unittest.TestCase):
    """One-shot immutable-mirror byte-equivalence proof for the pinned Quaternius source.

    Every candidate is accepted only if its complete bytes independently match the already-recorded source SHA-256.
    Candidate names, paths, or apparent glTF structure are never enough to promote a substitute.
    """

    def test_immutable_mirror_reproduces_accepted_derivation_and_animation_receipt(self) -> None:
        project_root = Path(__file__).resolve().parents[2]
        contract = project_root / "assets/sources/region_01_boss_dragon_evolved.source.json"
        acceptance = project_root / "assets/sources/region_01_boss_dragon_evolved.acceptance.json"

        observed: dict[str, str] = {}
        accepted_source: bytes | None = None
        accepted_candidate: str | None = None
        for candidate, url in SOURCE_CANDIDATES.items():
            request = urllib.request.Request(url, headers={"User-Agent": "Riftfrontier-source-audit/1"})
            try:
                with urllib.request.urlopen(request, timeout=60) as response:
                    source_bytes = response.read()
            except (urllib.error.HTTPError, urllib.error.URLError, TimeoutError) as exc:
                observed[candidate] = f"UNAVAILABLE:{type(exc).__name__}"
                continue
            actual_sha = hashlib.sha256(source_bytes).hexdigest()
            observed[candidate] = actual_sha
            if actual_sha == EXPECTED_SOURCE_SHA256:
                accepted_source = source_bytes
                accepted_candidate = candidate
                break

        self.assertIsNotNone(
            accepted_source,
            "no immutable mirror matched the pinned source SHA-256; observed=" + json.dumps(observed, sort_keys=True),
        )
        assert accepted_source is not None

        with tempfile.TemporaryDirectory(prefix="riftfrontier-region01-source-") as temporary:
            temp = Path(temporary)
            source = temp / "Dragon_Evolved.gltf"
            sanitized = temp / "region_01_boss_dragon_evolved.sanitized.v1.gltf"
            provenance = temp / "region_01_boss_dragon_evolved.sanitized.v1.provenance.json"
            receipt = temp / "region_01_boss_dragon_evolved.animation_audit.json"
            source.write_bytes(accepted_source)

            subprocess.run(
                [
                    sys.executable,
                    str(project_root / "tools/convert_region01_boss_geometry.py"),
                    "--contract", str(contract),
                    "--source", str(source),
                    "--output", str(sanitized),
                    "--provenance", str(provenance),
                ],
                cwd=project_root,
                check=True,
            )
            self.assertEqual(EXPECTED_SANITIZED_SHA256, hashlib.sha256(sanitized.read_bytes()).hexdigest())
            self.assertEqual(EXPECTED_PROVENANCE_SHA256, hashlib.sha256(provenance.read_bytes()).hexdigest())

            subprocess.run(
                [
                    sys.executable,
                    str(project_root / "tools/audit_region01_boss_animation_clips.py"),
                    str(sanitized),
                    str(acceptance),
                    "--output", str(receipt),
                ],
                cwd=project_root,
                check=True,
            )
            audit = json.loads(receipt.read_text(encoding="utf-8"))
            self.assertEqual(8, audit["clip_count"])
            self.assertEqual(EXPECTED_SANITIZED_SHA256, audit["source"]["sha256"])
            audit["reacquisition"] = {
                "candidate": accepted_candidate,
                "expected_source_sha256": EXPECTED_SOURCE_SHA256,
                "observed_candidates": observed,
            }
            print("REGION01_BOSS_SOURCE_RECEIPT=" + json.dumps(audit, sort_keys=True))


if __name__ == "__main__":
    unittest.main()
