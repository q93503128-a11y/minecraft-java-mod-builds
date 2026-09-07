package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Comparator;
import java.util.List;

public final class SettlementBenefitService {
    public static final String GUARD_POST_GUARD_TAG = "frontier_settlement_guard_post_guard";
    public static final String WATCH_GUARD_TAG = "frontier_settlement_watch_guard";
    private static final String GUARD_POST_ASSIGNMENT_PREFIX = "frontier_settlement_guard_post_";
    private static final String WATCH_ASSIGNMENT_PREFIX = "frontier_settlement_watchtower_";
    private static final int GUARD_CHECK_INTERVAL_TICKS = 200;
    private static final int WATCHTOWER_CHECK_INTERVAL_TICKS = 100;
    private static final double WATCHTOWER_ALERT_RADIUS = 40.0D;
    private static final double CITADEL_WATCH_RADIUS_BONUS = 16.0D;
    private static final double GUARD_POST_SEARCH_RADIUS = 64.0D;
    private static final double GUARD_POST_HOME_RADIUS_SQR = 24.0D * 24.0D;
    private static final double WATCH_GUARD_SEARCH_RADIUS = 64.0D;
    private static final double WATCH_GUARD_HOME_RADIUS_SQR = 18.0D * 18.0D;

    private SettlementBenefitService() {}

    public static void tick(MinecraftServer server, SettlementData data) {
        int tick = server.getTickCount();
        if (tick % GUARD_CHECK_INTERVAL_TICKS == 0) maintainGuards(server.overworld(), data);
        if (tick % WATCHTOWER_CHECK_INTERVAL_TICKS == 0) maintainWatchtowers(server.overworld(), data);
    }

    /**
     * Blacksmith durability repair is deliberately player-directed instead of a proximity aura.
     * Sneak-right-click the completed settlement blacksmith's own anvil while holding one damaged
     * item. The repair consumes real metal from the shared physical storage network and repairs the
     * held stack in one transaction. Ordinary anvil use remains untouched when the player is not
     * sneaking.
     */
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isShiftKeyDown()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos clicked = event.getPos();
        if (!isAnvil(level, clicked)) return;

        ItemStack held = event.getItemStack();
        if (held.isEmpty() || !held.isDamageableItem() || held.getDamageValue() <= 0) return;

        MinecraftServer server = level.getServer();
        SettlementData data = SettlementData.get(server);
        if (!data.founded() || !isSettlementBlacksmithAnvil(data, clicked)) return;

        int repairPerMetal = Math.max(1, SettlementExplorationBenefitService.repairPerMetal(data));
        int damage = held.getDamageValue();
        long metalCost = Math.max(1L, (damage + (long) repairPerMetal - 1L) / repairPerMetal);

        if (!SettlementStorageService.storageAvailable(level, data)) {
            player.displayClientMessage(Component.literal("§6[마을] §f공동 저장소가 모두 로드되어야 대장간 수리를 사용할 수 있습니다."), true);
            finishRepairInteraction(event);
            return;
        }
        if (!SettlementStorageService.consumeMetal(level, data, metalCost)) {
            player.displayClientMessage(Component.literal("§6[마을] §f수리 금속이 부족합니다. 필요: " + metalCost), true);
            finishRepairInteraction(event);
            return;
        }

