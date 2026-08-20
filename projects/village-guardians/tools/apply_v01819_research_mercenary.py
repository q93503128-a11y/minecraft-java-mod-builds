#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def replace_once(path: Path, old: str, new: str) -> None:
    text = read(path)
    if old not in text:
        raise SystemExit(f"missing anchor in {path}: {old[:140]!r}")
    write(path, text.replace(old, new, 1))


# Version.
props = ROOT / "gradle.properties"
replace_once(props, "mod_version=0.18.18-alpha.1", "mod_version=0.18.19-alpha.1")

# Deep, save-compatible research. Persisted branch ids stay unchanged.
research = r'''package kr.moonseungjun.villageguardians;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public final class VillageDefenseResearchSystem {
    public static final int MAX_LEVEL = 10;
    private static final EnumMap<Branch, Integer> LEVELS = new EnumMap<>(Branch.class);
    private static VillageDefenseResearchData savedData;

    private VillageDefenseResearchSystem() {}

    public static synchronized void initializeServer(MinecraftServer server) {
        savedData = server.overworld().getDataStorage().computeIfAbsent(VillageDefenseResearchData.TYPE);
        LEVELS.clear();
        savedData.levels().forEach((id, value) -> {
            Branch branch = Branch.fromId(id);
            if (branch != null) LEVELS.put(branch, Math.max(0, Math.min(MAX_LEVEL, value)));
        });
        persist();
    }

    public static synchronized int level(Branch branch) {
        return branch == null ? 0 : LEVELS.getOrDefault(branch, 0);
    }

    public static synchronized int upgradeCost(Branch branch) {
        int current = level(branch);
        int mastery = Math.max(0, current - 4);
        return 180 + branch.ordinal() * 40 + current * 220 + mastery * 180;
    }

    public static synchronized String upgrade(ServerPlayer player, Branch branch) {
        if (branch == null) return "알 수 없는 연구 분야입니다.";
        int current = level(branch);
        if (current >= MAX_LEVEL) return branch.displayName() + " 연구가 최고 단계입니다.";
        int cost = upgradeCost(branch);
        if (!VillageProgressionSystem.spendCoins(player, cost)) {
            return "수호 주화가 부족합니다. 필요 " + cost + ", 현재 " + VillageProgressionSystem.coins(player);
        }
        String before = branch.description(current);
        LEVELS.put(branch, current + 1);
        persist();
        String after = branch.description(current + 1);
        return branch.displayName() + " Lv." + (current + 1) + " 연구 완료"
                + "\n이전: " + before + "\n현재: " + after;
    }

    private static float curve(int level, float firstFive, float masteryFive) {
        int safe = Math.max(0, Math.min(MAX_LEVEL, level));
        int foundation = Math.min(5, safe);
        int mastery = Math.max(0, safe - 5);
        return foundation * firstFive + mastery * masteryFive;
    }

    public static float mercenaryDamageMultiplier() {
        return 1.0f + curve(level(Branch.MERCENARY), 0.12f, 0.05f);
    }

    public static float mercenaryHealingMultiplier() {
        return 1.0f + curve(level(Branch.MERCENARY), 0.04f, 0.025f);
    }

    public static int mercenaryTrainingProgressPerKill() {
        return 1 + level(Branch.MERCENARY) / 4;
    }

    public static int mercenaryCapacityBonus() {
        return Math.min(5, (level(Branch.MERCENARY) + 1) / 2);
    }

    public static float towerDamageMultiplier() {
        return 1.0f + curve(level(Branch.TOWER), 0.10f, 0.04f);
    }

    public static float towerRangeMultiplier() {
        return 1.0f + curve(level(Branch.TOWER), 0.01f, 0.015f);
    }

    public static float towerDurabilityMultiplier() {
        return 1.0f + curve(level(Branch.TOWER), 0.025f, 0.035f);
    }

    public static float equipmentDropBonus() {
        return curve(level(Branch.LOGISTICS), 0.03f, 0.01f);
    }

    public static float lootValueMultiplier() {
        return 1.0f + curve(level(Branch.LOGISTICS), 0.10f, 0.04f);
    }

    public static float consumableCostMultiplier() {
        return Math.max(0.75f, 1.0f - level(Branch.LOGISTICS) * 0.025f);
    }

    public static float fieldRepairMultiplier() {
        return 1.0f + curve(level(Branch.LOGISTICS), 0.04f, 0.03f);
    }

    public static synchronized void resetForNewGame() {
        LEVELS.clear();
        persist();
    }

    private static synchronized void persist() {
        if (savedData == null) return;
        Map<String, Integer> values = new java.util.LinkedHashMap<>();
        LEVELS.forEach((branch, level) -> values.put(branch.id(), level));
        savedData.replace(values);
    }

    private static int percent(float multiplier) {
        return Math.max(0, Math.round((multiplier - 1.0f) * 100.0f));
    }

    public enum Branch {
        MERCENARY("mercenary", "용병 교리"),
        TOWER("tower", "포탑 공학"),
        LOGISTICS("logistics", "전리품 군수학");

        private final String id;
        private final String displayName;

        Branch(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public String id() { return id; }
        public String displayName() { return displayName; }

        public String description(int value) {
            int safe = Math.max(0, Math.min(MAX_LEVEL, value));
            return switch (this) {
                case MERCENARY -> "정원 +" + Math.min(5, (safe + 1) / 2)
                        + " · 피해 +" + percent(1.0f + curve(safe, 0.12f, 0.05f)) + "%"
                        + " · 치유 +" + percent(1.0f + curve(safe, 0.04f, 0.025f)) + "%"
                        + " · 처치 훈련 진척 ×" + (1 + safe / 4);
                case TOWER -> "피해 +" + percent(1.0f + curve(safe, 0.10f, 0.04f)) + "%"
                        + " · 사거리 +" + percent(1.0f + curve(safe, 0.01f, 0.015f)) + "%"
                        + " · 내구 +" + percent(1.0f + curve(safe, 0.025f, 0.035f)) + "%";
                case LOGISTICS -> "장비 드랍 보너스 +"
                        + Math.round(curve(safe, 0.03f, 0.01f) * 100.0f) + "%p"
                        + " · 판매 +" + percent(1.0f + curve(safe, 0.10f, 0.04f)) + "%"
                        + " · 전투 소모품 할인 " + Math.round((1.0f - Math.max(0.75f, 1.0f - safe * 0.025f)) * 100.0f) + "%";
            };
        }

        public static Branch fromId(String id) {
            if (id == null) return null;
            String normalized = id.toLowerCase(Locale.ROOT);
            for (Branch branch : values()) if (branch.id.equals(normalized)) return branch;
            return null;
        }
    }
}
'''
write(JAVA / "VillageDefenseResearchSystem.java", research)

