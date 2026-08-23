package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

public final class SettlementBenefitService {
    public static final String WATCH_GUARD_TAG = "frontier_settlement_watch_guard";
    private static final String WATCH_ASSIGNMENT_PREFIX = "frontier_settlement_watchtower_";
    private static final int REPAIR_INTERVAL_TICKS = 100;
    private static final int GUARD_CHECK_INTERVAL_TICKS = 200;
    private static final int WATCHTOWER_CHECK_INTERVAL_TICKS = 100;
    private static final double BLACKSMITH_RADIUS_SQR = 10.0D * 10.0D;
    private static final double WATCHTOWER_ALERT_RADIUS = 40.0D;
    private static final double WATCH_GUARD_SEARCH_RADIUS = 48.0D;
    private static final double WATCH_GUARD_HOME_RADIUS_SQR = 14.0D * 14.0D;
    private static final int REPAIR_PER_METAL = 16;
    private static final EquipmentSlot[] REPAIR_SLOTS = {
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private SettlementBenefitService() {}

    public static void tick(MinecraftServer server, SettlementData data) {
        int tick = server.getTickCount();
        if (tick % REPAIR_INTERVAL_TICKS == 0) repairNearbyEquipment(server, data);
        if (tick % GUARD_CHECK_INTERVAL_TICKS == 0) maintainGuards(server.overworld(), data);
        if (tick % WATCHTOWER_CHECK_INTERVAL_TICKS == 0) maintainWatchtowers(server.overworld(), data);
    }

    private static void repairNearbyEquipment(MinecraftServer server, SettlementData data) {
        ServerLevel level = server.overworld();
        boolean changed = false;
        for (BuildingRecord blacksmith : buildings(data, BuildingType.BLACKSMITH)) {
            BlockPos work = blacksmith.workCenter();
            if (!level.hasChunkAt(work)) continue;
            for (ServerPlayer player : level.players()) {
                if (player.blockPosition().distSqr(work) > BLACKSMITH_RADIUS_SQR) continue;
                ItemStack damaged = mostDamagedEquippedItem(player);
                if (damaged.isEmpty()) continue;
                if (!SettlementStorageService.consumeMetal(level, data, 1L)) return;
                damaged.setDamageValue(Math.max(0, damaged.getDamageValue() - REPAIR_PER_METAL));
                changed = true;
            }
        }
        if (changed) {
            SettlementService.refreshResources(server, data);
            SettlementService.broadcast(server, data);
        }
    }

    private static ItemStack mostDamagedEquippedItem(ServerPlayer player) {
        ItemStack best = ItemStack.EMPTY;
        int bestDamage = 0;
        for (EquipmentSlot slot : REPAIR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.isDamageableItem()) continue;
            int damage = stack.getDamageValue();
            if (damage > bestDamage) {
                best = stack;
                bestDamage = damage;
            }
        }
        return best;
    }

    private static void maintainGuards(ServerLevel level, SettlementData data) {
        for (BuildingRecord post : buildings(data, BuildingType.GUARD_POST)) {
            BlockPos center = post.workCenter();
            if (!level.hasChunkAt(center)) continue;
            String identity = guardIdentity(post);
            AABB search = new AABB(center).inflate(16.0D, 8.0D, 16.0D);
            List<IronGolem> existing = level.getEntitiesOfClass(IronGolem.class, search,
                    guard -> guard.getCustomName() != null && identity.equals(guard.getCustomName().getString()));
            if (!existing.isEmpty()) continue;

            IronGolem guard = new IronGolem(EntityTypes.IRON_GOLEM, level);
            guard.setPos(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D);
            guard.setCustomName(Component.literal(identity));
            guard.setCustomNameVisible(false);
            guard.setPersistenceRequired();
            guard.setPlayerCreated(true);
            level.addFreshEntity(guard);
        }
    }

    private static void maintainWatchtowers(ServerLevel level, SettlementData data) {
        for (BuildingRecord tower : buildings(data, BuildingType.WATCHTOWER)) {
            BlockPos home = tower.localToWorld(3, 1, 5);
            if (!level.hasChunkAt(home)) continue;

            IronGolem guard = findWatchGuard(level, tower, home);
            if (guard == null) guard = spawnWatchGuard(level, tower, home);
            if (guard == null) continue;

            Monster threat = nearestWatchThreat(level, home);
            if (threat != null) {
                guard.setTarget(threat);
                continue;
            }

            if (guard.getTarget() != null) guard.setTarget(null);
            if (guard.distanceToSqr(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D) > WATCH_GUARD_HOME_RADIUS_SQR) {
                guard.getNavigation().moveTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D, 0.9D);
            }
        }
    }

    private static IronGolem findWatchGuard(ServerLevel level, BuildingRecord tower, BlockPos home) {
        String assignment = watchAssignment(tower);
        AABB search = new AABB(home).inflate(WATCH_GUARD_SEARCH_RADIUS, 20.0D, WATCH_GUARD_SEARCH_RADIUS);
        List<IronGolem> guards = level.getEntitiesOfClass(IronGolem.class, search,
                guard -> guard.entityTags().contains(WATCH_GUARD_TAG) && guard.entityTags().contains(assignment));
        return guards.isEmpty() ? null : guards.getFirst();
    }

    private static IronGolem spawnWatchGuard(ServerLevel level, BuildingRecord tower, BlockPos home) {
        if (!level.hasChunkAt(home)) return null;
        IronGolem guard = new IronGolem(EntityTypes.IRON_GOLEM, level);
        guard.setPos(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D);
        guard.setCustomName(Component.literal("감시 경비대 [" + tower.originX() + "," + tower.originZ() + "]"));
        guard.setCustomNameVisible(false);
        guard.setPersistenceRequired();
        guard.setPlayerCreated(true);
        guard.addTag(WATCH_GUARD_TAG);
        guard.addTag(watchAssignment(tower));
        return level.addFreshEntity(guard) ? guard : null;
    }

    private static Monster nearestWatchThreat(ServerLevel level, BlockPos home) {
        AABB area = new AABB(home).inflate(WATCHTOWER_ALERT_RADIUS, 16.0D, WATCHTOWER_ALERT_RADIUS);
        return level.getEntitiesOfClass(Monster.class, area,
                        monster -> monster.isAlive() && !(monster instanceof Creeper))
                .stream()
                .min(Comparator.comparingDouble(monster -> monster.distanceToSqr(
                        home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D)))
                .orElse(null);
    }

    private static String watchAssignment(BuildingRecord tower) {
        return WATCH_ASSIGNMENT_PREFIX + tower.originX() + "_" + tower.originZ();
    }

    private static String guardIdentity(BuildingRecord post) {
        return "개척 경비대 [" + post.originX() + "," + post.originZ() + "]";
    }

    private static List<BuildingRecord> buildings(SettlementData data, BuildingType type) {
        return data.buildings().stream().filter(building -> building.buildingType() == type).toList();
    }
}
