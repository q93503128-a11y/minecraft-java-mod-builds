from pathlib import Path
import re

REPO = Path('.')
SA = REPO / 'projects/survival-ascension'
FR = REPO / 'projects/frontier-settlement'


def read(path):
    return path.read_text(encoding='utf-8')


def write(path, text):
    path.write_text(text, encoding='utf-8')


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected 1 match, got {count}')
    return text.replace(old, new, 1)

# ---------------- Survival Ascension 0.61.10 ----------------
p = SA / 'gradle.properties'
s = read(p)
s = replace_once(s, 'mod_version=0.61.9-alpha.1', 'mod_version=0.61.10-alpha.1', 'sa gradle version')
write(p, s)

p = SA / 'src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java'
s = read(p)
s = replace_once(s, 'public static final String VERSION = "0.61.9-alpha.1";',
                 'public static final String VERSION = "0.61.10-alpha.1";', 'sa source version')
write(p, s)

p = SA / 'src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java'
s = read(p)
old = '''        switch (skill) {
            case MINING -> { early = 1.25D; late = 1.10D; }
            case WOODCUTTING -> { early = 1.60D; late = 1.25D; }
            case HARVESTING -> { early = 1.50D; late = 1.20D; }
            case FISHING -> { early = 3.00D; late = 2.50D; }
            case COMBAT -> { early = 1.25D; late = 1.15D; }
            case CONSTRUCTION -> { early = 2.75D; late = 1.75D; }
            case MOBILITY -> { early = 2.10D; late = 1.40D; }
            default -> { early = 1.0D; late = 1.0D; }
        }'''
new = '''        switch (skill) {
            // Mining already earns XP from very high real block counts and keeps its proven pacing.
            case MINING -> { early = 1.25D; late = 1.10D; }
            // Action-scarce skills are normalized against actual survival play time. Lv90 is a major
            // infrastructure/mastery threshold, so these must not require thousands of repetitive actions.
            case WOODCUTTING -> { early = 2.50D; late = 2.00D; }
            case HARVESTING -> { early = 3.00D; late = 2.50D; }
            case FISHING -> { early = 6.00D; late = 5.00D; }
            case COMBAT -> { early = 4.00D; late = 3.50D; }
            case CONSTRUCTION -> { early = 5.00D; late = 3.50D; }
            case MOBILITY -> { early = 4.00D; late = 3.00D; }
            default -> { early = 1.0D; late = 1.0D; }
        }'''
s = replace_once(s, old, new, 'skill multipliers')
write(p, s)

p = SA / 'src/main/java/kr/moonseungjun/survivalascension/mobility/MobilityProgression.java'
s = read(p)
s = replace_once(s,
'''    private static final Map<UUID, Long> DASH_READY_TICK = new HashMap<>();
    private static final Map<UUID, Integer> AIR_DASH_COUNT = new HashMap<>();''',
'''    private static final Map<UUID, Long> DASH_READY_TICK = new HashMap<>();
    private static final Map<UUID, Integer> AIR_DASH_COUNT = new HashMap<>();
    private static final Map<UUID, Integer> APPLIED_ATTRIBUTE_LEVEL = new HashMap<>();''', 'mobility cache field')
s = replace_once(s,
'''    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        int level = SkillProgressData.get(player).level(player, SkillType.MOBILITY);
        applyAttributes(player, level);
        UUID uuid = player.getUUID();
        if (player.onGround()) AIR_DASH_COUNT.put(uuid, 0);
        trackTraversal(player);
        syncDashCooldown(player);
    }''',
'''    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID uuid = player.getUUID();
        // Attribute state changes only when the mobility level changes. The old path re-read player
        // progression and rewrote three transient modifiers every server tick for every player.
        if (player.tickCount % 10 == 0 || !APPLIED_ATTRIBUTE_LEVEL.containsKey(uuid)) {
            refreshAttributesIfNeeded(player);
        }
        if (player.onGround() && AIR_DASH_COUNT.getOrDefault(uuid, 0) != 0) AIR_DASH_COUNT.put(uuid, 0);
        trackTraversal(player);
        syncDashCooldown(player);
    }''', 'mobility tick cache')