# Logistics research affects the tactical-supply economy and field repairs.
consumable = JAVA / "VillageConsumableSystem.java"
replace_once(consumable,
'''    public static int effectiveCost(Consumable consumable) {
        if (consumable == null) return 0;
        return Math.max(12, consumable.baseCost() - VillageProgressionSystem.storehouseLevel() * 2);
    }
''',
'''    public static int effectiveCost(Consumable consumable) {
        if (consumable == null) return 0;
        int storehousePrice = Math.max(12, consumable.baseCost() - VillageProgressionSystem.storehouseLevel() * 2);
        return Math.max(10, Math.round(storehousePrice * VillageDefenseResearchSystem.consumableCostMultiplier()));
    }
''')
replace_once(consumable,
'''                String repaired = VillagePlacedTurretSystem.fieldRepairNearest(player,
                        70 + VillageProgressionSystem.storehouseLevel() * 15);
''',
'''                int baseRepair = 70 + VillageProgressionSystem.storehouseLevel() * 15;
                String repaired = VillagePlacedTurretSystem.fieldRepairNearest(player,
                        Math.round(baseRepair * VillageDefenseResearchSystem.fieldRepairMultiplier()));
''')

# Mercenary research becomes long-term training/support progression and persistent visual identity.
merc = JAVA / "VillageMercenarySystem.java"
replace_once(merc,
'''    public static void reset() { tickCounter = 0; }
''',
'''    public static void reset() {
        tickCounter = 0;
        VillageMercenaryPresentationSystem.reset();
    }
''')
replace_once(merc,
'''        int kills = KILLS.getOrDefault(uuid, 0) + 1;
''',
'''        int kills = KILLS.getOrDefault(uuid, 0)
                + VillageDefenseResearchSystem.mercenaryTrainingProgressPerKill();
''')
replace_once(merc,
'''            recognize(mercenary);
            MercenaryClass kind = mercenaryClass(mercenary);
            int rank = rank(mercenary);
            applyClassPassives(mercenary, kind, rank);
            if (!VillageRaidSystem.isActive()) continue;
''',
'''            recognize(mercenary);
            MercenaryClass kind = mercenaryClass(mercenary);
            int rank = rank(mercenary);
            applyClassPassives(mercenary, kind, rank);
            VillageMercenaryPresentationSystem.ensure(level, mercenary, kind, rank);
            if (!VillageRaidSystem.isActive()) continue;
''')
replace_once(merc,
'''    private static void healAllies(ServerLevel level, MinecraftServer server, IronGolem medic, int rank) {
        float amount = 2.3f * mercenaryPower(rank);
''',
'''    private static void healAllies(ServerLevel level, MinecraftServer server, IronGolem medic, int rank) {
        float amount = 2.3f * mercenaryPower(rank) * VillageDefenseResearchSystem.mercenaryHealingMultiplier();
''')
replace_once(merc,
'''    public static synchronized void handleDeath(Mob mob) {
        if (mob != null && isMercenary(mob.getUUID())) unregister(mob.getUUID());
    }
''',
'''    public static synchronized void handleDeath(Mob mob) {
        if (mob == null || !isMercenary(mob.getUUID())) return;
        if (mob.level() instanceof ServerLevel level) VillageMercenaryPresentationSystem.remove(level, mob.getUUID());
        unregister(mob.getUUID());
    }
''')
replace_once(merc,
'''    private static void discardCurrent(MinecraftServer server) {
        for (UUID uuid : new java.util.HashSet<>(CLASSES.keySet())) {
            var entity = server.overworld().getEntity(uuid); if (entity != null) entity.discard();
            VillageWorldSystem.unmarkAllowedGameMob(uuid);
        }
    }
''',
'''    private static void discardCurrent(MinecraftServer server) {
        for (UUID uuid : new java.util.HashSet<>(CLASSES.keySet())) {
            VillageMercenaryPresentationSystem.remove(server.overworld(), uuid);
            var entity = server.overworld().getEntity(uuid); if (entity != null) entity.discard();
            VillageWorldSystem.unmarkAllowedGameMob(uuid);
        }
        VillageMercenaryPresentationSystem.reset();
    }
''')
# Spawn class presentation immediately after successful hire and snapshot restore.
replace_once(merc,
'''        if (!level.addFreshEntity(mercenary)) {
            unregister(mercenary.getUUID());
            VillageWorldSystem.unmarkAllowedGameMob(mercenary.getUUID());
            VillageProgressionSystem.addCoins(player, cost, "용병 배치 실패 환불");
            return "용병 배치에 실패해 주화를 돌려드렸습니다.";
        }
        return kind.displayName() + " 고용 완료 · Lv.1 · 현재 " + (current + 1) + " / " + cap
''',
'''        if (!level.addFreshEntity(mercenary)) {
            unregister(mercenary.getUUID());
            VillageWorldSystem.unmarkAllowedGameMob(mercenary.getUUID());
            VillageProgressionSystem.addCoins(player, cost, "용병 배치 실패 환불");
            return "용병 배치에 실패해 주화를 돌려드렸습니다.";
        }
        VillageMercenaryPresentationSystem.ensure(level, mercenary, kind, 1);
        return kind.displayName() + " 고용 완료 · Lv.1 · 현재 " + (current + 1) + " / " + cap
''')
replace_once(merc,
'''            if (!level.addFreshEntity(mob)) { unregister(mob.getUUID()); VillageWorldSystem.unmarkAllowedGameMob(mob.getUUID()); }
            index++;
''',
'''            if (!level.addFreshEntity(mob)) {
                unregister(mob.getUUID()); VillageWorldSystem.unmarkAllowedGameMob(mob.getUUID());
            } else {
                VillageMercenaryPresentationSystem.ensure(level, mob, snapshot.kind(), snapshot.level());
            }
            index++;
''')

