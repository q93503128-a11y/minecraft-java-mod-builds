package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * Coarse-grained fortress durability. Blocks are only the visual/physical projection of a segment HP pool;
 * individual blocks never own combat HP and all damage uses no-drop setBlock updates.
 */
public final class VillageSiegeSegmentSystem {
    private static final int WALL_HEIGHT = 9;
    private static final int WALL_THICKNESS = 5;
    private static final int BREACH_HALF_WIDTH = 2;

    private VillageSiegeSegmentSystem() {}

    public static int currentHp(Segment segment) {
        if (segment == Segment.NORTH_GATE) {
            return VillageProgressionSystem.durability(VillageProgressionSystem.Building.WALLS);
        }
        int maximum = maxHp(segment);
        return Math.max(0, Math.min(maximum,
                VillageSiegePersistence.getInt("segment_hp_" + segment.id(), maximum)));
    }

    public static int maxHp(Segment segment) {
        if (segment == Segment.NORTH_GATE) {
            return VillageProgressionSystem.maxDurability(VillageProgressionSystem.Building.WALLS);
        }
        int wall = VillageProgressionSystem.wallLevel();
        int local = upgradeLevel(segment);
        return 760 + wall * 190 + local * 170;
    }

    public static int upgradeLevel(Segment segment) {
        if (segment == Segment.NORTH_GATE) return VillageProgressionSystem.wallLevel();
        return Math.max(0, Math.min(3,
                VillageSiegePersistence.getInt("segment_upgrade_" + segment.id(), 0)));
    }

    public static DamageState damageState(Segment segment) {
        int hp = currentHp(segment);
        if (hp <= 0) return DamageState.BREACHED;
        float ratio = hp / (float) Math.max(1, maxHp(segment));
        if (ratio > 0.70f) return DamageState.HEALTHY;
        if (ratio > 0.40f) return DamageState.CRACKED;
        return DamageState.HEAVY;
    }

    public static boolean breached(Segment segment) { return currentHp(segment) <= 0; }

    public static String statusLine(Segment segment) {
        return segment.displayName() + " · " + currentHp(segment) + "/" + maxHp(segment)
                + " HP · " + damageState(segment).displayName()
                + " · 방어 " + defenseGrade(segment) + " · 강화 " + upgradeLevel(segment);
    }

    public static String defenseGrade(Segment segment) {
        int score = VillageProgressionSystem.wallLevel() + upgradeLevel(segment);
        if (score >= 7) return "S";
        if (score >= 5) return "A";
        if (score >= 3) return "B";
        return score >= 1 ? "C" : "D";
    }

    public static void damage(MinecraftServer server, Segment segment, int rawDamage, BlockPos impact) {
        if (server == null || segment == null || rawDamage <= 0) return;
        if (segment == Segment.NORTH_GATE) {
            VillageProgressionSystem.damageBuilding(server, VillageProgressionSystem.Building.WALLS, rawDamage);
            return;
        }
        int before = currentHp(segment);
        if (before <= 0) return;
        float mitigation = Math.min(0.42f,
                VillageProgressionSystem.wallLevel() * 0.035f + upgradeLevel(segment) * 0.055f);
        int damage = Math.max(1, Math.round(rawDamage * (1.0f - mitigation)));
        int after = Math.max(0, before - damage);
        VillageSiegePersistence.putInt("segment_hp_" + segment.id(), after);
        int breachAxis = clampAxis(segment, impact == null ? nominalAxis(segment) : axisValue(segment, impact));
        VillageSiegePersistence.putInt("segment_breach_" + segment.id(), breachAxis);
        applyDamageVisual(server.overworld(), segment, damageState(segment), breachAxis);
        if (after == 0) {
            VillageDefenseEffectSystem.breachAlarm(server.overworld(),
                    Vec3.atCenterOf(attackPoint(segment, impact)));
            server.getPlayerList().broadcastSystemMessage(Component.literal(
                    "§4[성벽 돌파] §f" + segment.displayName()
                            + "에 국소 돌파구가 생겼습니다. 적이 새 진입로로 사용할 수 있습니다."), false);
        }
    }

