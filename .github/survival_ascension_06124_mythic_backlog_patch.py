from pathlib import Path

ROOT = Path('projects/survival-ascension')
OLD = '0.61.23-alpha.1'
NEW = '0.61.24-alpha.1'


def read(rel):
    return (ROOT / rel).read_text(encoding='utf-8')


def write(rel, text):
    (ROOT / rel).write_text(text, encoding='utf-8')


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly one match, got {count}')
    return text.replace(old, new, 1)

# Runtime fix: 0.61.23 only gated newly promoted Mythics. Old persistent Mythics loaded from
# existing saves bypassed admission entirely in onEntityJoin, so a late save could still restore
# a backlog of 10-20+ bosses. Enforce the same cap on load and prune any already-registered overflow.
path = 'src/main/java/kr/moonseungjun/survivalascension/elite/EliteMobSystem.java'
text = read(path)
old_join = '''    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Mob mob && event.getLevel() instanceof ServerLevel && rank(mob) == Rank.MYTHIC_III) {
            mob.setPersistenceRequired();
            mob.setGlowingTag(true);
            ensureMythicRuntime(mob);
        }
    }
'''
new_join = '''    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Mob mob && event.getLevel() instanceof ServerLevel level && rank(mob) == Rank.MYTHIC_III) {
            // 0.61.23 only capped fresh promotions. Mythics persisted by older builds can re-enter
            // through chunk loading, so apply the same population admission before registering them.
            if (!canAdmitMythic(level, mob)) {
                retireOverflowMythic(mob);
                return;
            }
            mob.setPersistenceRequired();
            mob.setGlowingTag(true);
            ensureMythicRuntime(mob);
        }
    }
'''
text = replace_once(text, old_join, new_join, 'Mythic load admission')
text = replace_once(text,
'''        if (++mythicTicker < MYTHIC_RUNTIME_INTERVAL_TICKS) return;
        mythicTicker = 0;
        List<UUID> remove = new ArrayList<>();
''',
'''        if (++mythicTicker < MYTHIC_RUNTIME_INTERVAL_TICKS) return;
        mythicTicker = 0;
        enforceMythicRuntimeCaps(event.getServer());
        List<UUID> remove = new ArrayList<>();
''',
'Mythic runtime cap sweep')
old_admit = '''    private static boolean canAdmitMythic(ServerLevel level, Mob candidate) {
        int activeInDimension = 0;
        double localRadiusSqr = MYTHIC_LOCAL_CAP_RADIUS * MYTHIC_LOCAL_CAP_RADIUS;
        for (Map.Entry<UUID, MythicRuntime> entry : MYTHICS.entrySet()) {
            MythicRuntime runtime = entry.getValue();
            if (runtime.level != level) continue;
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof Mob active) || !active.isAlive() || rank(active) != Rank.MYTHIC_III) continue;
            activeInDimension++;
            if (candidate.distanceToSqr(active) <= localRadiusSqr) return false;
            if (activeInDimension >= MYTHIC_DIMENSION_CAP) return false;
        }
        return true;
    }
'''
new_admit = '''    private static boolean canAdmitMythic(ServerLevel level, Mob candidate) {
        int activeInDimension = 0;
        double localRadiusSqr = MYTHIC_LOCAL_CAP_RADIUS * MYTHIC_LOCAL_CAP_RADIUS;
        for (Map.Entry<UUID, MythicRuntime> entry : MYTHICS.entrySet()) {
            if (entry.getKey().equals(candidate.getUUID())) continue;
            MythicRuntime runtime = entry.getValue();
            if (runtime.level != level) continue;
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof Mob active) || !active.isAlive() || rank(active) != Rank.MYTHIC_III) continue;
            activeInDimension++;
            if (candidate.distanceToSqr(active) <= localRadiusSqr) return false;
            if (activeInDimension >= MYTHIC_DIMENSION_CAP) return false;
        }
        return true;
    }

    private static void enforceMythicRuntimeCaps(net.minecraft.server.MinecraftServer server) {
        Map<ServerLevel, List<Mob>> byLevel = new HashMap<>();
        List<UUID> stale = new ArrayList<>();
        for (Map.Entry<UUID, MythicRuntime> entry : new ArrayList<>(MYTHICS.entrySet())) {
            MythicRuntime runtime = entry.getValue();
            if (runtime.level.getServer() != server) continue;
            Entity entity = runtime.level.getEntity(entry.getKey());
            if (!(entity instanceof Mob mob) || !mob.isAlive() || rank(mob) != Rank.MYTHIC_III) {
                stale.add(entry.getKey());
                continue;
            }
            byLevel.computeIfAbsent(runtime.level, ignored -> new ArrayList<>()).add(mob);
        }
        for (UUID id : stale) {
            MythicRuntime runtime = MYTHICS.remove(id);
            if (runtime != null) closeMythicBar(runtime);
        }

        double localRadiusSqr = MYTHIC_LOCAL_CAP_RADIUS * MYTHIC_LOCAL_CAP_RADIUS;
        for (Map.Entry<ServerLevel, List<Mob>> dimension : byLevel.entrySet()) {
            List<Mob> loaded = dimension.getValue();
            // Preserve bosses closest to an active player first so backlog cleanup is least likely
            // to erase the fight the player is currently engaging.
            loaded.sort((left, right) -> Double.compare(nearestPlayerDistanceSqr(dimension.getKey(), left),
                    nearestPlayerDistanceSqr(dimension.getKey(), right)));
            List<Mob> kept = new ArrayList<>();
            for (Mob mob : loaded) {
                boolean localConflict = false;
                for (Mob existing : kept) {
                    if (mob.distanceToSqr(existing) <= localRadiusSqr) {
                        localConflict = true;
                        break;
                    }
                }
                if (localConflict || kept.size() >= MYTHIC_DIMENSION_CAP) {
                    retireOverflowMythic(mob);
                } else {
                    kept.add(mob);
                }
            }
        }
    }

    private static double nearestPlayerDistanceSqr(ServerLevel level, Mob mob) {
        double nearest = Double.MAX_VALUE;
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() != level || !player.isAlive() || player.isSpectator()) continue;
            nearest = Math.min(nearest, player.distanceToSqr(mob));
        }
        return nearest;
    }

    private static void retireOverflowMythic(Mob mob) {
        MythicRuntime runtime = MYTHICS.remove(mob.getUUID());
        if (runtime != null) closeMythicBar(runtime);
        mob.discard();
    }
'''
text = replace_once(text, old_admit, new_admit, 'Mythic admission/backlog enforcement')
# Test command must not be able to bypass the production population cap either.
text = replace_once(text,
'''        applyElite(mob, Rank.MYTHIC_III, Trait.SWIFT, 1);
        syncMythicTracker(player);
''',
'''        if (!canAdmitMythic(level, mob)) {
            mob.discard();
            player.sendSystemMessage(Component.literal("§e[신화 테스트] §f주변 신화 개체 제한에 걸려 소환하지 않았습니다."));
            return 0;
        }
        applyElite(mob, Rank.MYTHIC_III, Trait.SWIFT, 1);
        syncMythicTracker(player);
''',
'Mythic test cap')
write(path, text)