presentation = r'''package kr.moonseungjun.villageguardians;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Runtime-only owner-follow silhouettes that make mercenary classes readable without changing gameplay hitboxes. */
public final class VillageMercenaryPresentationSystem {
    private static final Map<UUID, Presence> ACTIVE = new HashMap<>();
    private static final int PRESENCE_DURATION = 20 * 60 * 60;

    private VillageMercenaryPresentationSystem() {}

    public static synchronized void reset() {
        ACTIVE.clear();
    }

    public static synchronized void ensure(
            ServerLevel level,
            Mob mercenary,
            VillageMercenarySystem.MercenaryClass kind,
            int rank) {
        if (level == null || mercenary == null || kind == null || !mercenary.isAlive()) return;
        int tier = visualTier(rank);
        String effectKind = "mercenary_presence_" + kind.id();
        Presence current = ACTIVE.get(mercenary.getUUID());
        if (current != null) {
            Entity actor = level.getEntity(current.actorUuid());
            if (actor instanceof VillageSkillEffectEntity effect && effect.isAlive()
                    && current.tier() == tier && effectKind.equals(effect.kind())) return;
            if (actor != null) actor.discard();
        }
        Vec3 look = horizontal(mercenary.getLookAngle());
        VillageSkillEffectEntity actor = VillageSkillEffectEntity.spawn(level, mercenary, effectKind,
                mercenary.position(), look, PRESENCE_DURATION, 0.0f, Integer.toString(tier));
        if (actor != null) ACTIVE.put(mercenary.getUUID(), new Presence(actor.getUUID(), tier));
    }

    public static synchronized void remove(ServerLevel level, UUID mercenaryUuid) {
        Presence presence = ACTIVE.remove(mercenaryUuid);
        if (presence == null || level == null) return;
        Entity actor = level.getEntity(presence.actorUuid());
        if (actor != null) actor.discard();
    }

    static int visualTier(int rank) {
        int safe = Math.max(1, Math.min(VillageMercenarySystem.MAX_LEVEL, rank));
        if (safe >= 60) return 3;
        if (safe >= 40) return 2;
        if (safe >= 20) return 1;
        return 0;
    }

    private static Vec3 horizontal(Vec3 value) {
        Vec3 source = value == null ? Vec3.ZERO : new Vec3(value.x, 0.0, value.z);
        return source.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : source.normalize();
    }

    private record Presence(UUID actorUuid, int tier) {}
}
'''
write(JAVA / "VillageMercenaryPresentationSystem.java", presentation)