    public static String repair(ServerPlayer player, Segment segment) {
        if (segment == null) return "알 수 없는 방어 구역입니다.";
        String blocked = maintenanceBlockReason("수리");
        if (blocked != null) return blocked;
        if (segment == Segment.NORTH_GATE) {
            return VillageProgressionSystem.repair(player, VillageProgressionSystem.Building.WALLS);
        }
        int current = currentHp(segment);
        int maximum = maxHp(segment);
        if (current >= maximum) return segment.displayName() + "은(는) 이미 완전합니다.";
        int missing = maximum - current;
        int cost = Math.max(35, (missing + 8) / 9);
        if (!VillageProgressionSystem.spendCoins(player, cost)) {
            return "수리 주화가 부족합니다. 필요 " + cost + ", 현재 " + VillageProgressionSystem.coins(player);
        }
        VillageSiegePersistence.putInt("segment_hp_" + segment.id(), maximum);
        int axis = VillageSiegePersistence.getInt("segment_breach_" + segment.id(), nominalAxis(segment));
        if (player.level() instanceof ServerLevel level) restoreLocalWall(level, segment, axis);
        return segment.displayName() + " 국소 손상 복구 완료 · 주화 " + cost + " 사용";
    }

    public static String upgrade(ServerPlayer player, Segment segment) {
        if (segment == null) return "알 수 없는 방어 구역입니다.";
        String blocked = maintenanceBlockReason("강화");
        if (blocked != null) return blocked;
        if (segment == Segment.NORTH_GATE) {
            return VillageProgressionSystem.upgrade(player, VillageProgressionSystem.Building.WALLS);
        }
        int current = upgradeLevel(segment);
        if (current >= 3) return segment.displayName() + " 구역 강화가 최고 단계입니다.";
        int cost = 150 + current * 180;
        if (!VillageProgressionSystem.spendCoins(player, cost)) {
            return "강화 주화가 부족합니다. 필요 " + cost + ", 현재 " + VillageProgressionSystem.coins(player);
        }
        int oldMax = maxHp(segment);
        VillageSiegePersistence.putInt("segment_upgrade_" + segment.id(), current + 1);
        int newMax = maxHp(segment);
        VillageSiegePersistence.putInt("segment_hp_" + segment.id(),
                Math.min(newMax, currentHp(segment) + Math.max(0, newMax - oldMax)));
        return segment.displayName() + " 구역 강화 " + (current + 1)
                + "단계 완료 · 최대 HP " + newMax + " · 방어 등급 " + defenseGrade(segment);
    }

    private static String maintenanceBlockReason(String action) {
        if (VillageProgressionSystem.isGameOver()) {
            return "게임 오버 상태에서는 성벽 " + action + "을(를) 실행할 수 없습니다. 재시작을 먼저 선택하세요.";
        }
        if (VillageRaidSystem.isRaidLocked() || VillageCouncilState.currentPhase() != VillageTimePhase.DAY) {
            return "성벽 " + action + "은(는) 낮 정비 시간에만 가능합니다.";
        }
        return null;
    }

    public static BlockPos attackPoint(Segment segment, BlockPos attacker) {
        BlockPos center = VillageCouncilState.villageCenter().orElse(new BlockPos(0, 0, 0));
        if (segment == Segment.NORTH_GATE) return VillageWorldSystem.northGateTarget();
        int axis = breached(segment)
                ? VillageSiegePersistence.getInt("segment_breach_" + segment.id(), nominalAxis(segment))
                : clampAxis(segment, attacker == null ? nominalAxis(segment) : axisValue(segment, attacker));
        return pointAt(center, segment, axis, false);
    }

    public static BlockPos insideApproach(Segment segment) {
        BlockPos center = VillageCouncilState.villageCenter().orElse(new BlockPos(0, 0, 0));
        if (segment == Segment.NORTH_GATE) return VillageWorldSystem.northInnerApproach();
        int axis = VillageSiegePersistence.getInt("segment_breach_" + segment.id(), nominalAxis(segment));
        return pointAt(center, segment, clampAxis(segment, axis), true);
    }

