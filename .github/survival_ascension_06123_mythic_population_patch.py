from pathlib import Path

ROOT = Path('projects/survival-ascension')
OLD = '0.61.22-alpha.1'
NEW = '0.61.23-alpha.1'


def read(rel):
    return (ROOT / rel).read_text(encoding='utf-8')


def write(rel, text):
    (ROOT / rel).write_text(text, encoding='utf-8')


def once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly one match, got {count}')
    return text.replace(old, new, 1)

# Runtime: Mythic III must be rare, region-bounded, and cheap enough that leaving one alive
# does not turn ordinary survival into permanent alert/entity pressure.
path = 'src/main/java/kr/moonseungjun/survivalascension/elite/EliteMobSystem.java'
text = read(path)
text = once(text,
'''    private static final double MYTHIC_ALERT_RADIUS = 192.0D;\n    private static final double MYTHIC_BOSSBAR_RADIUS = 128.0D;\n    private static final double MYTHIC_REWARD_RADIUS = 48.0D;''',
'''    private static final double MYTHIC_ALERT_RADIUS = 128.0D;\n    private static final double MYTHIC_TRACKER_RADIUS = 192.0D;\n    private static final double MYTHIC_BOSSBAR_RADIUS = 128.0D;\n    private static final double MYTHIC_REWARD_RADIUS = 48.0D;\n    private static final double MYTHIC_LOCAL_CAP_RADIUS = 256.0D;\n    private static final int MYTHIC_DIMENSION_CAP = 3;\n    private static final int MYTHIC_RUNTIME_INTERVAL_TICKS = 20;''',
'elite constants')
text = once(text,
'''        Rank rank = chooseRank(random, power, worldStage);\n        Trait trait = Trait.values()[random.nextInt(Trait.values().length)];\n        applyElite(mob, rank, trait, nearby.size());''',
'''        Rank rank = chooseRank(random, power, worldStage);\n        if (rank == Rank.MYTHIC_III && !canAdmitMythic(level, mob)) rank = Rank.ASCENDED_II;\n        Trait trait = Trait.values()[random.nextInt(Trait.values().length)];\n        applyElite(mob, rank, trait, nearby.size());''',
'mythic admission')
text = once(text,
'''        if (++mythicTicker < 10) return;''',
'''        if (++mythicTicker < MYTHIC_RUNTIME_INTERVAL_TICKS) return;''',
'runtime cadence')
text = once(text,
'''        double mythicChance = Math.min(0.08D, 0.004D + power * 0.00035D + worldStage * 0.012D);''',
'''        double mythicChance = Math.min(0.012D, 0.0008D + power * 0.00005D + worldStage * 0.0012D);''',
'mythic chance')
text = once(text,
'''    private static void applyElite(Mob mob, Rank rank, Trait trait, int nearbyPlayers) {''',
'''    private static boolean canAdmitMythic(ServerLevel level, Mob candidate) {\n        int activeInDimension = 0;\n        double localRadiusSqr = MYTHIC_LOCAL_CAP_RADIUS * MYTHIC_LOCAL_CAP_RADIUS;\n        for (Map.Entry<UUID, MythicRuntime> entry : MYTHICS.entrySet()) {\n            MythicRuntime runtime = entry.getValue();\n            if (runtime.level != level) continue;\n            Entity entity = level.getEntity(entry.getKey());\n            if (!(entity instanceof Mob active) || !active.isAlive() || rank(active) != Rank.MYTHIC_III) continue;\n            activeInDimension++;\n            if (candidate.distanceToSqr(active) <= localRadiusSqr) return false;\n            if (activeInDimension >= MYTHIC_DIMENSION_CAP) return false;\n        }\n        return true;\n    }\n\n    private static void applyElite(Mob mob, Rank rank, Trait trait, int nearbyPlayers) {''',
'admission helper')
text = once(text,
'''        double maxDistanceSqr = MYTHIC_ALERT_RADIUS * MYTHIC_ALERT_RADIUS;''',
'''        double maxDistanceSqr = MYTHIC_TRACKER_RADIUS * MYTHIC_TRACKER_RADIUS;''',
'tracker radius')
write(path, text)

