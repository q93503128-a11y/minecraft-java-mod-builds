from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/villageguardians'


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding='utf-8')
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{path}: expected one match, got {count}: {old[:100]!r}')
    path.write_text(text.replace(old, new, 1), encoding='utf-8')


props = ROOT / 'gradle.properties'
replace_once(props, 'mod_version=0.18.25-alpha.1', 'mod_version=0.18.26-alpha.1')

old_test = ROOT / 'tools/test_v01825_multifront_routing_integrity.py'
replace_once(old_test, '    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")\n', '')
replace_once(old_test, '    assert "mod_version=0.18.25-alpha.1" in props\n', '')

turret = JAVA / 'VillagePlacedTurretSystem.java'
replace_once(turret, '''        Mob target = candidates.stream().min(Comparator.comparingDouble(mob -> mob.distanceToSqr(Vec3.atCenterOf(state.pos())))).orElse(null);
        if (state.type() == TurretType.ANTI_AIR) {
            double baseY = VillageCouncilState.villageCenter().map(BlockPos::getY).orElse(state.pos().getY());
            target = candidates.stream().filter(mob -> mob.getY() > baseY + 6.0)
                    .min(Comparator.comparingDouble(mob -> mob.distanceToSqr(Vec3.atCenterOf(state.pos()))))
                    .orElse(target);
        }
''', '''        Mob target = selectTarget(level, state, candidates);
''')

replace_once(turret, '''            case NULLIFIER -> {
                hit(level, state, target, damage, ParticleTypes.ENCHANT);
                target.removeEffect(MobEffects.STRENGTH);
                target.removeEffect(MobEffects.REGENERATION);
            }
''', '''            case NULLIFIER -> {
                hit(level, state, target, damage, ParticleTypes.ENCHANT);
                target.removeEffect(MobEffects.STRENGTH);
                target.removeEffect(MobEffects.REGENERATION);
                target.removeEffect(MobEffects.SPEED);
                target.removeEffect(MobEffects.RESISTANCE);
                target.removeEffect(MobEffects.ABSORPTION);
            }
''')

anchor = '''    private static float piercingMultiplier(Mob target) {
'''
helper = '''    private static Mob selectTarget(ServerLevel level, TurretState state, List<Mob> candidates) {
        Vec3 origin = Vec3.atCenterOf(state.pos());
        return candidates.stream().max(Comparator.comparingDouble(mob -> targetScore(level, state, mob, origin))).orElse(null);
    }

    private static double targetScore(ServerLevel level, TurretState state, Mob mob, Vec3 origin) {
        VillageEnemyArchetypeSystem.Archetype archetype = VillageRaidSystem.archetypeOf(mob);
        double distance = Math.sqrt(Math.max(0.0, mob.position().distanceToSqr(origin)));
        double score = Math.max(0.0, 80.0 - distance);
        float maxHealth = Math.max(1.0f, mob.getMaxHealth());
        float healthRatio = Math.max(0.0f, Math.min(1.0f, mob.getHealth() / maxHealth));
        int cluster = VillageRaidSystem.activeEnemiesNear(level, mob.position(), 6.0, 10, null).size();

        switch (state.type()) {
            case BALLISTA -> {
                if (archetype != null && VillageEnemyArchetypeSystem.isBoss(archetype)) score += 120.0;
                if (archetype == VillageEnemyArchetypeSystem.Archetype.SIEGE_BEAST
                        || archetype == VillageEnemyArchetypeSystem.Archetype.SHIELDBREAKER
                        || archetype == VillageEnemyArchetypeSystem.Archetype.BULWARK) score += 75.0;
                score += healthRatio * 45.0;
            }
            case REPEATER -> {
                if (archetype == VillageEnemyArchetypeSystem.Archetype.RUSHER) score += 110.0;
                else if (archetype == VillageEnemyArchetypeSystem.Archetype.GRUNT) score += 55.0;
                score += (1.0 - healthRatio) * 65.0;
            }
            case PIERCER -> {
                if (isArmoredThreat(archetype, mob)) score += 150.0;
                if (archetype != null && VillageEnemyArchetypeSystem.isBoss(archetype)) score += 55.0;
            }
            case FLAME -> {
                if (mob.getRemainingFireTicks() <= 0) score += 55.0;
                score += cluster * 8.0;
                if (archetype == VillageEnemyArchetypeSystem.Archetype.RUSHER
                        || archetype == VillageEnemyArchetypeSystem.Archetype.GRUNT) score += 25.0;
            }
            case FROST -> {
                if (!mob.hasEffect(MobEffects.SLOWNESS)) score += 55.0;
                if (archetype == VillageEnemyArchetypeSystem.Archetype.RUSHER
                        || archetype == VillageEnemyArchetypeSystem.Archetype.SAPPER
                        || archetype == VillageEnemyArchetypeSystem.Archetype.TOWER_HUNTER) score += 115.0;
            }
            case CHAIN -> score += cluster * 26.0;
            case BOMBARD -> score += cluster * 34.0;
            case NULLIFIER -> {
                if (isSupportThreat(archetype)) score += 145.0;
                if (hasDispellableBuff(mob)) score += 90.0;
            }
            case ANTI_AIR -> {
                if (isHighAngleThreat(archetype)) score += 145.0;
                double baseY = VillageCouncilState.villageCenter().map(BlockPos::getY).orElse(state.pos().getY());
                if (mob.getY() > baseY + 5.0) score += 100.0;
            }
            default -> { }
        }
        return score;
    }

    private static boolean isArmoredThreat(VillageEnemyArchetypeSystem.Archetype type, Mob mob) {
        if (type == null) return mob.hasEffect(MobEffects.RESISTANCE);
        return switch (type) {
            case BULWARK, SHIELDBREAKER, SIEGE_BEAST, IRON_WARLORD, DREAD_KNIGHT -> true;
            default -> mob.hasEffect(MobEffects.RESISTANCE);
        };
    }

    private static boolean isSupportThreat(VillageEnemyArchetypeSystem.Archetype type) {
        return type == VillageEnemyArchetypeSystem.Archetype.HEXER
                || type == VillageEnemyArchetypeSystem.Archetype.WAR_CHANTER
                || type == VillageEnemyArchetypeSystem.Archetype.NECROMANCER
                || type == VillageEnemyArchetypeSystem.Archetype.PLAGUE_ARCHON
                || type == VillageEnemyArchetypeSystem.Archetype.IRON_WARLORD;
    }

    private static boolean isHighAngleThreat(VillageEnemyArchetypeSystem.Archetype type) {
        return type == VillageEnemyArchetypeSystem.Archetype.MARKSMAN
                || type == VillageEnemyArchetypeSystem.Archetype.HEXER
                || type == VillageEnemyArchetypeSystem.Archetype.WAR_CHANTER
                || type == VillageEnemyArchetypeSystem.Archetype.NECROMANCER
                || type == VillageEnemyArchetypeSystem.Archetype.TOWER_HUNTER;
    }

    private static boolean hasDispellableBuff(Mob mob) {
        return mob.hasEffect(MobEffects.STRENGTH)
                || mob.hasEffect(MobEffects.REGENERATION)
                || mob.hasEffect(MobEffects.SPEED)
                || mob.hasEffect(MobEffects.RESISTANCE)
                || mob.hasEffect(MobEffects.ABSORPTION);
    }

'''
replace_once(turret, anchor, helper + anchor)

