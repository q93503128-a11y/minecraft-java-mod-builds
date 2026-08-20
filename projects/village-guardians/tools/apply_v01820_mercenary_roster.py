#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")


def replace_once(path: Path, old: str, new: str) -> None:
    text = read(path)
    if old not in text:
        raise SystemExit(f"missing anchor in {path}: {old[:160]!r}")
    write(path, text.replace(old, new, 1))


# Version.
props = ROOT / "gradle.properties"
replace_once(props, "mod_version=0.18.19-alpha.1", "mod_version=0.18.20-alpha.1")

merc = JAVA / "VillageMercenarySystem.java"
replace_once(merc,
'''public final class VillageMercenarySystem {
    public static final int MAX_LEVEL = 60;
''',
'''public final class VillageMercenarySystem {
    public static final int MAX_LEVEL = 60;
    private static final String LEGACY_MERCENARY_NAME = "마을 용병";
''')

replace_once(merc,
'''    public static synchronized boolean recognize(Mob mob) {
        if (!(mob instanceof IronGolem) || !CLASSES.containsKey(mob.getUUID())) return false;
        mob.setPersistenceRequired();
        VillageWorldSystem.markAllowedGameMob(mob);
        refreshName(mob);
        return true;
    }

''',
'''    public static synchronized boolean recognize(Mob mob) {
        if (!(mob instanceof IronGolem) || !CLASSES.containsKey(mob.getUUID())) return false;
        mob.setPersistenceRequired();
        VillageWorldSystem.markAllowedGameMob(mob);
        refreshName(mob);
        return true;
    }

    /** One-time migration for generic pre-class mercenaries created by the retired barracks path. */
    public static synchronized boolean adoptLegacy(Mob mob) {
        if (!(mob instanceof IronGolem)) return false;
        if (CLASSES.containsKey(mob.getUUID())) return recognize(mob);
        Component name = mob.getCustomName();
        if (name == null || !LEGACY_MERCENARY_NAME.equals(name.getString())) return false;
        UUID uuid = mob.getUUID();
        MercenaryClass kind = MercenaryClass.BASTION;
        CLASSES.put(uuid, kind);
        LEVELS.put(uuid, 1);
        KILLS.put(uuid, 0);
        mob.setPersistenceRequired();
        VillageWorldSystem.markAllowedGameMob(mob);
        applyClassPassives(mob, kind, 1);
        refreshName(mob);
        persist();
        return true;
    }

''')

replace_once(merc,
'''    public static int hireCost(MercenaryClass kind) {
        if (kind == null) return 0;
        return 150 + kind.ordinal() * 35 + VillageProgressionSystem.barracksLevel() * 25;
    }
''',
'''    public static int hireCost(MercenaryClass kind) {
        if (kind == null) return 0;
        int barracks = Math.max(0, VillageProgressionSystem.barracksLevel());
        int base = 150 + kind.ordinal() * 35;
        int discount = Math.max(0, barracks - 1) * 10;
        int floor = 110 + kind.ordinal() * 30;
        return Math.max(floor, base - discount);
    }
''')

replace_once(merc,
'''        int cap = capacity();
        int current = count(level);
''',
'''        int cap = capacity();
        int current = rosterCount();
''')