# Version identity.
path = 'gradle.properties'
text = read(path)
text = once(text, f'mod_version={OLD}', f'mod_version={NEW}', 'gradle version')
text = text.replace('# 0.61.22 fishing mastery pacing rebalance (2026-09-13).', '# 0.61.23 Mythic population and alert control (2026-09-13).')
write(path, text)

path = 'src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java'
text = read(path)
text = once(text, f'VERSION = "{OLD}"', f'VERSION = "{NEW}"', 'runtime version')
write(path, text)

# Current cumulative source audit.
path = 'tools/test_current_source.py'
text = read(path)
text = text.replace(f'mod_version={OLD}', f'mod_version={NEW}')
text = text.replace(f'VERSION = "{OLD}"', f'VERSION = "{NEW}"')
needle = 'warband = text(JAVA / "elite/WarbandDirector.java")\n'
insert = '''elite_system = text(JAVA / "elite/EliteMobSystem.java")\nfor invariant in (\n    "MYTHIC_ALERT_RADIUS = 128.0D",\n    "MYTHIC_TRACKER_RADIUS = 192.0D",\n    "MYTHIC_LOCAL_CAP_RADIUS = 256.0D",\n    "MYTHIC_DIMENSION_CAP = 3",\n    "MYTHIC_RUNTIME_INTERVAL_TICKS = 20",\n    "rank == Rank.MYTHIC_III && !canAdmitMythic(level, mob)",\n    "Math.min(0.012D, 0.0008D + power * 0.00005D + worldStage * 0.0012D)",\n    "candidate.distanceToSqr(active) <= localRadiusSqr",\n):\n    require(invariant in elite_system, f"Mythic population-control invariant missing: {invariant}")\nrequire("double maxDistanceSqr = MYTHIC_TRACKER_RADIUS * MYTHIC_TRACKER_RADIUS;" in elite_system,\n        "Mythic tracker range must stay independent from reduced spawn-alert range")\n\n'''
text = once(text, needle, insert + needle, 'current audit insertion')
write(path, text)

# Release audit version identity. Existing historical contracts remain unchanged.
path = 'tools/test_release_source.py'
text = read(path)
text = once(text, f'CURRENT_VERSION = "{OLD}"', f'CURRENT_VERSION = "{NEW}"', 'release current version')
text = once(text, f'PREVIOUS_DOC_VERSION = "{OLD}"', f'PREVIOUS_DOC_VERSION = "{NEW}"', 'release project identity')
write(path, text)

# Docs: explain why this is progression-driven rather than calendar-day-driven.
project_section = '''## 0.61.23 Mythic Population Control / 신화 개체 밀도 제어\n- Mythic III spawn pressure is not tied directly to Minecraft day count; the previous probability rose with nearby players' average mastery and World Ascension stage, which made late saves look as if each passing day increased Mythic frequency.\n- Mythic rank probability is reduced from the old `min(8%, 0.4% + power*0.035% + stage*1.2%)` conditional roll to `min(1.2%, 0.08% + power*0.005% + stage*0.12%)`. Elite admission itself is unchanged.\n- At most one loaded Mythic may be admitted within 256 blocks, and at most three loaded Mythics may be active in one dimension. A Mythic roll that hits either cap is downgraded to Ascended II instead of creating another persistent boss.\n- Spawn alerts are reduced from 192 to 128 blocks while the directional tracker keeps its existing 192-block acquisition range. This cuts chat spam without making an already-nearby Mythic harder to track.\n- Mythic runtime/bossbar maintenance moves from every 10 ticks to every 20 ticks. Phase effects still last 30 ticks, so combat behavior remains continuous while idle server work is roughly halved.\n- Existing Mythic entities are not deleted or silently demoted. The caps prevent new accumulation around them; old loaded Mythics remain valid fights/rewards until killed or unloaded.\n- No SavedData, packet, reward, boss stats, phase rules or network protocol changes.\n\n'''
path = 'PROJECT.md'
text = read(path)
text = once(text, f'- Mod version: `{OLD}`', f'- Mod version: `{NEW}`', 'PROJECT version')
old_compat = '- Existing-world compatibility: 0.61.22 changes only Fishing mastery XP pacing and does not add a SavedData ID/codec field or bump the network protocol. Existing skill XP, deterministic fishing meters, infrastructure/logistics/outpost/production data and equipment CustomData remain compatible. Bulk Mining queues and queued-tool profiles are runtime-only. Network protocol remains 15.'
new_compat = '- Existing-world compatibility: 0.61.23 changes only Mythic spawn admission/chance, alert radius and runtime cadence; it adds no SavedData ID/codec field and does not bump the network protocol. Existing Mythic entities, skill XP, fishing meters, infrastructure/logistics/outpost/production data and equipment CustomData remain compatible. Network protocol remains 15.'
text = once(text, old_compat, new_compat, 'PROJECT compatibility')
text = once(text, '## 0.61.22 Fishing Mastery Pacing / 낚시 숙련 속도 재조정\n', project_section + '## 0.61.22 Fishing Mastery Pacing / 낚시 숙련 속도 재조정\n', 'PROJECT section')
write(path, text)