# Version identity.
path = 'gradle.properties'
text = read(path)
text = replace_once(text, f'mod_version={OLD}', f'mod_version={NEW}', 'gradle version')
text = text.replace('# 0.61.23 Mythic population and alert control (2026-09-13).', '# 0.61.24 Mythic backlog enforcement (2026-09-13).')
write(path, text)

path = 'src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java'
text = read(path)
text = replace_once(text, f'public static final String VERSION = "{OLD}";', f'public static final String VERSION = "{NEW}";', 'source version')
write(path, text)

path = 'tools/test_current_source.py'
text = read(path)
text = text.replace(f'mod_version={OLD}', f'mod_version={NEW}')
text = text.replace(f'VERSION = "{OLD}"', f'VERSION = "{NEW}"')
needle = '''    "candidate.distanceToSqr(active) <= localRadiusSqr",
):
'''
replacement = '''    "candidate.distanceToSqr(active) <= localRadiusSqr",
    "if (entry.getKey().equals(candidate.getUUID())) continue;",
    "if (!canAdmitMythic(level, mob))",
    "enforceMythicRuntimeCaps(event.getServer())",
    "retireOverflowMythic(mob)",
    "loaded.sort((left, right) -> Double.compare(nearestPlayerDistanceSqr",
    "mob.discard()",
):
'''
text = replace_once(text, needle, replacement, 'current-source Mythic backlog invariants')
write(path, text)

path = 'tools/test_release_source.py'
text = read(path)
text = replace_once(text, f'CURRENT_VERSION = "{OLD}"', f'CURRENT_VERSION = "{NEW}"', 'release current version')
text = replace_once(text, f'PREVIOUS_DOC_VERSION = "{OLD}"', f'PREVIOUS_DOC_VERSION = "{NEW}"', 'release doc version')
write(path, text)