replace_once(turret,
    '        ANTI_AIR("anti_air", "대공 발사대", 19, 24, 72, 260, 220, Blocks.IRON_BARS, "고고도 우선 사격"),',
    '        ANTI_AIR("anti_air", "고각 요격포", 19, 24, 72, 260, 220, Blocks.IRON_BARS, "장거리 원거리·지원병 우선 요격"),')

readme = ROOT / 'README.md'
replace_once(readme, '- 현재 소스 버전 `0.18.25-alpha.1`', '- 현재 소스 버전 `0.18.26-alpha.1`')
replace_once(readme, '- 목표 JAR `villageguardians-0.18.25-alpha.1.jar`', '- 목표 JAR `villageguardians-0.18.26-alpha.1.jar`')
marker = '## 0.18.25 다전선 경로 소유권·돌파 후 침입 정합\n'
insert = '''## 0.18.26 포탑 역할·타깃 우선순위 정합

- 10종 포탑이 모두 단순히 가장 가까운 적을 찍던 타깃 선택을 역할별 위협 점수 방식으로 교체했다. 중쇠뇌는 보스/공성 중량 적, 연사 포탑은 척후·마무리, 관통포는 중장갑, 화염/연쇄/투석은 군집, 서리는 고기동 공병, 억제탑은 버프·지원병을 우선한다.
- `anti_air` 저장 ID는 호환성을 위해 유지하지만 실제 적군에 비행 병종이 없던 현재 콘텐츠와 맞지 않던 표시명은 `고각 요격포`로 바로잡았다. 고각 요격포는 장거리 사수·주술사·지원병·탑 사냥꾼과 높은 지형의 표적을 우선한다.
- 마법 억제탑은 공격력/재생만 지우던 반쪽 역할에서 벗어나 속도·저항·흡수까지 실제 제거해 War Chanter, Necromancer, 지휘 보스의 강화와 정면으로 상호작용한다.
- 광역·연쇄 포탑은 적 군집 밀도를 타깃 점수에 반영해 단일 선두 적에게 광역 화력을 낭비하는 빈도를 줄였다.

'''
replace_once(readme, marker, insert + marker)

test = ROOT / 'tools/test_v01826_turret_role_integrity.py'
test.write_text('''#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    turret = read("VillagePlacedTurretSystem.java")
    old = (ROOT / "tools/test_v01825_multifront_routing_integrity.py").read_text(encoding="utf-8")
    assert "mod_version=0.18.26-alpha.1" in props
    assert "0.18.26-alpha.1" in readme and "villageguardians-0.18.26-alpha.1.jar" in readme
    assert 'assert "mod_version=0.18.25-alpha.1" in props' not in old
    assert "Mob target = selectTarget(level, state, candidates);" in turret
    assert "private static double targetScore" in turret
    for role in ("BALLISTA", "REPEATER", "PIERCER", "FLAME", "FROST", "CHAIN", "BOMBARD", "NULLIFIER", "ANTI_AIR"):
        assert f"case {role}" in turret
    for archetype in ("RUSHER", "SAPPER", "TOWER_HUNTER", "BULWARK", "SHIELDBREAKER", "SIEGE_BEAST", "WAR_CHANTER", "NECROMANCER", "MARKSMAN"):
        assert f"Archetype.{archetype}" in turret
    assert "cluster * 26.0" in turret and "cluster * 34.0" in turret
    assert "isArmoredThreat" in turret and "isSupportThreat" in turret and "isHighAngleThreat" in turret
    for effect in ("STRENGTH", "REGENERATION", "SPEED", "RESISTANCE", "ABSORPTION"):
        assert f"target.removeEffect(MobEffects.{effect})" in turret
    assert 'ANTI_AIR("anti_air", "고각 요격포"' in turret
    assert "장거리 원거리·지원병 우선 요격" in turret
    assert '"대공 발사대"' not in turret
    print("[PASS] v0.18.26 turret role integrity contract complete")

if __name__ == "__main__":
    main()
''', encoding='utf-8')

print('[PASS] applied v0.18.26 corrected patch')