readme_section = '''## 0.61.23-alpha.1 — Mythic Population Control / 신화 개체 밀도 제어\n신화 III 출현은 날짜 자체에 비례하지 않았다. 기존 확률이 플레이어 평균 숙련과 월드 승천 단계에 따라 함께 상승했고, 신화 III는 영구 유지되기 때문에 진행도가 높은 세이브에서 개체와 알림이 누적되는 구조였다.\n\n신화 등급 추첨 확률을 크게 낮추고, 같은 차원에서 **256블록 이내 신화 III 1체**, 로드된 차원 전체 **최대 3체**까지만 새 신화를 허용한다. 제한에 걸린 신화 추첨은 승천 II로 내려가므로 일반 몹 스폰 자체를 취소하지 않는다. 출현 알림은 192→128블록으로 줄이되 방향 추적은 192블록 그대로 유지한다. 신화 런타임 갱신도 0.5초마다에서 1초마다로 줄여 다수 개체가 로드됐을 때의 서버 작업을 낮췄다.\n\n기존 월드에 이미 존재하는 신화 III는 삭제하거나 강제로 약화시키지 않는다. 그 개체들이 로드돼 있는 동안에는 새 신화가 지역/차원 제한에 막히므로 추가 누적이 멈춘다. 저장 데이터·보상·보스 능력치·3단계 전투·네트워크 프로토콜은 그대로다.\n\n'''
path = 'README.md'
text = read(path)
text = once(text, '## 0.61.22-alpha.1 — Fishing Mastery Pacing / 낚시 숙련 속도 재조정\n', readme_section + '## 0.61.22-alpha.1 — Fishing Mastery Pacing / 낚시 숙련 속도 재조정\n', 'README section')
write(path, text)

change = '''## 0.61.23-alpha.1\n- Mythic III frequency is no longer allowed to scale into late-save alert spam: the conditional Mythic rank roll now caps at 1.2% and uses a much smaller mastery/world-stage slope.\n- New Mythic admission is capped to one active loaded Mythic within 256 blocks and three per loaded dimension. Overflow Mythic rolls become Ascended II rather than canceling normal hostile spawns.\n- Spawn alerts now reach 128 blocks while the directional Mythic tracker retains 192-block targeting.\n- Mythic runtime/bossbar maintenance cadence is reduced from 10 to 20 ticks; phase effects remain continuous. Existing Mythics are preserved and block further local accumulation.\n- No SavedData, reward, boss-stat, packet or protocol change. Protocol remains 15.\n\n'''
path = 'CHANGELOG.md'
text = read(path)
text = once(text, '# Changelog\n\n', '# Changelog\n\n' + change, 'CHANGELOG section')
write(path, text)

print('Survival Ascension 0.61.23 Mythic population-control patch applied.')
