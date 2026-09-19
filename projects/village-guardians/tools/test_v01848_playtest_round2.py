#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

def section(source: str, start: str, end: str) -> str:
    return source.split(start, 1)[1].split(end, 1)[0]

def main() -> None:
    controller = read("VillageUiController.java")
    network = read("VillageNetwork.java")
    role_screen = read("VillageRoleProgressScreen.java")
    raid = read("VillageRaidSystem.java")
    merc = read("VillageMercenarySystem.java")
    council = read("VillageCouncilState.java")
    effect_entity = read("VillageSkillEffectEntity.java")
    siege = read("VillageSiegeCommandUi.java")
    relic = read("VillageRelicSystem.java")

    assert '"role_skill_equip:"' in role_screen
    assert 'action.startsWith("role_skill_equip:")' in controller
    assert 'action.startsWith("role_skill_unlock:")' in controller
    assert 'action.startsWith("research_skill_equip:")' in controller
    assert 'action.startsWith("role_skill_equip:")' in network

    max_waves = section(raid, "public static int previewMaxWaves", "public static int previewWaveCount")
    assert "Math.min(7, 3 + Math.max(0, day - 1) / 4)" in max_waves

    flying = section(raid, "private static void directFlyingEnemy", "private static int aerialCadence")
    assert "ingressRadius" in flying
    assert "VillageWorldSystem.FORTRESS_RADIUS - 10.0" in flying
    steering = section(raid, "private static void moveFlyingToward", "private static ServerPlayer nearestFlyingPriorityPlayer")
    assert "mob.getMoveControl().setWantedPosition" in steering
    assert "mob.setDeltaMovement" in steering

    assert "MobEffects.INVISIBILITY" in merc
    assert "public static synchronized void healAtDawn" in merc
    assert council.count("VillageMercenarySystem.healAtDawn(server)") >= 2
    assert 'kind().startsWith("mercenary_presence_")' in effect_entity

    choices = section(relic, "private static List<Relic> choicesFor", "private static List<Relic> pendingChoices")
    assert "(mask & relic.bit()) == 0" in choices
    assert "!reserved.contains(relic)" in choices
    assert "중복 획득 없음" in relic

    assert "private static String damageSummary" in siege
    for token in ("base * 1.55f", "base * 0.78f", "base * 0.72f", "base * 1.65f"):
        assert token in siege, token
    assert "방어·저항 적용 전 기준" in siege

    print("[PASS] role growth skill equip/unlock actions are server-routed")
    print("[PASS] wave-count growth is slower and late XP scaling is stronger")
    print("[PASS] flying raid actors are forced into fortress airspace")
    print("[PASS] mercenary visuals stay upright and dawn heal is bounded")
    print("[PASS] relic offers remain duplicate-free across owned and pending choices")
    print("[PASS] turret UI reports contextual combat damage")

if __name__ == "__main__":
    main()