old_status = '''    public static String status(MinecraftServer server) {
        if (server == null) return "용병 상태를 확인할 수 없습니다.";
        ServerLevel level = server.overworld();
        return "용병 " + count(level) + " / " + capacity()
                + " · 용병 교리 Lv."
                + VillageDefenseResearchSystem.level(VillageDefenseResearchSystem.Branch.MERCENARY)
                + " · 적 처치 경험으로 최대 Lv." + MAX_LEVEL + "까지 장기 성장";
    }

    public static int capacity() {
        return 1 + VillageProgressionSystem.barracksLevel() / 2
                + VillageDefenseResearchSystem.mercenaryCapacityBonus();
    }

    private static synchronized int count(ServerLevel level) {
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) return 0;
        AABB area = new AABB(center).inflate(VillageWorldSystem.BATTLEFIELD_RADIUS, 96,
                VillageWorldSystem.BATTLEFIELD_RADIUS);
        return level.getEntitiesOfClass(IronGolem.class, area,
                entity -> isMercenary(entity.getUUID())).size();
    }

'''
new_status = '''    public static String status(MinecraftServer server) {
        if (server == null) return "용병 상태를 확인할 수 없습니다.";
        ServerLevel level = server.overworld();
        return "용병 명부 " + rosterCount() + " / " + capacity()
                + " · 현재 로드 " + loadedCount(level)
                + " · 용병 교리 Lv."
                + VillageDefenseResearchSystem.level(VillageDefenseResearchSystem.Branch.MERCENARY)
                + " · 적 처치 경험으로 최대 Lv." + MAX_LEVEL + "까지 장기 성장";
    }

    public static int capacity() {
        return 1 + VillageProgressionSystem.barracksLevel() / 2
                + VillageDefenseResearchSystem.mercenaryCapacityBonus();
    }

    /** Authoritative saved roster size. Hiring capacity must never depend on current chunk/AABB loading. */
    public static synchronized int rosterCount() {
        return CLASSES.size();
    }

    public static synchronized int loadedCount(ServerLevel level) {
        if (level == null) return 0;
        int count = 0;
        for (UUID uuid : CLASSES.keySet()) {
            var entity = level.getEntity(uuid);
            if (entity instanceof IronGolem golem && golem.isAlive()) count++;
        }
        return count;
    }

    public static synchronized List<RosterEntry> rosterEntries(MinecraftServer server) {
        if (server == null) return List.of();
        ServerLevel level = server.overworld();
        List<RosterEntry> result = new ArrayList<>();
        CLASSES.forEach((uuid, kind) -> {
            var entity = level.getEntity(uuid);
            boolean loaded = entity instanceof IronGolem golem && golem.isAlive();
            result.add(new RosterEntry(uuid, kind, LEVELS.getOrDefault(uuid, 1),
                    KILLS.getOrDefault(uuid, 0), loaded));
        });
        return List.copyOf(result);
    }

    public static synchronized String retire(ServerPlayer player, UUID uuid) {
        if (player == null || uuid == null) return "퇴역할 용병을 찾을 수 없습니다.";
        if (VillageRaidSystem.isRaidLocked() || VillageCouncilState.currentPhase() != VillageTimePhase.DAY) {
            return "용병 퇴역은 낮 정비 시간에만 가능합니다.";
        }
        MercenaryClass kind = CLASSES.get(uuid);
        if (kind == null) return "이미 명부에서 제외된 용병입니다.";
        if (!(player.level() instanceof ServerLevel level)) return "현재 월드에서는 용병을 퇴역시킬 수 없습니다.";
        var entity = level.getEntity(uuid);
        if (!(entity instanceof IronGolem mercenary) || !mercenary.isAlive()) {
            return "해당 용병이 현재 로드되지 않았습니다. 용병이 있는 구역을 불러온 뒤 다시 시도하세요.";
        }
        int rank = LEVELS.getOrDefault(uuid, 1);
        VillageMercenaryPresentationSystem.remove(level, uuid);
        mercenary.discard();
        VillageWorldSystem.unmarkAllowedGameMob(uuid);
        unregister(uuid);
        return kind.displayName() + " Lv." + rank + " 퇴역 완료 · 고용비는 환불되지 않습니다.";
    }

'''
replace_once(merc, old_status, new_status)

# Add public roster DTO immediately before the class enum.
replace_once(merc,
'''    public enum MercenaryClass {
''',
'''    public record RosterEntry(UUID uuid, MercenaryClass kind, int level, int kills, boolean loaded) {}

    public enum MercenaryClass {
''')