        held.setDamageValue(0);
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
        player.displayClientMessage(Component.literal("§6[마을] §f대장간 수리 완료 · 금속 " + metalCost + " 소비"), true);
        finishRepairInteraction(event);
    }

    private static boolean isSettlementBlacksmithAnvil(SettlementData data, BlockPos clicked) {
        for (BuildingRecord blacksmith : buildings(data, BuildingType.BLACKSMITH)) {
            if (blacksmith.localToWorld(2, 1, 2).equals(clicked)) return true;
        }
        return false;
    }

    private static boolean isAnvil(ServerLevel level, BlockPos pos) {
        var block = level.getBlockState(pos).getBlock();
        return block == Blocks.ANVIL || block == Blocks.CHIPPED_ANVIL || block == Blocks.DAMAGED_ANVIL;
    }

    private static void finishRepairInteraction(PlayerInteractEvent.RightClickBlock event) {
        event.setCancellationResult(InteractionResult.SUCCESS_SERVER);
        event.setCanceled(true);
    }

    private static void maintainGuards(ServerLevel level, SettlementData data) {
        for (BuildingRecord post : buildings(data, BuildingType.GUARD_POST)) {
            BlockPos center = post.workCenter();
            if (!level.hasChunkAt(center)) continue;
            String assignment = guardPostAssignment(post);
            String identity = guardIdentity(post);
            AABB search = new AABB(center).inflate(GUARD_POST_SEARCH_RADIUS, 24.0D, GUARD_POST_SEARCH_RADIUS);
            List<IronGolem> existing = level.getEntitiesOfClass(IronGolem.class, search,
                    guard -> (guard.entityTags().contains(GUARD_POST_GUARD_TAG)
                            && guard.entityTags().contains(assignment))
                            || (guard.getCustomName() != null && identity.equals(guard.getCustomName().getString())));
            existing.sort(Comparator.comparing(guard -> guard.getUUID().toString()));
            if (!existing.isEmpty()) {
                IronGolem active = existing.getFirst();
                active.addTag(GUARD_POST_GUARD_TAG);
                active.addTag(assignment);
                active.setNoAi(false);
                active.setInvulnerable(false);
                double homeDistance = active.distanceToSqr(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D);
                if (homeDistance > GUARD_POST_HOME_RADIUS_SQR) {
                    active.setTarget(null);
                    active.getNavigation().moveTo(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D, 0.9D);
                }
                for (int i = 1; i < existing.size(); i++) {
                    removeDuplicateCivicGuard(existing.get(i));
                }
                continue;
            }
            if (!entityAreaLoaded(level, search)) continue;

            IronGolem guard = new IronGolem(EntityTypes.IRON_GOLEM, level);
            guard.setPos(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D);
            guard.setCustomName(Component.literal(identity));
            guard.setCustomNameVisible(false);
            guard.setPersistenceRequired();
            guard.setPlayerCreated(true);
            guard.addTag(GUARD_POST_GUARD_TAG);
            guard.addTag(assignment);
            if (!level.addFreshEntity(guard)) continue;
        }
    }

    private static void maintainWatchtowers(ServerLevel level, SettlementData data) {
        for (BuildingRecord tower : buildings(data, BuildingType.WATCHTOWER)) {
            BlockPos home = tower.localToWorld(3, 1, 5);
            if (!level.hasChunkAt(home)) continue;

            IronGolem guard = findWatchGuard(level, tower, home);
            if (guard == null) guard = spawnWatchGuard(level, tower, home);
            if (guard == null) continue;

            double homeDistance = guard.distanceToSqr(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D);
            if (homeDistance > WATCH_GUARD_HOME_RADIUS_SQR) {
                guard.setTarget(null);
                guard.getNavigation().moveTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D, 0.9D);
                continue;
            }

            Monster threat = nearestWatchThreat(level, home, data);
            if (threat != null) {
                guard.setTarget(threat);
                continue;
            }
            if (guard.getTarget() != null) guard.setTarget(null);
        }
    }

    private static IronGolem findWatchGuard(ServerLevel level, BuildingRecord tower, BlockPos home) {
        String assignment = watchAssignment(tower);
        AABB search = watchGuardArea(home);
        List<IronGolem> guards = level.getEntitiesOfClass(IronGolem.class, search,
                guard -> guard.entityTags().contains(WATCH_GUARD_TAG) && guard.entityTags().contains(assignment));
        guards.sort(Comparator.comparing(guard -> guard.getUUID().toString()));
        if (guards.isEmpty()) return null;
        IronGolem active = guards.getFirst();
        active.setNoAi(false);
        active.setInvulnerable(false);
        for (int i = 1; i < guards.size(); i++) {
            removeDuplicateCivicGuard(guards.get(i));
        }
        return active;
    }

    private static IronGolem spawnWatchGuard(ServerLevel level, BuildingRecord tower, BlockPos home) {
        AABB search = watchGuardArea(home);
        if (!level.hasChunkAt(home) || !entityAreaLoaded(level, search)) return null;
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

    private static void removeDuplicateCivicGuard(IronGolem duplicate) {
        duplicate.setTarget(null);
        duplicate.getNavigation().stop();
        duplicate.setNoAi(false);
        duplicate.setInvulnerable(false);
        duplicate.discard();
    }

    private static AABB watchGuardArea(BlockPos home) {
        return new AABB(home).inflate(WATCH_GUARD_SEARCH_RADIUS, 20.0D, WATCH_GUARD_SEARCH_RADIUS);
    }

    private static boolean entityAreaLoaded(ServerLevel level, AABB area) {
        int minChunkX = Math.floorDiv((int) Math.floor(area.minX), 16);
        int maxChunkX = Math.floorDiv((int) Math.floor(Math.nextDown(area.maxX)), 16);
        int minChunkZ = Math.floorDiv((int) Math.floor(area.minZ), 16);
        int maxChunkZ = Math.floorDiv((int) Math.floor(Math.nextDown(area.maxZ)), 16);
        int probeY = (int) Math.floor((area.minY + area.maxY) * 0.5D);
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!level.hasChunkAt(new BlockPos(chunkX * 16 + 8, probeY, chunkZ * 16 + 8))) return false;
            }
        }
        return true;
    }

    private static Monster nearestWatchThreat(ServerLevel level, BlockPos home, SettlementData data) {
        double radius = WATCHTOWER_ALERT_RADIUS
                + (data.buildingCount(BuildingType.CITADEL) > 0 ? CITADEL_WATCH_RADIUS_BONUS : 0.0D);
        AABB area = new AABB(home).inflate(radius, 16.0D, radius);
        return level.getEntitiesOfClass(Monster.class, area,
                        monster -> monster.isAlive() && !(monster instanceof Creeper))
                .stream()
                .min(Comparator.comparingDouble(monster -> monster.distanceToSqr(
                        home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D)))
                .orElse(null);
    }

    private static String guardPostAssignment(BuildingRecord post) {
        return GUARD_POST_ASSIGNMENT_PREFIX + post.originX() + "_" + post.originZ();
    }

    private static String watchAssignment(BuildingRecord tower) {
        return WATCH_ASSIGNMENT_PREFIX + tower.originX() + "_" + tower.originZ();
    }

    private static String guardIdentity(BuildingRecord post) {
        return "개척 경비대 [" + post.originX() + "," + post.originZ() + "]";
    }

    /** Recreated civic guards are public-defense infrastructure, never a renewable iron-drop source. */
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof IronGolem guard)) return;
        if (!guard.entityTags().contains(GUARD_POST_GUARD_TAG)
                && !guard.entityTags().contains(WATCH_GUARD_TAG)) return;
        event.getDrops().clear();
    }

    private static List<BuildingRecord> buildings(SettlementData data, BuildingType type) {
        return data.buildings().stream().filter(building -> building.buildingType() == type).toList();
    }
}