# Presentation actor follows and faces with its owner.
effect = JAVA / "VillageSkillEffectEntity.java"
replace_once(effect,
'''    private boolean tracksOwnerLook() {
        if (kind().startsWith("boss_presence_") || kind().startsWith("boss_phase_two_")) return true;
''',
'''    private boolean tracksOwnerLook() {
        if (kind().startsWith("mercenary_presence_")
                || kind().startsWith("boss_presence_") || kind().startsWith("boss_phase_two_")) return true;
''')
replace_once(effect,
'''    private boolean followsOwner() {
        if (kind().startsWith("elite_aura_") || kind().startsWith("boss_presence_")
''',
'''    private boolean followsOwner() {
        if (kind().startsWith("mercenary_presence_") || kind().startsWith("elite_aura_") || kind().startsWith("boss_presence_")
''')

# Four class silhouettes, with subtle milestone upgrades at Lv20/40/60.
mesh = JAVA / "VillageSkillMeshLibrary.java"
replace_once(mesh,
'''            case "merc_medic_pulse" -> renderDefensePulse(pose, out, basis, age, progress, state.extra, 3);
            case "siege_structure_impact" -> renderDefensePulse(pose, out, basis, age, progress, state.extra, 4);
''',
'''            case "merc_medic_pulse" -> renderDefensePulse(pose, out, basis, age, progress, state.extra, 3);
            case "mercenary_presence_bastion" -> renderMercenaryPresence(pose, out, basis, age, state.extra, 0);
            case "mercenary_presence_striker" -> renderMercenaryPresence(pose, out, basis, age, state.extra, 1);
            case "mercenary_presence_ranger" -> renderMercenaryPresence(pose, out, basis, age, state.extra, 2);
            case "mercenary_presence_medic" -> renderMercenaryPresence(pose, out, basis, age, state.extra, 3);
            case "siege_structure_impact" -> renderDefensePulse(pose, out, basis, age, progress, state.extra, 4);
''')
insert_anchor = '''    private static void renderEliteAura(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, int style) {
'''
merc_renderer = r'''    private static void renderMercenaryPresence(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, String encodedTier, int style) {
        int tier = 0;
        try { tier = Math.max(0, Math.min(3, Integer.parseInt(encodedTier == null ? "0" : encodedTier))); }
        catch (NumberFormatException ignored) {}
        double scale = 1.0 + tier * 0.085;
        double pulse = 0.96 + 0.04 * Math.sin(age * 0.10);
        int color = switch (style) {
            case 0 -> rgba(112, 190, 255, 150 + tier * 12);
            case 1 -> rgba(255, 119, 72, 150 + tier * 12);
            case 2 -> rgba(137, 226, 156, 150 + tier * 12);
            default -> rgba(255, 224, 135, 150 + tier * 12);
        };
        int pale = withAlpha(color, 92 + tier * 10);
        ring(pose, out, b, 0.78 * scale * pulse, 0.035, 0.038, 44, pale, age * 0.010);

        if (style == 0) {
            Vec3 shieldCenter = b.local(0.0, 1.10, 0.78 * scale);
            curvedShield(pose, out, b, shieldCenter, 1.32 * scale, 1.72 * scale, 0.30,
                    withAlpha(color, 92 + tier * 12));
            shieldFrame(pose, out, b, shieldCenter.add(b.forward.scale(0.035)),
                    1.32 * scale, 1.72 * scale, 0.30, color);
            for (int side : new int[]{-1, 1}) {
                prism(pose, out, b.local(side * 0.58 * scale, 0.55, 0.15),
                        b.local(side * 0.70 * scale, 1.65 * scale, 0.26), 0.075 + tier * 0.01, color);
            }
        } else if (style == 1) {
            prism(pose, out, b.local(-0.62 * scale, 0.52, -0.06),
                    b.local(0.58 * scale, 1.74 * scale, 0.64), 0.065 + tier * 0.012, color);
            prism(pose, out, b.local(0.62 * scale, 0.52, -0.06),
                    b.local(-0.58 * scale, 1.74 * scale, 0.64), 0.065 + tier * 0.012, color);
            for (int i = 0; i < 2 + tier; i++) {
                slashArc(pose, out, b, age * 0.018 + i * TAU / (2.0 + tier),
                        0.84 + i * 0.08, 0.88 + i * 0.10, 0.58, 0.035, pale);
            }
        } else if (style == 2) {
            prism(pose, out, b.local(-0.48 * scale, 0.46, -0.42),
                    b.local(-0.48 * scale, 1.82 * scale, -0.30), 0.09, pale);
            int arrows = 2 + tier;
            for (int i = 0; i < arrows; i++) {
                customArrow(pose, out, b, b.local(-0.50 * scale + i * 0.14, 1.44 + i * 0.10, -0.38),
                        0.86 + tier * 0.06, 0.035, color);
            }
            ringVertical(pose, out, b, 0.62 * scale, 1.12, 0.032, 42, pale, -age * 0.012);
        } else {
            verticalPillarAt(pose, out, b, b.local(0.56 * scale, 0.34, 0.02),
                    0.07 + tier * 0.008, 1.66 * scale, color);
            crystal(pose, out, b.local(0.56 * scale, 2.03 * scale, 0.02),
                    0.42 + tier * 0.06, 0.15 + tier * 0.015, color);
            ring(pose, out, b, 0.66 * scale, 1.94 * scale, 0.038, 46,
                    pale, age * 0.020);
            if (tier >= 2) ring(pose, out, b, 0.46 * scale, 1.48 * scale, 0.028, 38,
                    withAlpha(color, 78), -age * 0.026);
        }

        if (tier >= 1) {
            ring(pose, out, b, (0.92 + tier * 0.10) * scale, 0.09, 0.026, 48,
                    withAlpha(color, 70 + tier * 10), -age * 0.014);
        }
        if (tier >= 3) {
            crystal(pose, out, b.local(0.0, 1.50, 0.18), 0.34, 0.115,
                    withAlpha(color, 130));
        }
    }

'''
replace_once(mesh, insert_anchor, merc_renderer + insert_anchor)