    public static boolean touching(Segment segment, BlockPos mob) {
        if (mob == null) return false;
        BlockPos point = attackPoint(segment, mob);
        long dx = (long) mob.getX() - point.getX();
        long dz = (long) mob.getZ() - point.getZ();
        return dx * dx + dz * dz <= 16L && Math.abs(mob.getY() - point.getY()) <= 5;
    }

    public static Segment primarySideFor(VillageAttackPlanSystem.Front front) {
        return switch (front) {
            case NORTH -> Segment.NORTH_GATE;
            case NORTH_WEST -> Segment.NORTH_WEST;
            case NORTH_EAST -> Segment.NORTH_EAST;
            case WEST -> Segment.WEST;
            case EAST -> Segment.EAST;
            case SOUTH_WEST -> Segment.SOUTH_WEST;
            case SOUTH_EAST -> Segment.SOUTH_EAST;
        };
    }

    public static void restoreAllVisuals(ServerLevel level) {
        for (Segment segment : Segment.values()) {
            if (segment == Segment.NORTH_GATE) continue;
            int axis = VillageSiegePersistence.getInt("segment_breach_" + segment.id(), nominalAxis(segment));
            if (damageState(segment) == DamageState.HEALTHY) restoreLocalWall(level, segment, axis);
            else applyDamageVisual(level, segment, damageState(segment), axis);
        }
    }

