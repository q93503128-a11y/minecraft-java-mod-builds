from __future__ import annotations

import hashlib
import json
import subprocess
import sys
import tempfile
import unittest
import urllib.request
from pathlib import Path


SOURCE_URL = (
    "https://raw.githubusercontent.com/flawlesshappiness/EmotionCreatures/"
    "215ae451ba7690a3f765b01eea5295f37c120e5a/"
    "Assets/Quaternius/Ultimate%20Monsters/Flying/glTF/Dragon_Evolved.gltf"
)
EXPECTED_SOURCE_SHA256 = "39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c"
EXPECTED_SANITIZED_SHA256 = "ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac"
EXPECTED_PROVENANCE_SHA256 = "3e16877a0043cf980ac8de05bb518834c5e77bd72d96103b4984c65a2a5a4c6c"


class Region01BossRemoteProvenanceTest(unittest.TestCase):
    """One-shot immutable-mirror byte-equivalence proof for the pinned Quaternius source.

    This intentionally accepts the mirror only when its complete bytes independently match the already-recorded
    source SHA-256. The mirror commit and file URL are immutable; a mismatch is a hard failure, not a substitute.
    """

    def test_immutable_mirror_reproduces_accepted_derivation_and_animation_receipt(self) -> None:
        project_root = Path(__file__).resolve().parents[2]
        contract = project_root / "assets/sources/region_01_boss_dragon_evolved.source.json"
        acceptance = project_root / "assets/sources/region_01_boss_dragon_evolved.acceptance.json"
        report_dir = project_root / "build/reports/riftfrontier"
        report_dir.mkdir(parents=True, exist_ok=True)

        request = urllib.request.Request(SOURCE_URL, headers={"User-Agent": "Riftfrontier-source-audit/1"})
        with urllib.request.urlopen(request, timeout=60) as response:
            source_bytes = response.read()
        self.assertEqual(EXPECTED_SOURCE_SHA256, hashlib.sha256(source_bytes).hexdigest())

        with tempfile.TemporaryDirectory(prefix="riftfrontier-region01-source-") as temporary:
            temp = Path(temporary)
            source = temp / "Dragon_Evolved.gltf"
            sanitized = temp / "region_01_boss_dragon_evolved.sanitized.v1.gltf"
            provenance = temp / "region_01_boss_dragon_evolved.sanitized.v1.provenance.json"
            receipt = report_dir / "region_01_boss_dragon_evolved.animation_audit.json"
            source.write_bytes(source_bytes)

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


if __name__ == "__main__":
    unittest.main()