# Legacy facade can no longer spawn a second, unclassed mercenary population.
defense = JAVA / "VillageDefenseSystem.java"
text = read(defense)
old_recognize = '''    public static boolean recognizeDefenseMob(Mob mob) {
        Component name = mob.getCustomName();
        if (!(mob instanceof IronGolem) || name == null || !MERCENARY_NAME.equals(name.getString())) return false;
        VillageWorldSystem.markAllowedGameMob(mob);
        mob.setPersistenceRequired();
        return true;
    }
'''
if old_recognize not in text:
    raise SystemExit("legacy recognize anchor missing")
text = text.replace(old_recognize,
'''    public static boolean recognizeDefenseMob(Mob mob) {
        return VillageMercenarySystem.adoptLegacy(mob);
    }
''', 1)
start = text.index('    public static int mercenaryHireCost() {')
end = text.index('    public static String status(ServerLevel level) {', start)
text = text[:start] + '''    /** Compatibility facade: production hiring is owned by VillageMercenarySystem. */
    public static int mercenaryHireCost() {
        return VillageMercenarySystem.hireCost(VillageMercenarySystem.MercenaryClass.BASTION);
    }

    public static String hireMercenary(ServerPlayer player) {
        return VillageMercenarySystem.hire(player, VillageMercenarySystem.MercenaryClass.BASTION);
    }

''' + text[end:]
old_return = '        return towers + " | 용병 " + countMercenaries(level) + " / " + mercenaryCapacity(level.getServer());\n'
if old_return not in text:
    raise SystemExit("legacy status anchor missing")
text = text.replace(old_return,
                    '        return towers + " | " + VillageMercenarySystem.status(level.getServer());\n', 1)
write(defense, text)

# Dedicated scrollable barracks roster UI.
ui = JAVA / "VillageUiService.java"
insert_before_vote = '''    public static void openVoteForAll(MinecraftServer server, String proposerName) {
'''
roster_ui = '''    public static void openMercenaryRoster(ServerPlayer player) {
        if (!requireManagementAccess(player, VillageProgressionSystem.Building.BARRACKS,
                "용병 명부는 병영 또는 마을 회관에서만 관리할 수 있습니다.")) return;
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (VillageMercenarySystem.MercenaryClass kind : VillageMercenarySystem.MercenaryClass.values()) {
            int cost = VillageMercenarySystem.hireCost(kind);
            actions.add("hire_mercenary:" + kind.id());
            labels.add("고용 · " + kind.displayName() + " · " + cost + "주화|" + kind.description());
        }
        for (VillageMercenarySystem.RosterEntry entry : VillageMercenarySystem.rosterEntries(server)) {
            if (!entry.loaded()) continue;
            actions.add("retire_mercenary:" + entry.uuid());
            labels.add("퇴역 · " + entry.kind().displayName() + " Lv." + entry.level()
                    + "|누적 훈련 진척 " + entry.kills() + " · 환불 없음");
        }
        String body = VillageMercenarySystem.status(server)
                + " · 병영 Lv." + VillageProgressionSystem.barracksLevel()
                + " · 퇴역은 현재 로드된 용병만 가능";
        send(player, "mercenary_roster", "용병 명부", body, actions, labels);
    }

'''
replace_once(ui, insert_before_vote, roster_ui + insert_before_vote)

