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
    town = read("VillageTownHallGridScreen.java")
    confirm = read("VillageConfirmScreen.java")
    starter = read("VillageStarterKit.java")
    raid = read("VillageRaidSystem.java")
    guardians = read("VillageGuardians.java")

    assert "mod_version=0.18.48-alpha.1" in props
    assert "현재 소스 버전 `0.18.48-alpha.1`" in readme
    assert "villageguardians-0.18.48-alpha.1.jar" in readme

    # Confirmation must compare authoritative current/next values rather than generic help prose.
    click = section(town, "public boolean mouseClicked", "private static String confirmationDetail")
    assert "confirmationDetail(facility, spec.action())" in click
    compare = section(town, "private static String confirmationDetail", "private static String nextStageLabel")
    assert '"현재 · "' in compare
    assert "facility.effect()" in compare
    assert '"다음 · "' in compare
    assert "facility.nextEffect()" in compare
    assert '"필요 공동 보급품 · "' in compare
    assert '"현재 내구도 · "' in compare and '"수리 후 · "' in compare
    assert "Math.min(260" in confirm

    # Old physical menu items are removed while player-facing guidance describes only current controls.
    assert "ensureTacticalSheet(player)" not in starter
    assert "removeTacticalSheetItems(player)" in starter
    assert "작전표·호출기 아이템은 폐지되었습니다" not in starter
    assert "빠른 통신과 상태·성장·직업 성장 기능" in starter
    assert "player.setItemInHand(event.getHand(), ItemStack.EMPTY)" in starter

    # Party sharing follows the target-level XP requirement so days 20-100 remain levelable.
    xp = section(raid, "public static int experienceForEnemy", "public static VillageEnemyArchetypeSystem.AerialRole")
    assert "VillageCampaignProgression.targetPlayerLevel(day)" in xp
    assert "RpgProgress.experienceRequiredAtLevel(targetLevel)" in xp
    assert "expectedThreatsPerLevel(day)" in xp
    assert "VillageEnemyEliteSystem.isElite(mob)" in xp
    death = section(guardians, "public void onLivingDeath", "public void onArrowLoose")
    assert "VillageRaidSystem.experienceForEnemy(defeated)" in death
    assert "VillageProgressionSystem.nightParticipants(server)" in death

    print("[PASS] facility confirmation compares current -> next effect and exact shared-supply cost")
    print("[PASS] obsolete tactical-sheet item is removed instead of reissued")
    print("[PASS] shared raid XP scales from the current campaign target-level requirement")

if __name__ == "__main__":
    main()