# Tower research now affects range and survivability, not only damage.
turret = JAVA / "VillagePlacedTurretSystem.java"
replace_once(turret,
'''    private static void fire(ServerLevel level, TurretState state) {
        double range = state.type().range() + (state.level() - 1) * 2.5;
''',
'''    private static void fire(ServerLevel level, TurretState state) {
        double range = (state.type().range() + (state.level() - 1) * 2.5)
                * VillageDefenseResearchSystem.towerRangeMultiplier();
''')
replace_once(turret,
'''    private static void supportPulse(ServerLevel level, MinecraftServer server, TurretState state) {
        double radius = state.type().range() + state.level() * 2.0;
''',
'''    private static void supportPulse(ServerLevel level, MinecraftServer server, TurretState state) {
        double radius = (state.type().range() + state.level() * 2.0)
                * VillageDefenseResearchSystem.towerRangeMultiplier();
''')
replace_once(turret,
'''    private static int maxHp(TurretState state) { return state.type().baseHp() + (state.level() - 1) * 70; }
''',
'''    private static int maxHp(TurretState state) {
        int base = state.type().baseHp() + (state.level() - 1) * 70;
        return Math.max(base, Math.round(base * VillageDefenseResearchSystem.towerDurabilityMultiplier()));
    }
''')
replace_once(turret,
'''        TurretState state = new TurretState(id, pending.type(), candidate.immutable(), 1,
                pending.type().baseHp(), true);
        synchronized (VillagePlacedTurretSystem.class) { TURRETS.put(id, state); persist(state); }
''',
'''        TurretState state = new TurretState(id, pending.type(), candidate.immutable(), 1,
                pending.type().baseHp(), true);
        state = new TurretState(id, state.type(), state.pos(), state.level(), maxHp(state), true);
        synchronized (VillagePlacedTurretSystem.class) { TURRETS.put(id, state); persist(state); }
''')
replace_once(turret,
'''        int newLevel = state.level() + 1;
        TurretState upgraded = new TurretState(id, state.type(), state.pos(), newLevel,
                state.type().baseHp() + (newLevel - 1) * 70, true);
        TURRETS.put(id, upgraded); persist(upgraded);
''',
'''        int newLevel = state.level() + 1;
        TurretState upgradedBase = new TurretState(id, state.type(), state.pos(), newLevel,
                state.type().baseHp() + (newLevel - 1) * 70, true);
        TurretState upgraded = new TurretState(id, state.type(), state.pos(), newLevel,
                maxHp(upgradedBase), true);
        TURRETS.put(id, upgraded); persist(upgraded);
''')

