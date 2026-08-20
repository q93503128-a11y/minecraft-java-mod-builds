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


research = JAVA / "VillageDefenseResearchSystem.java"
replace_once(research,
'''import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
''',
'''import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
''')
replace_once(research,
'''        String before = branch.description(current);
        LEVELS.put(branch, current + 1);
        persist();
        String after = branch.description(current + 1);
''',
'''        String before = branch.description(current);
        float previousTowerDurability = branch == Branch.TOWER ? towerDurabilityMultiplier() : 1.0f;
        LEVELS.put(branch, current + 1);
        persist();
        if (branch == Branch.TOWER && player.level() instanceof ServerLevel level) {
            VillagePlacedTurretSystem.applyResearchDurabilityUpgrade(level, previousTowerDurability);
        }
        String after = branch.description(current + 1);
''')
replace_once(research,
'''    public static int mercenaryCapacityBonus() {
        return Math.min(5, (level(Branch.MERCENARY) + 1) / 2);
    }
''',
'''    private static int mercenaryCapacityAt(int researchLevel) {
        int safe = Math.max(0, Math.min(MAX_LEVEL, researchLevel));
        int foundation = Math.min(3, safe);
        int mastery = Math.max(0, safe - 4) / 2;
        return Math.min(5, foundation + mastery);
    }

    public static int mercenaryCapacityBonus() {
        return mercenaryCapacityAt(level(Branch.MERCENARY));
    }
''')
replace_once(research,
'''                case MERCENARY -> "정원 +" + Math.min(5, (safe + 1) / 2)
''',
'''                case MERCENARY -> "정원 +" + mercenaryCapacityAt(safe)
''')

merc = JAVA / "VillageMercenarySystem.java"
replace_once(merc,
'''        savedData = server.overworld().getDataStorage().computeIfAbsent(VillageMercenaryData.TYPE);
        snapshotData = server.overworld().getDataStorage().computeIfAbsent(VillageMercenarySnapshotData.TYPE);
        CLASSES.clear();
''',
'''        savedData = server.overworld().getDataStorage().computeIfAbsent(VillageMercenaryData.TYPE);
        snapshotData = server.overworld().getDataStorage().computeIfAbsent(VillageMercenarySnapshotData.TYPE);
        VillageMercenaryPresentationSystem.reset();
        CLASSES.clear();
''')
replace_once(merc,
'''    public static void reset() {
        tickCounter = 0;
        VillageMercenaryPresentationSystem.reset();
    }
''',
'''    public static void reset() {
        tickCounter = 0;
    }
''')

turret = JAVA / "VillagePlacedTurretSystem.java"
replace_once(turret,
'''    private static final String PREFIX = "turret_";
''',
'''    private static final String PREFIX = "turret_";
    private static final String RESEARCH_DURABILITY_MIGRATION = "v01819_turret_durability_migrated";
''')
# Both initializeServer and reloadAfterPersistenceChange load the same persisted turret list.
needle = '''        VillageSiegePersistence.stringsWithPrefix(PREFIX).forEach((key, value) -> {
            try {
                int id = Integer.parseInt(key.substring(PREFIX.length()));
                TurretState state = decode(id, value);
                if (state != null) TURRETS.put(id, state);
            } catch (NumberFormatException ignored) { }
        });
'''
replacement = needle + '''        migrateLegacyResearchDurability();
'''
text = read(turret)
if text.count(needle) != 2:
    raise SystemExit(f"expected two turret-load anchors, found {text.count(needle)}")