# Prefix action routing before gear purchase routing.
route_anchor = '''        if (action.startsWith("gear:")) {
'''
route_code = '''        if (action.startsWith("hire_mercenary:")) {
            if (!requireManagementAccess(player, VillageProgressionSystem.Building.BARRACKS,
                    "용병 고용은 병영 또는 마을 회관에서만 가능합니다.")) return;
            VillageMercenarySystem.MercenaryClass kind = VillageMercenarySystem.MercenaryClass.fromId(action.substring(16));
            player.sendSystemMessage(Component.literal("§b" + VillageMercenarySystem.hire(player, kind)));
            openMercenaryRoster(player);
            return;
        }
        if (action.startsWith("retire_mercenary:")) {
            if (!requireManagementAccess(player, VillageProgressionSystem.Building.BARRACKS,
                    "용병 퇴역은 병영 또는 마을 회관에서만 가능합니다.")) return;
            try {
                UUID uuid = UUID.fromString(action.substring(17));
                player.sendSystemMessage(Component.literal("§e" + VillageMercenarySystem.retire(player, uuid)));
            } catch (IllegalArgumentException ignored) {
                player.sendSystemMessage(Component.literal("§c잘못된 용병 식별자입니다."));
            }
            openMercenaryRoster(player);
            return;
        }

'''
replace_once(ui, route_anchor, route_code + route_anchor)

# UI service needs UUID import.
replace_once(ui, 'import java.util.List;\n', 'import java.util.List;\nimport java.util.UUID;\n')

# Old exact action becomes a safe roster-open compatibility route.
replace_once(ui,
'''            case "hire_mercenary" -> actAndReopen(player, () -> VillageDefenseSystem.hireMercenary(player), VillageProgressionSystem.Building.BARRACKS);
''',
'''            case "open_mercenary_roster", "hire_mercenary" -> openMercenaryRoster(player);
''')

# Barracks facility card now opens classed roster instead of instantly spawning legacy mercenary.
replace_once(ui,
'''            case BARRACKS -> add(actions, labels,
                    "train", "전투 훈련 · XP " + (30 + VillageProgressionSystem.barracksLevel() * 18) + "|3분 재사용 대기시간",
                    "hire_mercenary", "용병 고용 · 주화 " + VillageDefenseSystem.mercenaryHireCost()
                            + "|사망 전까지 저장·재접속 후에도 유지");
''',
'''            case BARRACKS -> add(actions, labels,
                    "train", "전투 훈련 · XP " + (30 + VillageProgressionSystem.barracksLevel() * 18) + "|3분 재사용 대기시간",
                    "open_mercenary_roster", "용병 명부 · " + VillageMercenarySystem.rosterCount() + " / "
                            + VillageMercenarySystem.capacity() + "|4병과 고용·현재 용병 확인·개별 퇴역");
''')

# Action descriptions / confirmation semantics.
desc = JAVA / "VillageActionDescriptions.java"
replace_once(desc,
'''        if (action.startsWith("funding:")) {
''',
'''        if (action.startsWith("hire_mercenary:")) {
            return label + "\\n선택한 병과의 영구 용병을 고용합니다. 병영 강화는 고용비를 올리지 않고 단계적으로 할인합니다.";
        }
        if (action.startsWith("retire_mercenary:")) {
            return label + "\\n현재 로드된 용병을 명부에서 영구 퇴역시킵니다. 고용비는 환불되지 않습니다.";
        }
        if (action.startsWith("funding:")) {
''')
replace_once(desc,
'''            case "hire_mercenary" -> label + "\\n수호 주화로 영구 용병을 고용합니다. 사망 전까지 저장과 재접속 후에도 유지됩니다.";
''',
'''            case "open_mercenary_roster", "hire_mercenary" -> "4병과 용병의 고용 비용·현재 명부·레벨을 확인하고 퇴역을 관리합니다.";
''')
replace_once(desc,
'''                || action.equals("hire_mercenary")
''',
'''                || action.startsWith("hire_mercenary:")
                || action.startsWith("retire_mercenary:")
''')
replace_once(desc,
'''        if (action.startsWith("gear:")) return "장비 구매";
''',
'''        if (action.startsWith("hire_mercenary:")) return "용병 고용";
        if (action.startsWith("retire_mercenary:")) return "용병 퇴역";
        if (action.startsWith("gear:")) return "장비 구매";
''')

# Detail screen asks for confirmation on retirement too.
detail = JAVA / "VillageActionDetailScreen.java"
replace_once(detail,
'''                || action.startsWith("hire_mercenary:")
''',
'''                || action.startsWith("hire_mercenary:")
                || action.startsWith("retire_mercenary:")
''')