s = replace_once(s,
'''    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // A dedicated-server relog must not reset a live dash cooldown. Client state can survive
        // world switches, so always send the authoritative server value on join.
        if (DASH_READY_TICK.containsKey(player.getUUID())) syncDashCooldown(player);
        else SkillNetwork.sendMobilityCooldown(player, new MobilityCooldownPayload(0));
    }''',
'''    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        APPLIED_ATTRIBUTE_LEVEL.remove(player.getUUID());
        refreshAttributesIfNeeded(player);
        // A dedicated-server relog must not reset a live dash cooldown. Client state can survive
        // world switches, so always send the authoritative server value on join.
        if (DASH_READY_TICK.containsKey(player.getUUID())) syncDashCooldown(player);
        else SkillNetwork.sendMobilityCooldown(player, new MobilityCooldownPayload(0));
    }''', 'mobility login cache')
s = replace_once(s,
'''        // Keep those two until landing or actual server shutdown so relogging cannot refresh a dash.
        TRAVERSAL.remove(uuid);''',
'''        // Keep those two until landing or actual server shutdown so relogging cannot refresh a dash.
        TRAVERSAL.remove(uuid);
        APPLIED_ATTRIBUTE_LEVEL.remove(uuid);''', 'mobility logout cache')
s = replace_once(s,
'''        TRAVERSAL.clear();
        DASH_READY_TICK.clear();
        AIR_DASH_COUNT.clear();''',
'''        TRAVERSAL.clear();
        DASH_READY_TICK.clear();
        AIR_DASH_COUNT.clear();
        APPLIED_ATTRIBUTE_LEVEL.clear();''', 'mobility shutdown cache')
marker = '    private static void applyAttributes(ServerPlayer player, int level) {'
if marker not in s:
    raise SystemExit('mobility applyAttributes marker missing')
helper = '''    private static void refreshAttributesIfNeeded(ServerPlayer player) {
        UUID uuid = player.getUUID();
        int level = SkillProgressData.get(player).level(player, SkillType.MOBILITY);
        Integer applied = APPLIED_ATTRIBUTE_LEVEL.get(uuid);
        if (applied != null && applied == level) return;
        applyAttributes(player, level);
        APPLIED_ATTRIBUTE_LEVEL.put(uuid, level);
    }

'''
s = s.replace(marker, helper + marker, 1)
write(p, s)

p = SA / 'src/main/java/kr/moonseungjun/survivalascension/elite/WarbandDirector.java'
s = read(p)
s = replace_once(s, 'private static final int BEHAVIOR_INTERVAL = 10;',
                 'private static final int BEHAVIOR_INTERVAL = 20;', 'warband cadence')
write(p, s)

# Refresh the stale version/curve portion of the existing source checker instead of creating a second authority.
p = SA / 'tools/test_current_source.py'
s = read(p)
s = s.replace('mod_version=0.48.0-alpha.1', 'mod_version=0.61.10-alpha.1')
s = s.replace('VERSION = "0.48.0-alpha.1"', 'VERSION = "0.61.10-alpha.1"')
pattern = re.compile(r'tuning = read\("src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning\.java"\)\nneed\(tuning, \[.*?\], "early mastery curve"\)\n', re.S)
replacement = '''tuning = read("src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java")
need(tuning, [
    "if (level < 20)", "if (level < 30) return 430L", "if (level < 60)", "if (level < 90)",
    "case MINING -> { early = 1.25D; late = 1.10D; }",
    "case WOODCUTTING -> { early = 2.50D; late = 2.00D; }",
    "case HARVESTING -> { early = 3.00D; late = 2.50D; }",
    "case FISHING -> { early = 6.00D; late = 5.00D; }",
    "case COMBAT -> { early = 4.00D; late = 3.50D; }",
    "case CONSTRUCTION -> { early = 5.00D; late = 3.50D; }",
    "case MOBILITY -> { early = 4.00D; late = 3.00D; }"
], "0.61.10 mastery pacing")
mobility = read("src/main/java/kr/moonseungjun/survivalascension/mobility/MobilityProgression.java")
need(mobility, ["APPLIED_ATTRIBUTE_LEVEL", "player.tickCount % 10 == 0", "refreshAttributesIfNeeded"], "0.61.10 mobility tick cache")
warband = read("src/main/java/kr/moonseungjun/survivalascension/elite/WarbandDirector.java")
need(warband, ["BEHAVIOR_INTERVAL = 20"], "0.61.10 warband scan cadence")
'''
s, n = pattern.subn(replacement, s, count=1)
if n != 1:
    raise SystemExit(f'sa checker tuning block replacement count={n}')
write(p, s)

