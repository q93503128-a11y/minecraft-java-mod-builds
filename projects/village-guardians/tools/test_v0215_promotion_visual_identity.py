#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"
MESH = (JAVA / "VillageSkillMeshLibrary.java").read_text(encoding="utf-8")
SKILLS = (JAVA / "VillageRoleSkillSystem.java").read_text(encoding="utf-8")
EFFECTS = (JAVA / "VillageSkillEffectSystem.java").read_text(encoding="utf-8")
CLIENT = (JAVA / "VillageSkillEffectClient.java").read_text(encoding="utf-8")

PROMOTION = re.findall(
    r'^\s*([A-Z0-9_]+)\("([a-z0-9_]+)", VillageRole\.([A-Z]+), (\d+), "([^"]+)"',
    SKILLS,
    re.MULTILINE,
)
PROMOTION = [entry for entry in PROMOTION if int(entry[3]) >= 4]

HELPERS = {
    "bladeFan", "battleStandard", "batteringWedge", "crossedScythes", "siphonTendrils",
    "armoredChargeWedge", "giantSkyBlade", "bowArc", "hawkCrest", "antiAirCrown",
    "arrowCanopy", "constellationWeb", "stellarLance", "skyCage", "meteorSpear",
    "craterBurst", "moltenCore", "iceCage", "lightningNodeWeb", "gravityFunnel",
    "sunCorona", "sunRays", "snowflakeSigil", "iceBloom", "thunderLattice",
    "singularityCore", "guardianHalo", "holyCross", "expandingCrossWave", "lifeWave",
    "judgementMark", "sanctuaryDome", "returnBeacon", "choirCrown", "sacredMandala",
    "gateFrame", "shieldRam", "challengeCrown", "fourWallBarrier", "shieldPlateWave",
    "fortressWall", "fortressRam", "phalanxRing", "fortressKeepAt",
}
SECOND = {
    "vanguard_sword_chain", "vanguard_life_sever", "vanguard_absolute_break", "vanguard_heaven_sever",
    "ranger_star_tracker", "ranger_constellation", "ranger_sky_lock", "ranger_meteor_bow",
    "arcanist_solar_core", "arcanist_absolute_zero", "arcanist_heaven_chain", "arcanist_singularity",
    "luminar_heavenly_barrier", "luminar_returning_light", "luminar_resurrection_hymn", "luminar_last_miracle",
    "warden_unbroken_wall", "warden_fortress_charge", "warden_absolute_formation", "warden_fortress_descent",
}


def case_body(skill_id: str) -> str:
    start = MESH.index(f'case "{skill_id}" -> {{')
    brace = MESH.index("{", start)
    depth = 0
    for idx in range(brace, len(MESH)):
        if MESH[idx] == "{":
            depth += 1
        elif MESH[idx] == "}":
            depth -= 1
            if depth == 0:
                return MESH[brace + 1:idx]
    raise AssertionError(f"unterminated case: {skill_id}")


def main() -> None:
    assert len(PROMOTION) == 40, len(PROMOTION)
    assert "isSecondPromotionSkill" in MESH
    assert "double tierScale = second ? 1.24 : 1.0;" in MESH

    used_signatures = {}
    for constant, skill_id, role, index, name in PROMOTION:
        body = case_body(skill_id)
        used = sorted(helper for helper in HELPERS if re.search(rf"\b{helper}\s*\(", body))
        assert used, f"{skill_id}: no authored promotion helper"
        used_signatures[skill_id] = tuple(used)

        # No promotion skill may regress to a single generic ring/shield/rune primitive.
        generic_only = {"ring", "ringVertical", "runeDisc", "curvedShield", "verticalPillar"}
        generic_calls = set(re.findall(
            r"\b(ring|ringVertical|runeDisc|curvedShield|verticalPillar)\s*\(", body
        ))
        if not used:
            assert generic_calls - generic_only, skill_id

        if skill_id in SECOND:
            assert any(token in body for token in (
                "tierScale", "readableRadius", "phase == 3", "phase >= 2"
            )), f"{skill_id}: second promotion lacks staged escalation"

    assert set(SECOND).issubset({skill_id for _, skill_id, _, _, _ in PROMOTION})
    for skill_id in SECOND:
        assert f'"{skill_id}"' in MESH.split("isSecondPromotionSkill", 1)[1], skill_id

    # High-risk identities must keep their literal silhouette primitives.
    contracts = {
        "vanguard_war_banner": "battleStandard",
        "vanguard_heaven_sever": "giantSkyBlade",
        "ranger_hawk_mark": "hawkCrest",
        "ranger_meteor_bow": "meteorSpear",
        "arcanist_frost_prison": "iceCage",
        "arcanist_solar_core": "sunCorona",
        "arcanist_singularity": "singularityCore",
        "luminar_heavenly_barrier": "sanctuaryDome",
        "luminar_resurrection_hymn": "choirCrown",
        "warden_gate_impact": "gateFrame",
        "warden_unbroken_wall": "fortressWall",
        "warden_absolute_formation": "phalanxRing",
        "warden_fortress_descent": "fortressKeepAt",
    }
    for skill_id, helper in contracts.items():
        assert helper in case_body(skill_id), f"{skill_id}: missing {helper}"

    # The redesign must retain procedural meshes and avoid particle/item-model shortcuts.
    assert "ParticleTypes" not in MESH
    assert "ItemDisplay" not in MESH
    assert "BlockDisplay" not in MESH

    assert '"promotion:" + skill.id()' in EFFECTS
    assert 'motion.name.startsWith("promotion:")' in CLIENT
    for skill_id in (
        "vanguard_heaven_sever", "ranger_meteor_bow", "arcanist_singularity",
        "luminar_last_miracle", "warden_fortress_descent",
    ):
        assert f'case "{skill_id}"' in CLIENT, skill_id

    print("[PASS] all 40 promotion skills retain individually authored silhouette branches")
    print("[PASS] all 20 second-promotion skills retain staged visual escalation")
    print("[PASS] signature skills retain bow, meteor, prison, sun, singularity, sanctuary and fortress motifs")
    print("[PASS] promotion casts drive role body motion and five final skills keep stronger signature poses")


if __name__ == "__main__":
    main()