    private static void applyDamageVisual(ServerLevel level, Segment segment, DamageState state, int axis) {
        if (segment == Segment.NORTH_GATE) return;
        restoreLocalWall(level, segment, axis);
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null || state == DamageState.HEALTHY) return;
        int groundY = center.getY() - 1;
        for (int along = -3; along <= 3; along++) {
            int hash = Math.floorMod(axis * 13 + along * 17 + segment.ordinal() * 19, 7);
            if (hash > (state == DamageState.CRACKED ? 2 : 4)) continue;
            BlockPos face = wallCell(center, segment, axis + along, 0, groundY + 2 + Math.floorMod(hash, 6));
            VillageFortressTerrain.set(level, face, hash % 2 == 0 ? Blocks.CRACKED_STONE_BRICKS : Blocks.COBBLESTONE);
        }
        if (state == DamageState.HEAVY || state == DamageState.BREACHED) {
            for (int along = -3; along <= 3; along++) {
                if (Math.floorMod(axis + along + segment.ordinal(), 3) != 0) continue;
                for (int depth = 0; depth < WALL_THICKNESS; depth++) {
                    VillageFortressTerrain.set(level,
                            wallCell(center, segment, axis + along, depth, groundY + 7 + Math.floorMod(along, 2)),
                            Blocks.AIR);
                }
            }
        }
        if (state == DamageState.BREACHED) {
            for (int along = -BREACH_HALF_WIDTH; along <= BREACH_HALF_WIDTH; along++) {
                for (int depth = 0; depth < WALL_THICKNESS; depth++) {
                    for (int y = 1; y <= 4; y++) {
                        VillageFortressTerrain.set(level,
                                wallCell(center, segment, axis + along, depth, groundY + y), Blocks.AIR);
                    }
                }
            }
        }
    }

    private static void restoreLocalWall(ServerLevel level, Segment segment, int axis) {
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null || segment == Segment.NORTH_GATE) return;
        int groundY = center.getY() - 1;
        for (int along = -4; along <= 4; along++) {
            int safeAxis = clampAxis(segment, axis + along);
            for (int depth = 0; depth < WALL_THICKNESS; depth++) {
                for (int y = 1; y <= WALL_HEIGHT; y++) {
                    Block material = y <= 2 || y >= 8 ? Blocks.STONE_BRICKS : Blocks.COBBLESTONE;
                    VillageFortressTerrain.set(level,
                            wallCell(center, segment, safeAxis, depth, groundY + y), material);
                }
            }
        }
    }

    private static BlockPos wallCell(BlockPos center, Segment segment, int axis, int depth, int y) {
        int r = VillageWorldSystem.FORTRESS_RADIUS;
        return switch (segment.orientation()) {
            case NORTH -> new BlockPos(center.getX() + axis, y, center.getZ() - r + depth);
            case SOUTH -> new BlockPos(center.getX() + axis, y, center.getZ() + r - WALL_THICKNESS + 1 + depth);
            case WEST -> new BlockPos(center.getX() - r + depth, y, center.getZ() + axis);
            case EAST -> new BlockPos(center.getX() + r - WALL_THICKNESS + 1 + depth, y, center.getZ() + axis);
            case GATE -> VillageWorldSystem.northGateTarget().atY(y);
        };
    }

    private static BlockPos pointAt(BlockPos center, Segment segment, int axis, boolean inside) {
        int r = VillageWorldSystem.FORTRESS_RADIUS;
        int offset = inside ? 8 : -2;
        return switch (segment.orientation()) {
            case NORTH -> center.offset(axis, 0, -r + offset);
            case SOUTH -> center.offset(axis, 0, r - offset);
            case WEST -> center.offset(-r + offset, 0, axis);
            case EAST -> center.offset(r - offset, 0, axis);
            case GATE -> inside ? VillageWorldSystem.northInnerApproach() : VillageWorldSystem.northGateTarget();
        };
    }

    private static int axisValue(Segment segment, BlockPos pos) {
        BlockPos center = VillageCouncilState.villageCenter().orElse(new BlockPos(0, 0, 0));
        return switch (segment.orientation()) {
            case NORTH, SOUTH -> pos.getX() - center.getX();
            case WEST, EAST -> pos.getZ() - center.getZ();
            case GATE -> 0;
        };
    }

    private static int nominalAxis(Segment segment) { return (segment.minAxis() + segment.maxAxis()) / 2; }
    private static int clampAxis(Segment segment, int axis) {
        return Math.max(segment.minAxis(), Math.min(segment.maxAxis(), axis));
    }

    public enum DamageState {
        HEALTHY("정상"), CRACKED("균열"), HEAVY("대파"), BREACHED("돌파");
        private final String displayName;
        DamageState(String displayName) { this.displayName = displayName; }
        public String displayName() { return displayName; }
    }

    public enum Segment {
        NORTH_WEST("north_west", "북서 성벽", Orientation.NORTH, -64, -20),
        NORTH_GATE("north_gate", "북문", Orientation.GATE, -9, 9),
        NORTH_EAST("north_east", "북동 성벽", Orientation.NORTH, 20, 64),
        WEST("west", "서쪽 방벽", Orientation.WEST, -58, 58),
        EAST("east", "동쪽 방벽", Orientation.EAST, -58, 58),
        SOUTH_WEST("south_west", "후방 서측 방벽", Orientation.SOUTH, -64, -8),
        SOUTH_EAST("south_east", "후방 동측 방벽", Orientation.SOUTH, 8, 64);

        private final String id;
        private final String displayName;
        private final Orientation orientation;
        private final int minAxis;
        private final int maxAxis;
        Segment(String id, String displayName, Orientation orientation, int minAxis, int maxAxis) {
            this.id = id; this.displayName = displayName; this.orientation = orientation;
            this.minAxis = minAxis; this.maxAxis = maxAxis;
        }
        public String id() { return id; }
        public String displayName() { return displayName; }
        Orientation orientation() { return orientation; }
        int minAxis() { return minAxis; }
        int maxAxis() { return maxAxis; }
        public static Segment fromId(String id) {
            if (id == null) return null;
            String value = id.toLowerCase(Locale.ROOT);
            for (Segment segment : values()) if (segment.id.equals(value)) return segment;
            return null;
        }
    }

    enum Orientation { NORTH, SOUTH, WEST, EAST, GATE }
}
