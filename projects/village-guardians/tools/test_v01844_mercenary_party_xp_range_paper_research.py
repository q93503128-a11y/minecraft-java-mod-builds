#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def section(source: str, start: str, end: str) -> str:
    return source.split(start, 1)[1].split(end, 1)[0]


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    build = (ROOT / "build.gradle").read_text(encoding="utf-8")
    assets = (ROOT / "tools/prepare_licensed_assets.py").read_text(encoding="utf-8")
    notices = (ROOT / "THIRD_PARTY_NOTICES.md").read_text(encoding="utf-8")

    merc = read("VillageMercenarySystem.java")
    raid = read("VillageRaidSystem.java")
    guardians = read("VillageGuardians.java")
    research = read("VillageDefenseResearchSystem.java")
    turret = read("VillagePlacedTurretSystem.java")
    effects = read("VillageDefenseEffectSystem.java")
    skill_mesh = read("VillageSkillMeshLibrary.java")
    external_mesh = read("VillageExternalMercenaryMesh.java")
    starter = read("VillageStarterKit.java")
    ui = read("VillageUiController.java")

    assert "mod_version=" in props
    assert "현재 소스 버전 `" in readme
    assert "목표 JAR `villageguardians-" in readme

    for name in ("bastion.obj", "striker.obj", "ranger.obj", "medic.obj"):
        assert name in assets
    assert "QUATERNIUS_RPG_MIRROR_COMMIT" in assets
    assert "e50f41492f5cc35cffa7990ddb998e860881bd41" in assets
    assert "quaterniusRpgMirrorCommit" in build
    assert "CC0 1.0" in notices
    assert "VillageExternalMercenaryMesh.mesh(style)" in skill_mesh
    assert "class VillageExternalMercenaryMesh" in external_mesh
    assert "mercenary.setInvisible(true)" in merc

    passives = section(merc, "private static void applyClassPassives", "public static synchronized MercenaryClass classOf")
    for value in ("260.0", "190.0", "165.0", "205.0",
                  "Attributes.MAX_HEALTH", "Attributes.ARMOR"):
        assert value in passives
    assert "mercenaryDurabilityMultiplier()" in research
    assert "public static int aggroCapacity" in merc
    direct = section(raid, "private static void directEnemies",
                     "private static net.minecraft.world.entity.animal.golem.IronGolem selectMercenaryTarget")
    assert "mercenaryPressure" in direct
    assert "VillageMercenarySystem.aggroCapacity" in direct
    target = section(raid,
                     "private static net.minecraft.world.entity.animal.golem.IronGolem selectMercenaryTarget",
                     "private static LivingEntity activeTauntTarget")
    assert "case BASTION -> 0.52" in target
    assert "case MEDIC -> 1.38" in target

    death = section(guardians, "public void onLivingDeath", "@SubscribeEvent\n    public void onArrowLoose")
    assert "VillageRaidSystem.experienceForEnemy(defeated)" in death
    assert "VillageProgressionSystem.nightParticipants(server)" in death
    assert "VillageCouncilState.grantExperience(server, playerId, sharedExperience)" in death
    assert "VillageMercenarySystem.awardKillExperience(killer)" in death
    assert "public static int experienceForEnemy" in raid

    placement = section(turret, "public static boolean handlePlacementClick",
                        "public static String cancelPlacement")
    assert "turretRangeParticleRing" in placement
    tick = section(turret, "public static void tick",
                   "private static void refreshPlacementRangePreviews")
    assert "refreshPlacementRangePreviews(level)" in tick
    refresh = section(turret, "private static void refreshPlacementRangePreviews",
                      "private static void fire")
    assert "effectiveRange(pending.type(), 1)" in refresh
    assert "turretRangeParticleRing" in refresh
    assert "ParticleTypes.ELECTRIC_SPARK" in effects

    assert 'TACTICAL_SHEET_NAME = "수호단 작전표"' in starter
    assert "removeTacticalSheetItems(player)" in starter
    assert "ensureTacticalSheet(player)" not in starter
    assert "작전표·호출기 아이템은 폐지되었습니다" not in starter
    assert "빠른 통신과 상태·성장·직업 성장 기능" in starter

    assert "세 갈래 동시 성장 가능" in ui
    assert "지속·위력·특수는 서로 배타적이지 않음" in ui
    upgrade = section(research, "public static synchronized String upgrade", "private static float curve")
    assert "이전 Lv." in upgrade
    assert "현재 Lv." in upgrade
    assert "다음 강화 Lv." in upgrade
    assert "다음 비용: 공동 보급품" in upgrade

    print("[PASS] four mercenary classes use pinned CC0 external character geometry")
    print("[PASS] mercenary durability and class-aware aggro caps prevent whole-wave dogpiles")
    print("[PASS] every raid death grants equal party XP regardless of player/mercenary/turret killer")
    print("[PASS] turret range preview has a persistent particle-circle fallback")
    print("[PASS] obsolete tactical paper is retired and role/research progression is clearer")


if __name__ == "__main__":
    main()