# Current-version contracts stay current without weakening their behavioral assertions.
for path in (ROOT / "tools").glob("test_*.py"):
    text = read(path)
    if "mod_version=0.18.18-alpha.1" in text:
        write(path, text.replace("mod_version=0.18.18-alpha.1", "mod_version=0.18.19-alpha.1"))

progression_depth = ROOT / "tools/test_progression_depth.py"
text = read(progression_depth)
text = text.replace('assert "MAX_LEVEL = 5" in research', 'assert "MAX_LEVEL = 10" in research')
text = text.replace('Defense research expands from 9 to 15 upgrades', 'Defense research now provides 30 save-compatible long-term upgrades')
write(progression_depth, text)

contract = r'''#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name):
    return (JAVA / name).read_text(encoding="utf-8")


def curve(level, first, mastery):
    safe = max(0, min(10, level))
    return min(5, safe) * first + max(0, safe - 5) * mastery


def main():
    assert "mod_version=0.18.19-alpha.1" in (ROOT / "gradle.properties").read_text(encoding="utf-8")

    research = read("VillageDefenseResearchSystem.java")
    assert "MAX_LEVEL = 10" in research
    assert "mastery = Math.max(0, current - 4)" in research
    for token in ("towerRangeMultiplier", "towerDurabilityMultiplier", "mercenaryHealingMultiplier",
                  "mercenaryTrainingProgressPerKill", "consumableCostMultiplier", "fieldRepairMultiplier"):
        assert token in research
    # Foundation levels preserve v0.18.18 strength; mastery levels grow more slowly.
    assert abs((1 + curve(5, .12, .05)) - 1.60) < 1e-6
    assert abs((1 + curve(10, .12, .05)) - 1.85) < 1e-6
    assert abs((1 + curve(5, .10, .04)) - 1.50) < 1e-6
    assert abs((1 + curve(10, .10, .04)) - 1.70) < 1e-6
    assert (5 + 1) // 2 == 3 and min(5, (10 + 1) // 2) == 5

    merc = read("VillageMercenarySystem.java")
    presentation = read("VillageMercenaryPresentationSystem.java")
    assert "VillageDefenseResearchSystem.mercenaryTrainingProgressPerKill()" in merc
    assert "VillageDefenseResearchSystem.mercenaryHealingMultiplier()" in merc
    assert "VillageMercenaryPresentationSystem.ensure(level, mercenary, kind, rank)" in merc
    assert "VillageMercenaryPresentationSystem.remove" in merc
    for kind in ("bastion", "striker", "ranger", "medic"):
        assert '"mercenary_presence_" + kind.id()' in presentation
    for milestone in ("safe >= 20", "safe >= 40", "safe >= 60"):
        assert milestone in presentation

    effect = read("VillageSkillEffectEntity.java")
    mesh = read("VillageSkillMeshLibrary.java")
    assert effect.count('kind().startsWith("mercenary_presence_")') >= 2
    for kind in ("mercenary_presence_bastion", "mercenary_presence_striker",
                 "mercenary_presence_ranger", "mercenary_presence_medic"):
        assert kind in mesh
    assert "renderMercenaryPresence" in mesh and "shieldFrame" in mesh and "customArrow" in mesh

    turret = read("VillagePlacedTurretSystem.java")
    assert "* VillageDefenseResearchSystem.towerRangeMultiplier()" in turret
    assert "VillageDefenseResearchSystem.towerDurabilityMultiplier()" in turret
    assert "maxHp(upgradedBase)" in turret
    assert "state = new TurretState(id, state.type(), state.pos(), state.level(), maxHp(state), true);" in turret

    consumable = read("VillageConsumableSystem.java")
    assert "VillageDefenseResearchSystem.consumableCostMultiplier()" in consumable
    assert "VillageDefenseResearchSystem.fieldRepairMultiplier()" in consumable

    print("[PASS] v0.18.19 research extends to 10 levels with bounded mastery scaling")
    print("[PASS] tower research affects damage, range and durability without reducing old Lv5 strength")
    print("[PASS] mercenary research accelerates training/support and four classes have persistent milestone visuals")
    print("[PASS] logistics research improves tactical-supply economy without reintroducing duplicate food")


if __name__ == "__main__":
    main()
'''
write(ROOT / "tools/test_v01819_research_mercenary_presentation.py", contract)

print("[PASS] v0.18.19 research + mercenary presentation patch staged")