# Player/project docs: explain the bug rather than hiding the old behavior.
project_section = '''## 0.61.24 Mythic Backlog Enforcement / 신화 누적 개체 정리
- 0.61.23 correctly capped newly promoted Mythics but intentionally preserved older persistent Mythics. That left a real hole: existing saves could reload 10-20+ Mythic III mobs through `EntityJoinLevelEvent`, and those restored bosses bypassed the new admission cap.
- Loaded Mythic III entities now pass the same 256-block / three-per-dimension admission rule when they enter the world. Overflow legacy Mythics are retired without drops or kill rewards instead of being registered as more persistent bosses.
- Runtime maintenance also reconciles the registered Mythic set every second. If an old backlog or another load path produces an over-cap set, the bosses nearest active players are preserved first and overflow is removed.
- The Mythic test summon now respects the same cap, so testing cannot accidentally create a permanent backlog.
- This intentionally changes the 0.61.23 preservation policy: existing Mythics above the cap may disappear when their chunks load. It does not touch player skill XP, infrastructure, equipment, fishing data, rewards for valid Mythics, SavedData schemas or network protocol 15.

'''
path = 'PROJECT.md'
text = read(path)
text = replace_once(text, f'- Mod version: `{OLD}`', f'- Mod version: `{NEW}`', 'PROJECT version')
old_compat = '- Existing-world compatibility: 0.61.23 changes only Mythic spawn admission/chance, alert radius and runtime cadence; it adds no SavedData ID/codec field and does not bump the network protocol. Existing Mythic entities, skill XP, fishing meters, infrastructure/logistics/outpost/production data and equipment CustomData remain compatible. Network protocol remains 15.'
new_compat = '- Existing-world compatibility: 0.61.24 adds no SavedData ID/codec field and does not bump the network protocol. It intentionally retires loaded Mythic III entities that exceed the 256-block / three-per-dimension cap so old persistent backlogs cannot survive indefinitely; all player progression and valid in-cap Mythics remain compatible. Network protocol remains 15.'
text = replace_once(text, old_compat, new_compat, 'PROJECT compatibility')
text = replace_once(text, '## 0.61.23 Mythic Population Control / 신화 개체 밀도 제어\n', project_section + '## 0.61.23 Mythic Population Control / 신화 개체 밀도 제어\n', 'PROJECT section')
write(path, text)

readme_section = '''## 0.61.24-alpha.1 — Mythic Backlog Enforcement / 신화 누적 개체 정리
0.61.23은 새로 승급되는 신화 III만 제한했고, 이전 버전에서 이미 `setPersistenceRequired()`로 저장된 신화 III는 월드/청크 로딩 때 제한 검사를 받지 않았다. 그래서 오래된 세이브에서는 새 출현률을 낮춰도 과거에 쌓인 신화가 한꺼번에 다시 로드되어 10~20체 이상 보일 수 있었다.

이제 신화 III는 새 출현뿐 아니라 **월드에 다시 들어오는 순간에도 256블록 내 1체 / 차원 내 3체 제한을 적용**한다. 제한을 넘는 기존 누적 개체는 보상이나 처치 판정 없이 정리된다. 1초 주기의 런타임 정합 검사도 추가해 우회 경로로 초과 개체가 등록되더라도 플레이어에게 가까운 신화를 우선 남기고 나머지를 제거한다. 테스트 소환도 같은 제한을 따른다.

따라서 0.61.24 JAR로 월드를 다시 열면 로드되는 과거 신화 누적분이 자동으로 정리된다. 정상 범위 안의 신화 전투/보상, 숙련·장비·인프라·낚시 데이터와 네트워크 프로토콜 15는 그대로다.

'''
path = 'README.md'
text = read(path)
text = replace_once(text, '## 0.61.23-alpha.1 — Mythic Population Control / 신화 개체 밀도 제어\n', readme_section + '## 0.61.23-alpha.1 — Mythic Population Control / 신화 개체 밀도 제어\n', 'README section')
write(path, text)

change = '''## 0.61.24-alpha.1
- Fixed the 0.61.23 Mythic-cap hole for existing saves: persistent Mythic III mobs reloaded through EntityJoin now pass the same 256-block / three-per-dimension admission rule instead of bypassing it.
- Loaded over-cap legacy Mythics are retired without drops/rewards; a one-second runtime reconciliation preserves Mythics nearest active players first and removes overflow.
- Mythic test summons now respect the production population cap, preventing accidental permanent test backlogs.
- No SavedData schema, normal Mythic reward/stat/phase, player progression, packet or protocol change. Protocol remains 15.

'''
path = 'CHANGELOG.md'
text = read(path)
text = replace_once(text, '# Changelog\n\n', '# Changelog\n\n' + change, 'CHANGELOG section')
write(path, text)

print('Survival Ascension 0.61.24 Mythic backlog patch applied.')