write(turret, text.replace(needle, replacement))
replace_once(turret,
'''    private static int maxHp(TurretState state) {
        int base = state.type().baseHp() + (state.level() - 1) * 70;
        return Math.max(base, Math.round(base * VillageDefenseResearchSystem.towerDurabilityMultiplier()));
    }
''',
'''    private static int legacyBaseMaxHp(TurretState state) {
        return state.type().baseHp() + (state.level() - 1) * 70;
    }

    private static int maxHp(TurretState state) {
        int base = legacyBaseMaxHp(state);
        return Math.max(base, Math.round(base * VillageDefenseResearchSystem.towerDurabilityMultiplier()));
    }

    /** One-time v0.18.18 save migration: turrets that were full before research durability remain full. */
    private static synchronized void migrateLegacyResearchDurability() {
        if (VillageSiegePersistence.getInt(RESEARCH_DURABILITY_MIGRATION, 0) != 0) return;
        for (TurretState state : new ArrayList<>(TURRETS.values())) {
            int legacyMax = legacyBaseMaxHp(state);
            int researchedMax = maxHp(state);
            int migratedHp = state.hp();
            if (state.active() && state.hp() >= legacyMax) migratedHp = researchedMax;
            else migratedHp = Math.min(researchedMax, Math.max(0, state.hp()));
            if (migratedHp != state.hp()) {
                TurretState migrated = new TurretState(state.id(), state.type(), state.pos(),
                        state.level(), migratedHp, state.active() && migratedHp > 0);
                TURRETS.put(state.id(), migrated);
                persist(migrated);
            }
        }
        VillageSiegePersistence.putInt(RESEARCH_DURABILITY_MIGRATION, 1);
    }

    /** Preserve each active turret's health ratio when tower-durability research increases. */
    public static synchronized void applyResearchDurabilityUpgrade(ServerLevel level, float previousMultiplier) {
        if (level == null) return;
        float oldMultiplier = Math.max(1.0f, previousMultiplier);
        for (TurretState state : new ArrayList<>(TURRETS.values())) {
            if (!state.active() || state.hp() <= 0) continue;
            int base = legacyBaseMaxHp(state);
            int oldMax = Math.max(base, Math.round(base * oldMultiplier));
            int newMax = maxHp(state);
            if (newMax <= oldMax) continue;
            float healthRatio = Math.max(0.0f, Math.min(1.0f, state.hp() / (float) oldMax));
            int newHp = Math.max(1, Math.min(newMax, Math.round(newMax * healthRatio)));
            TurretState updated = new TurretState(state.id(), state.type(), state.pos(),
                    state.level(), newHp, true);
            TURRETS.put(state.id(), updated);
            persist(updated);
            buildVisual(level, updated);
        }
    }
''')

contract = ROOT / "tools/test_v01819_research_mercenary_presentation.py"
text = read(contract)
text = text.replace(
'''    assert (5 + 1) // 2 == 3 and min(5, (10 + 1) // 2) == 5
''',
'''    # Capacity must not regress any v0.18.18 research level: 0/1/2/3/3/3, then mastery grows to 5.
    capacity = lambda level: min(5, min(3, level) + max(0, level - 4) // 2)
    assert [capacity(level) for level in range(0, 6)] == [0, 1, 2, 3, 3, 3]
    assert capacity(6) == 4 and capacity(8) == 5 and capacity(10) == 5
    assert "mercenaryCapacityAt" in research
''')
text = text.replace(
'''    assert "maxHp(upgradedBase)" in turret
    assert "state = new TurretState(id, state.type(), state.pos(), state.level(), maxHp(state), true);" in turret
''',
'''    assert "maxHp(upgradedBase)" in turret
    assert "state = new TurretState(id, state.type(), state.pos(), state.level(), maxHp(state), true);" in turret
    assert "RESEARCH_DURABILITY_MIGRATION" in turret and "migrateLegacyResearchDurability()" in turret
    assert turret.count("migrateLegacyResearchDurability();") >= 2
    assert "applyResearchDurabilityUpgrade(ServerLevel level, float previousMultiplier)" in turret
    assert "healthRatio" in turret and "previousTowerDurability" in research
''')
text = text.replace(
'''    assert "VillageMercenaryPresentationSystem.remove" in merc
''',
'''    assert "VillageMercenaryPresentationSystem.remove" in merc
    reset_block = merc.split("public static void reset()", 1)[1].split("}", 1)[0]
    assert "VillageMercenaryPresentationSystem.reset()" not in reset_block
    init_prefix = merc.split("public static synchronized void initializeServer", 1)[1].split("public static void reset()", 1)[0]
    assert "VillageMercenaryPresentationSystem.reset();" in init_prefix
''')
write(contract, text)

print("[PASS] v0.18.19 post-audit fixes staged")