# ---------------- Frontier Alpha.105 ----------------
p = FR / 'gradle.properties'
s = read(p)
s = replace_once(s, 'mod_version=0.1.0-alpha.104', 'mod_version=0.1.0-alpha.105', 'frontier version')
s += '\n# Alpha.105 runtime optimization: maintenance-only duplicate sweeps move to 10-second cadence, idle builder routing to 1-second cadence, and active builder discovery avoids a redundant entity scan.\n'
write(p, s)

p = FR / 'COMPANION_LOCK.json'
s = read(p)
s = replace_once(s, '"frontier_settlement": "0.1.0-alpha.104"',
                 '"frontier_settlement": "0.1.0-alpha.105"', 'frontier lock')
write(p, s)

p = FR / 'src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementWorkerService.java'
s = read(p)
s = replace_once(s, '    private static final int PRODUCTION_HAUL_STACK = 64;',
'''    private static final int PRODUCTION_HAUL_STACK = 64;
    // Duplicate/migration scans are recovery maintenance, not production AI. Running their broad
    // entity/evidence queries every 10 ticks wasted time in healthy saves; 200 still divides the
    // 600-tick recruitment boundary so duplicate authority is normalized before any new arrival.
    private static final int DUPLICATE_MAINTENANCE_INTERVAL_TICKS = 200;''', 'frontier maintenance constant')
s = replace_once(s, '        if (server.getTickCount() % 10 == 0) {',
                 '        if (server.getTickCount() % DUPLICATE_MAINTENANCE_INTERVAL_TICKS == 0) {', 'frontier maintenance cadence')
write(p, s)

p = FR / 'src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementService.java'
s = read(p)
s = replace_once(s, 'if (tick % 10 == 0) SettlementConstructionService.settleIdleBuilders(server, data);',
                 'if (tick % 20 == 0) SettlementConstructionService.settleIdleBuilders(server, data);', 'idle builder cadence')
write(p, s)

p = FR / 'src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementConstructionService.java'
s = read(p)
old = '''    public static List<FrontierWorkerEntity> ensureProjectBuilders(ServerLevel level, SettlementData data) {
        reconcileBuilderDuplicates(level, data);
        List<FrontierWorkerEntity> existing = new ArrayList<>(findBuilders(level, data));
        int desired = desiredBuilderCount(data);'''
new = '''    public static List<FrontierWorkerEntity> ensureProjectBuilders(ServerLevel level, SettlementData data) {
        // One authoritative query per active-project tick. The old path queried the same builder
        // envelope once in reconcileBuilderDuplicates() and immediately again in findBuilders().
        List<FrontierWorkerEntity> existing = new ArrayList<>(findBuilders(level, data));
        int desired = desiredBuilderCount(data);
        if (existing.size() > desired) {
            for (int i = desired; i < existing.size(); i++) removeDuplicateBuilderPreservingCargo(level, existing.get(i));
            existing = new ArrayList<>(existing.subList(0, desired));
        }'''
s = replace_once(s, old, new, 'single builder query')
write(p, s)

p = FR / 'tools/test_current_source.py'
s = read(p)
s = s.replace('mod_version=0.1.0-alpha.104', 'mod_version=0.1.0-alpha.105')
s = s.replace('CURRENT SOURCE CHECK PASS: alpha104 full flatten + serialized builder crew + prior authority invariants',
              'CURRENT SOURCE CHECK PASS: alpha105 runtime optimization + alpha104 full flatten + prior authority invariants')
anchor = 'require("isBlockedOutsideWorkReach" in worker, "blocked-target retry still suppresses already-reachable remote work")\n'
if anchor not in s:
    raise SystemExit('frontier checker worker anchor missing')
s = s.replace(anchor, anchor + 'require("DUPLICATE_MAINTENANCE_INTERVAL_TICKS = 200" in worker, "maintenance duplicate scans regressed to hot-path cadence")\n', 1)
anchor = 'require("8-tick grading gate" not in service, "obsolete construction cadence prose remains")\n'
if anchor not in s:
    raise SystemExit('frontier checker service anchor missing')
s = s.replace(anchor, anchor + 'require("tick % 20 == 0) SettlementConstructionService.settleIdleBuilders" in service, "idle builder path maintenance cadence regressed")\nrequire("List<FrontierWorkerEntity> existing = new ArrayList<>(findBuilders(level, data));" in construction, "active builder discovery path missing")\n', 1)
write(p, s)

print('Combined performance + pacing patch applied')
