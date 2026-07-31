package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VillageWorldSystem {
    public static final int FORTRESS_RADIUS = 76;
    public static final int ENEMY_SPAWN_DISTANCE = 112;
    private static final int CLEAN_RADIUS = 138;
    private static final Set<UUID> ALLOWED_GAME_MOBS = ConcurrentHashMap.newKeySet();
    private static boolean generationInProgress;

    private VillageWorldSystem() {
    }

    public static synchronized void resetTransientState() {
        generationInProgress = false;
        ALLOWED_GAME_MOBS.clear();
    }

    public static synchronized void ensureFortifiedVillage(ServerPlayer player) {
        if (generationInProgress || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        MinecraftServer server = level.getServer();
        if (server == null || level != server.overworld()) {
            return;
        }
        if (VillageCouncilState.villageCenter().isEmpty()) {
            VillageCouncilState.setVillageCenter(player);
        }
        BlockPos center = VillageCouncilState.villageCenter().orElse(player.blockPosition()).immutable();
        if (!level.getBlockState(center.below(2)).is(Blocks.NETHERITE_BLOCK)) {
            generationInProgress = true;
            try {
                player.sendSystemMessage(Component.literal(
                        "§6[마을 재건] §f넓어진 단일 북문 전투 마을과 기능별 건물을 배치합니다."));
                buildAll(level, center);
                for (VillageProgressionSystem.Building building : VillageProgressionSystem.Building.values()) {
                    if (!VillageProgressionSystem.isOperational(building)) {
                        destroyStructure(level, building);
                    }
                }
                player.sendSystemMessage(Component.literal(
                        "§a[마을 준비 완료] §f중앙 종, 건물 간판, 호출기 UI를 이용하세요."));
            } finally {
                generationInProgress = false;
            }
        }
        removeUnauthorizedMobs(level, center);
    }

    public static synchronized void forceRebuild(MinecraftServer server) {
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null || generationInProgress) {
            return;
        }
        generationInProgress = true;
        try {
            buildAll(server.overworld(), center);
            for (VillageProgressionSystem.Building building : VillageProgressionSystem.Building.values()) {
                if (!VillageProgressionSystem.isOperational(building)) {
                    destroyStructure(server.overworld(), building);
                }
            }
        } finally {
            generationInProgress = false;
        }
    }

    public static boolean isAllowedGameMob(Mob mob) {
        return ALLOWED_GAME_MOBS.contains(mob.getUUID());
    }

    public static void markAllowedGameMob(Mob mob) {
        ALLOWED_GAME_MOBS.add(mob.getUUID());
    }

    public static boolean isInsideVillageArea(BlockPos pos) {
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) {
            return false;
        }
        long dx = (long) pos.getX() - center.getX();
        long dz = (long) pos.getZ() - center.getZ();
        return dx * dx + dz * dz <= (long) VillageCouncilState.VILLAGE_RADIUS * VillageCouncilState.VILLAGE_RADIUS;
    }

    public static BlockPos northGateTarget() {
        return VillageCouncilState.villageCenter().orElse(new BlockPos(0, 0, 0))
                .offset(0, 0, -FORTRESS_RADIUS + 3);
    }

    public static BlockPos northInnerApproach() {
        return VillageCouncilState.villageCenter().orElse(new BlockPos(0, 0, 0))
                .offset(0, 0, -FORTRESS_RADIUS + 12);
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
                VillageCouncilState.villageCenter().orElse(new BlockPos(0, 0, 0)),
                building);
    }

    public static void destroyStructure(ServerLevel level, VillageProgressionSystem.Building building) {
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) {
            return;
        }
        if (building == VillageProgressionSystem.Building.WALLS) {
            VillageFortressTerrain.destroyNorthGate(level, center);
        } else {
            VillageFortressBuildings.remove(level, center, building);
        }
    }

    public static void rebuildStructure(ServerLevel level, VillageProgressionSystem.Building building) {
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) {
            return;
        }
        if (building == VillageProgressionSystem.Building.WALLS) {
            VillageFortressTerrain.rebuildNorthGate(level, center);
        } else {
            VillageFortressBuildings.rebuild(level, center, building);
        }
    }

    public static void applyUpgradeVisual(
            ServerLevel level,
            VillageProgressionSystem.Building building,
            int levelValue) {
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center != null) {
            VillageFortressBuildings.applyUpgradeVisual(level, center, building, levelValue);
        }
    }

    private static void buildAll(ServerLevel level, BlockPos center) {
        VillageFortressTerrain.buildBase(level, center);
        VillageFortressBuildings.buildAll(level, center);
        for (VillageProgressionSystem.Building building : VillageProgressionSystem.Building.values()) {
            applyUpgradeVisual(level, building, VillageProgressionSystem.level(building));
        }
        VillageFortressTerrain.set(level, center.below(2), Blocks.NETHERITE_BLOCK);
        VillageFortressTerrain.set(level, center.below(), Blocks.CHISELED_STONE_BRICKS);
    }

    private static void removeUnauthorizedMobs(ServerLevel level, BlockPos center) {
        AABB area = new AABB(center).inflate(CLEAN_RADIUS, 42, CLEAN_RADIUS);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area)) {
            if (!isAllowedGameMob(mob)) {
                mob.discard();
            }
        }
    }
}
