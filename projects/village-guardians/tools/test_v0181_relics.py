#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    relic = read("VillageRelicSystem.java")
    rpg = read("VillageRpgSystem.java")
    role = read("VillageRoleSkillSystem.java")
    controller = read("VillageUiController.java")
    client = read("VillageClientUi.java")
    status = read("VillageStatusScreen.java")
    screen = read("VillageRelicScreen.java")

    assert "mod_version=" in props

    for token in [
        "result *= 1.15f", "result *= 1.18f", "result *= 0.86f",
        "result *= 1.20f", "return health <= maximumHealth * 0.30f ? 1.20f : 1.0f",
        "return has(player, Relic.CHRONO_SHARD) ? 4 : 0",
        "return has(player, Relic.BLOOD_CHALICE) ? 0.04f : 0.0f",
    ]:
        assert token in relic, token
    assert "마무리 전투 강화" not in relic
    print("[PASS] 11개 유물 수치가 상향되고 처형 효과 설명과 실제 계산이 일치합니다")

    assert "VillageRelicSystem.projectileMultiplier(attacker)" in rpg
    assert "VillageRelicSystem.meleeMultiplier(attacker)" in rpg
    assert "VillageRelicSystem.executionMultiplier(attacker" in rpg
    assert "VillageRelicSystem.incomingMultiplier(defender)" in rpg
    assert role.count("VillageRelicSystem.skillMultiplier(player)") >= 2
    assert "VillageRelicSystem.cooldownReductionSeconds(player)" in role
    assert "VillageRelicSystem.vanguardLifeStealBonus(attacker)" in read("VillageRoleAbilitySystem.java")
    print("[PASS] 근접·원거리·처형·방어·기술·쿨다운·흡혈 유물이 실제 전투 경로에 연결됩니다")

    assert "openCollection" in relic and "aggregateSummary" in relic
    assert "sanitizeMask" in relic and "ownedCount" in relic
    assert '"relic_collection"' in client
    assert 'List.of("open_relic_collection")' in controller
    assert 'case "open_relic_collection" -> openRelicCollection(player)' in controller
    assert "VillageRelicSystem.openCollection(player)" in controller
    print("[PASS] 상태 화면에서 플레이어별 유물 보관함으로 진입할 수 있습니다")

    assert "class VillageRelicScreen" in screen
    assert "columns = contentRight - contentLeft >= 610 ? 2 : 1" in screen
    assert "enableScissor" in screen and "mouseScrolled" in screen
    assert "CARD_HEIGHT = 68" in screen and "fit(" in screen
    assert "Math.min(7, summary.size())" in screen
    assert "actionSpace = action.isBlank() ? 0 : 34" in status
    assert "buttonWidth = Math.min(220" in status
    print("[PASS] 유물 UI는 좁은 화면 1열·넓은 화면 2열·스크롤·텍스트 제한으로 넘침을 방지합니다")

    enum_order = [
        "WAR_SIGIL", "HUNTERS_EYE", "WARD_STONE", "ARCANE_HEART", "EXECUTION_EDGE",
        "LAST_LIGHT", "CHRONO_SHARD", "BLOOD_CHALICE", "BASTION_CORE", "DAWN_PRISM",
        "STORM_FEATHER",
    ]
    cursor = -1
    for name in enum_order:
        next_cursor = relic.index(name + "(")
        assert next_cursor > cursor, name
        cursor = next_cursor
    assert "sanitizeMask(OWNED.getOrDefault" in relic
    print("[PASS] 기존 유물 비트 순서와 월드 저장 호환성을 유지하고 잘못된 마스크를 정리합니다")


if __name__ == "__main__":
    main()
