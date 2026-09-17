#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match in {path}, found {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def main() -> None:
    raid = JAVA / "VillageRaidSystem.java"
    replace_once(
        raid,
'''    private static void updateEnemyOutline(MinecraftServer server, Mob mob) {
        boolean visibleToAnyPlayer = false;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() == mob.level()
                    && player.isAlive()
                    && player.distanceToSqr(mob) <= 160.0 * 160.0
                    && player.hasLineOfSight(mob)) {
                visibleToAnyPlayer = true;
                break;
            }
        }
        VillageEnemyArchetypeSystem.Archetype archetype = archetypeOf(mob);
        boolean tactical = VillageEnemyArchetypeSystem.isFlying(mob)
                || VillageEnemyArchetypeSystem.isTacticalThreat(archetype);
        mob.setGlowingTag(isBossEnemy(mob) || (tactical && !visibleToAnyPlayer));
    }
''',
'''    private static void updateEnemyOutline(MinecraftServer server, Mob mob) {
        boolean visibleToAnyPlayer = false;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() == mob.level()
                    && player.isAlive()
                    && !player.isSpectator()
                    && !VillageRespawnSystem.isDowned(player)
                    && player.distanceToSqr(mob) <= 160.0 * 160.0
                    && player.hasLineOfSight(mob)) {
                visibleToAnyPlayer = true;
                break;
            }
        }
        // Fortress walls should not turn cleanup into hide-and-seek. Every occluded raid enemy is
        // outlined in the raid team's red color; bosses stay outlined even when directly visible.
        mob.setGlowingTag(isBossEnemy(mob) || !visibleToAnyPlayer);
    }
''',
        "restore occluded raid outline",
    )

    props = ROOT / "gradle.properties"
    replace_once(props, "mod_version=0.18.35-alpha.1", "mod_version=0.18.36-alpha.1", "version bump")

    readme = ROOT / "README.md"
    replace_once(readme, "- 현재 소스 버전 `0.18.35-alpha.1`\n- 목표 JAR `villageguardians-0.18.35-alpha.1.jar`",
                 "- 현재 소스 버전 `0.18.36-alpha.1`\n- 목표 JAR `villageguardians-0.18.36-alpha.1.jar`", "README version")
    text = readme.read_text(encoding="utf-8")
    marker = "## 0.18.35 성벽 계단 상단 접속부 실플레이 수정\n"
    if marker not in text:
        raise RuntimeError("README 0.18.35 section marker missing")
    section = '''## 0.18.36 실플레이 전장 가독성·회귀 재감사\n\n- 성벽·건물 뒤에 가려진 **모든 활성 습격 적**이 다시 `vg_raid` 빨간 외곽선으로 표시된다. 눈앞에 직접 보이는 일반 적은 외곽선을 끄고, 보스는 시야 여부와 관계없이 항상 빨간 외곽선을 유지한다.\n- 0.18.29에서 일반 적의 벽 너머 외곽선을 전술/공중 적으로 제한했던 규칙을 실플레이 피드백에 따라 되돌렸다. 성벽 방어 중 잔존 일반 적을 찾기 위해 벽 둘레를 반복 탐색하는 불필요한 시간을 없애는 것이 목적이다.\n- 관전자와 다운 상태 플레이어는 적의 `보임` 판정에 참여하지 않는다. 실제 전투 가능한 플레이어에게 가려진 적의 외곽선이 다른 비전투 플레이어 때문에 꺼지지 않는다.\n- 이름표 가시성은 기존 규칙을 유지한다. 일반 적 이름표를 상시 노출하지 않고, 보스·공중·전술 위협의 식별 계약도 그대로 보존한다.\n- 전장 가독성 변경과 함께 AI 이동 소유권, 활성 적 UUID 명부, 웨이브/공중 경고, 회관·시설 권한, 포탑/성벽 정비, 용병 배치, HUD/modal 억제, 0.18.35 성벽 계단 동선 계약을 다시 회귀검사한다.\n\n'''
    readme.write_text(text.replace(marker, section + marker, 1), encoding="utf-8")

    old = ROOT / "tools/test_v01829_battlefield_readability.py"
    replace_once(old,
'''    assert "(tactical && !visibleToAnyPlayer)" in raid
    assert "isBossEnemy(mob) || !visibleToAnyPlayer" not in raid
''',
'''    assert "isBossEnemy(mob) || !visibleToAnyPlayer" in raid
    assert "(tactical && !visibleToAnyPlayer)" not in raid
    assert "!player.isSpectator()" in raid
    assert "!VillageRespawnSystem.isDowned(player)" in raid
''', "v01829 outline contract")
    replace_once(old,
'''    print("[PASS] cover outlines no longer reveal every generic enemy through fortress walls")
''',
'''    print("[PASS] occluded raid enemies use red cover outlines while directly visible generic enemies stay clean")
''', "v01829 outline report")

    v35 = ROOT / "tools/test_v01835_wall_access_climb.py"
    replace_once(v35,
                 '    assert "mod_version=0.18.35-alpha.1" in props\n',
                 '    assert "mod_version=" in props\n',
                 "v01835 historical version independence")

    print("[PATCH] v0.18.36 battlefield readability and regression-audit patch applied")


if __name__ == "__main__":
    main()
