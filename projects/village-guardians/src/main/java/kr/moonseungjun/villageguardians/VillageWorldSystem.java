package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VillageWorldSystem {
    public static final int FORTRESS_RADIUS = 76;
    public static final int ENEMY_SPAWN_DISTANCE = 112;
    public static final int BATTLEFIELD_RADIUS = ENEMY_SPAWN_DISTANCE + 80;
    private static final int MIGRATION_CLEAN_RADIUS = FORTRESS_RADIUS + 24;
    private static final long RETURN_COOLDOWN_TICKS = 20L * 60L;
    private static final long COMBAT_RETURN_LOCK_TICKS = 20L * 10L;
    private static final Set<UUID> ALLOWED_GAME_MOBS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> RETURN_READY_AT = new HashMap<>();
    private static final Map<UUID, Long> LAST_COMBAT_AT = new HashMap<>();
    private static boolean generationInProgress;

    private VillageWorldSystem() {}

    public static synchronized void resetTransientState() {
        generationInProgress = false;
        ALLOWED_GAME_MOBS.clear();
        RETURN_READY_AT.clear();
        LAST_COMBAT_AT.clear();
    }

    public static synchronized void ensureFortifiedVillage(ServerPlayer player) {
        if (generationInProgress || !(player.level() instanceof ServerLevel level)) return;
        MinecraftServer server = level.getServer();
        if (server == null || level != server.overworld()) return;
        if (VillageCouncilState.villageCenter().isEmpty()) VillageCouncilState.setVillageCenter(player);
        BlockPos center = VillageCouncilState.villageCenter().orElse(player.blockPosition()).immutable();
        boolean firstBuild = !level.getBlockState(center.below(2)).is(Blocks.LODESTONE);
        boolean visualRevisionMissing = !level.getBlockState(center.below(4)).is(Blocks.RESPAWN_ANCHOR)
                || !level.getBlockState(center.below(5)).is(Blocks.AMETHYST_BLOCK)
                || !level.getBlockState(center.below(6)).is(Blocks.LAPIS_BLOCK)
                || !level.getBlockState(center.below(7)).is(Blocks.EMERALD_BLOCK)
                || !level.getBlockState(center.below(8)).is(Blocks.DIAMOND_BLOCK)
                || !level.getBlockState(center.below(9)).is(Blocks.GOLD_BLOCK);
        if (!firstBuild && !visualRevisionMissing) return;

        generationInProgress = true;
        try {
            if (firstBuild) {
                player.sendSystemMessage(Component.literal("§6[마을 건설] §f요새와 시설을 생성합니다."));
                VillageProgressionSystem.restoreFacilitiesForMigration();
            } else {
                player.sendSystemMessage(Component.literal(
                        "§6[마을 정비] §f성벽 4면 접근 계단·사격구·포좌 동선을 최신 실전 배치로 갱신합니다."));
            }
            buildAll(level, center);
            if (!firstBuild) {
                for (VillageProgressionSystem.Building building : VillageProgressionSystem.Building.values()) {
                    if (!VillageProgressionSystem.isOperational(building)) destroyStructure(level, building);
                }
            } else {
                removeLooseDebris(level, center);
            }
            VillageSiegeSegmentSystem.restoreAllVisuals(level);
            VillagePlacedTurretSystem.initializeServer(server);
            purgeUnauthorizedVillageMobs(server);
            player.sendSystemMessage(Component.literal(
                    "§a[마을 준비 완료] §f시설과 성벽 4면 접근로·상부 포좌·방어탑이 최신 상태로 적용됐습니다."));
        } finally {
            generationInProgress = false;
        }
    }

    public static synchronized void forceRebuild(MinecraftServer server) {
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null || generationInProgress) return;
        generationInProgress = true;
        try {
            ServerLevel level = server.overworld();
            buildAll(level, center);
            for (VillageProgressionSystem.Building building : VillageProgressionSystem.Building.values()) {
                if (!VillageProgressionSystem.isOperational(building)) destroyStructure(level, building);
            }
            // A retry/new-game rebuild first restores the base fortress, then projects the authoritative
            // phase-2 segment damage and placed turret state back into the world. This prevents a failed
            // night's visual/turret damage from leaking into a same-day retry.
            VillageSiegeSegmentSystem.restoreAllVisuals(level);
            VillagePlacedTurretSystem.initializeServer(server);
            purgeUnauthorizedVillageMobs(server);
        } finally {
            generationInProgress = false;
        }
    }

    public static boolean handleCentralBellInteraction(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) return false;
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null || !VillageFortressTerrain.isCentralBell(center, event.getPos())
                || !level.getBlockState(event.getPos()).is(Blocks.BELL)) return false;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        player.sendSystemMessage(Component.literal(VillageCouncilState.proposeAdvanceTime(player)));
        return true;
    }

    public static boolean handleGateInteraction(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) return false;
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null || !VillageFortressTerrain.isGateControl(center, event.getPos())) return false;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!VillageProgressionSystem.isOperational(VillageProgressionSystem.Building.WALLS)) {
            player.sendSystemMessage(Component.literal("§c북문이 파괴되어 개폐 장치를 사용할 수 없습니다."));
            return true;
        }
        if (VillageFortressTerrain.isNorthGatePassable(level, center)) {
            VillageFortressTerrain.closeNorthGate(level, center);
            player.sendSystemMessage(Component.literal("§6[북문] §f성문을 닫았습니다."));
        } else {
            VillageFortressTerrain.openNorthGate(level, center);
            player.sendSystemMessage(Component.literal("§6[북문] §f성문을 열었습니다."));
        }
        return true;
    }

    public static synchronized void recordCombat(LivingIncomingDamageEvent event) {
        long gameTime = event.getEntity().level().getGameTime();
        if (event.getEntity() instanceof ServerPlayer defender) LAST_COMBAT_AT.put(defender.getUUID(), gameTime);
        Entity source = event.getSource().getEntity();
        if (source instanceof ServerPlayer attacker) LAST_COMBAT_AT.put(attacker.getUUID(), gameTime);
    }

    public static synchronized String returnToVillage(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (server == null || center == null) return "마을 중심이 아직 설정되지 않았습니다.";
        if (!player.isAlive() || player.isSpectator()) return "현재 상태에서는 귀환할 수 없습니다.";
        long now = player.level().getGameTime();
        long combatReadyAt = LAST_COMBAT_AT.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2L) + COMBAT_RETURN_LOCK_TICKS;
        if (combatReadyAt > now) return "전투 중에는 귀환할 수 없습니다. "
                + Math.max(1L, (combatReadyAt - now + 19L) / 20L) + "초 뒤 다시 시도하세요.";
        long readyAt = RETURN_READY_AT.getOrDefault(player.getUUID(), 0L);
        if (readyAt > now) return "귀환 재사용 대기시간이 "
                + Math.max(1L, (readyAt - now + 19L) / 20L) + "초 남았습니다.";
        ServerLevel destination = server.overworld();
        BlockPos target = findSafeReturnPosition(destination, center);
        if (target == null) return "마을 광장에서 안전한 귀환 위치를 찾지 못했습니다.";
        player.teleportTo(destination, target.getX() + 0.5, target.getY(), target.getZ() + 0.5,
                Set.of(), player.getYRot(), player.getXRot(), true);
        RETURN_READY_AT.put(player.getUUID(), now + RETURN_COOLDOWN_TICKS);
        return "마을 중앙 광장으로 귀환했습니다. 재사용 대기시간은 60초입니다.";
    }

    public static boolean isAllowedGameMob(Mob mob) { return ALLOWED_GAME_MOBS.contains(mob.getUUID()); }
    public static void markAllowedGameMob(Mob mob) { ALLOWED_GAME_MOBS.add(mob.getUUID()); }
    public static void unmarkAllowedGameMob(UUID uuid) { ALLOWED_GAME_MOBS.remove(uuid); }

    public static boolean isInsideVillageArea(BlockPos pos) {
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        return center != null && horizontalDistanceSquared(pos, center)
                <= (long) VillageCouncilState.VILLAGE_RADIUS * VillageCouncilState.VILLAGE_RADIUS;
    }

    public static boolean isInsideBattlefield(BlockPos pos) {
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        return center != null && horizontalDistanceSquared(pos, center)
                <= (long) BATTLEFIELD_RADIUS * BATTLEFIELD_RADIUS;
    }

    public static void purgeDaytimeHostiles(MinecraftServer server) {
        if (server == null || VillageCouncilState.currentPhase() != VillageTimePhase.DAY) return;
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) return;
        ServerLevel level = server.overworld();
        AABB area = new AABB(center).inflate(BATTLEFIELD_RADIUS, 96, BATTLEFIELD_RADIUS);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area)) {
            if (mob.getType().getCategory() == MobCategory.MONSTER && !isAllowedGameMob(mob)) mob.discard();
        }
    }

    public static void purgeUnauthorizedVillageMobs(MinecraftServer server) {
        if (server == null) return;
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) return;
        ServerLevel level = server.overworld();
        AABB area = new AABB(center).inflate(BATTLEFIELD_RADIUS, 96, BATTLEFIELD_RADIUS);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area)) {
            if (!isAllowedGameMob(mob) && !mob.isPersistenceRequired()) mob.discard();
        }
    }

    public static BlockPos northGateTarget() {
        return VillageCouncilState.villageCenter().orElse(new BlockPos(0, 0, 0))
                .offset(0, 0, -FORTRESS_RADIUS - 3);
    }

    public static BlockPos northInnerApproach() {
        return VillageCouncilState.villageCenter().orElse(new BlockPos(0, 0, 0))
                .offset(0, 0, -FORTRESS_RADIUS + 14);
    }

    public static BlockPos northSpawnOrigin() {
        return VillageCouncilState.villageCenter().orElse(new BlockPos(0, 0, 0))
                .offset(0, 0, -ENEMY_SPAWN_DISTANCE);
    }

    public static boolean isNorthGatePassable(ServerLevel level) {
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        return center != null && VillageFortressTerrain.isNorthGatePassable(level, center);
    }

    public static BlockPos buildingCenter(VillageProgressionSystem.Building building) {
        return VillageFortressBuildings.center(
                VillageCouncilState.villageCenter().orElse(new BlockPos(0, 0, 0)), building);
    }

    public static void destroyStructure(ServerLevel level, VillageProgressionSystem.Building building) {
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) return;
        VillageBuildingSignatures.remove(level, center, building);
        if (building == VillageProgressionSystem.Building.WALLS) VillageFortressTerrain.destroyNorthGate(level, center);
        else VillageFortressBuildings.remove(level, center, building);
    }

    public static void rebuildStructure(ServerLevel level, VillageProgressionSystem.Building building) {
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) return;
        if (building == VillageProgressionSystem.Building.WALLS) {
            VillageFortressTerrain.rebuildNorthGate(level, center);
            VillageBuildingEnhancements.reinforceWallRailings(level, center);
            VillageDefenseTowerBuilder.build(level, center);
        } else {
            VillageFortressBuildings.rebuild(level, center, building);
        }
        VillageBuildingSignatures.build(level, center, building);
    }

    public static void applyUpgradeVisual(ServerLevel level, VillageProgressionSystem.Building building, int levelValue) {
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) return;
        VillageFortressBuildings.applyUpgradeVisual(level, center, building, levelValue);
        if (VillageProgressionSystem.isOperational(building)) {
            VillageBuildingSignatures.build(level, center, building);
        }
    }

    private static void buildAll(ServerLevel level, BlockPos center) {
        VillageFortressTerrain.buildBase(level, center);
        VillageBuildingEnhancements.reinforceWallRailings(level, center);
        VillageFortressBuildings.buildAll(level, center);
        VillageDefenseTowerBuilder.build(level, center);
        VillageBuildingSignatures.buildAll(level, center);
        VillageFortressTerrain.restoreCentralBell(level, center);
        // 26.2 exposes Blocks.COPPER_BLOCK as a weathering collection, so the migration marker uses a stable block.
        VillageFortressTerrain.set(level, center.below(9), Blocks.GOLD_BLOCK);
        VillageFortressTerrain.set(level, center.below(8), Blocks.DIAMOND_BLOCK);
        VillageFortressTerrain.set(level, center.below(7), Blocks.EMERALD_BLOCK);
        VillageFortressTerrain.set(level, center.below(6), Blocks.LAPIS_BLOCK);
        VillageFortressTerrain.set(level, center.below(5), Blocks.AMETHYST_BLOCK);
        VillageFortressTerrain.set(level, center.below(4), Blocks.RESPAWN_ANCHOR);
        VillageFortressTerrain.set(level, center.below(3), Blocks.CRYING_OBSIDIAN);
        VillageFortressTerrain.set(level, center.below(2), Blocks.LODESTONE);
        VillageFortressTerrain.set(level, center.below(), Blocks.CHISELED_STONE_BRICKS);
    }

    private static BlockPos findSafeReturnPosition(ServerLevel level, BlockPos center) {
        for (int z = 12; z <= 20; z++) {
            for (int x = -3; x <= 3; x++) {
                BlockPos candidate = center.offset(x, 0, z);
                if (level.getBlockState(candidate).isAir() && level.getBlockState(candidate.above()).isAir()
                        && !level.getBlockState(candidate.below()).isAir()) return candidate;
            }
        }
        return null;
    }

    private static void removeLooseDebris(ServerLevel level, BlockPos center) {
        AABB area = new AABB(center).inflate(MIGRATION_CLEAN_RADIUS, 64, MIGRATION_CLEAN_RADIUS);
        level.getEntitiesOfClass(ItemEntity.class, area).forEach(ItemEntity::discard);
        level.getEntitiesOfClass(ExperienceOrb.class, area).forEach(ExperienceOrb::discard);
    }

    private static long horizontalDistanceSquared(BlockPos first, BlockPos second) {
        long dx = (long) first.getX() - second.getX();
        long dz = (long) first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }
}