# Existing current-version tests move with the release; historical semantics remain unchanged.
for path in (ROOT / "tools").glob("test_*.py"):
    text = read(path)
    if "mod_version=0.18.19-alpha.1" in text:
        write(path, text.replace("mod_version=0.18.19-alpha.1", "mod_version=0.18.20-alpha.1"))

contract = r'''#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name):
    return (JAVA / name).read_text(encoding="utf-8")


def hire_cost(kind_ordinal, barracks):
    base = 150 + kind_ordinal * 35
    discount = max(0, barracks - 1) * 10
    floor = 110 + kind_ordinal * 30
    return max(floor, base - discount)


def main():
    assert "mod_version=0.18.20-alpha.1" in (ROOT / "gradle.properties").read_text(encoding="utf-8")
    merc = read("VillageMercenarySystem.java")
    defense = read("VillageDefenseSystem.java")
    ui = read("VillageUiService.java")
    desc = read("VillageActionDescriptions.java")
    detail = read("VillageActionDetailScreen.java")

    # Barracks progression must never increase hire cost for any class.
    for ordinal in range(4):
        costs = [hire_cost(ordinal, level) for level in range(0, 6)]
        assert all(b <= a for a, b in zip(costs, costs[1:])), costs
    assert "Math.max(0, barracks - 1) * 10" in merc
    assert "+ VillageProgressionSystem.barracksLevel() * 25" not in merc

    # Saved roster, not a local AABB scan, owns capacity and status.
    assert "int current = rosterCount();" in merc
    assert "public static synchronized int rosterCount()" in merc
    assert "return CLASSES.size();" in merc
    assert '"용병 명부 " + rosterCount() + " / " + capacity()' in merc
    assert "loadedCount(level)" in merc
    assert "private static synchronized int count(ServerLevel level)" not in merc

    # Legacy generic mercenaries migrate once into the classed SavedData system.
    assert 'LEGACY_MERCENARY_NAME = "마을 용병"' in merc
    assert "public static synchronized boolean adoptLegacy(Mob mob)" in merc
    assert "MercenaryClass kind = MercenaryClass.BASTION;" in merc
    assert "return VillageMercenarySystem.adoptLegacy(mob);" in defense
    assert "VillageMercenarySystem.hire(player, VillageMercenarySystem.MercenaryClass.BASTION)" in defense
    assert "EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.EVENT)" not in defense

    # Barracks UI enters one roster owner; four classes and retirement use explicit action IDs.
    assert 'send(player, "mercenary_roster", "용병 명부"' in ui
    assert '"hire_mercenary:" + kind.id()' in ui
    assert '"retire_mercenary:" + entry.uuid()' in ui
    assert 'case "open_mercenary_roster", "hire_mercenary" -> openMercenaryRoster(player);' in ui
    assert "VillageDefenseSystem.hireMercenary(player)" not in ui
    assert '"open_mercenary_roster", "용병 명부 · "' in ui
    assert "public static synchronized String retire(ServerPlayer player, UUID uuid)" in merc
    assert "VillageMercenaryPresentationSystem.remove(level, uuid);" in merc
    assert "unregister(uuid);" in merc
    assert 'action.startsWith("retire_mercenary:")' in desc
    assert 'action.startsWith("retire_mercenary:")' in detail

    print("[PASS] barracks upgrades no longer make classed mercenaries more expensive")
    print("[PASS] authoritative SavedData roster owns mercenary capacity and status")
    print("[PASS] legacy generic mercenaries migrate into the classed progression system")
    print("[PASS] barracks roster UI exposes four hires and confirmation-gated retirement")


if __name__ == "__main__":
    main()
'''
write(ROOT / "tools/test_v01820_mercenary_roster.py", contract)

print("[PASS] v0.18.20 mercenary roster/economy patch staged")
