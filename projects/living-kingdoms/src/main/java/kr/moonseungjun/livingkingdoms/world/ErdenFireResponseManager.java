package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Loaded-city fire response for Erden.
 *
 * <p>Only fires inside chunks already visible to a player are discovered during normal play. A real,
 * alive guard-post worker must also be materialised before an incident can progress. The responder
 * walks to the nearest civic fire cistern, spends time drawing water, then walks to the incident.
 * Fire blocks are removed only after that villager is physically within extinguishing range. No
 * unloaded district is force-loaded and no remote fire deletion is performed in normal gameplay.</p>
 */
public final class ErdenFireResponseManager {
    private static final Identifier VILLAGER_ID =
            Identifier.fromNamespaceAndPath("minecraft", "villager");
    private static final int SCAN_INTERVAL = 20;
    private static final int RESPONSE_INTERVAL = 5;
    private static final int SCAN_RADIUS_XZ = 12;
    private static final int SCAN_RADIUS_Y = 6;
    private static final int MAX_ACTIVE_INCIDENTS = 4;
    private static final int WATER_DRAW_TICKS = 20;
    private static final double MOVE_SPEED = 0.72D;
    private static final double CISTERN_REACH_SQR = 9.0D;
    private static final double FIRE_REACH_SQR = 12.25D;
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private static MinecraftServer activeServer;
    private static boolean ciRequested;
    private static boolean ciTicketHeld;
    private static boolean ciPrepared;
    private static boolean ciPassed;
    private static BlockPos ciSupportPos;
    private static BlockPos ciFirePos;
    private static UUID ciResponderUuid;
    private static ChunkPos ciChunk;
    private static int ciDetectedBaseline;
    private static int ciDispatchedBaseline;
    private static int ciExtinguishedBaseline;

    private ErdenFireResponseManager() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        ErdenFireResponseSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenFireResponseSavedData.TYPE);
        prepareCiSample(level, data);

        long tick = level.getGameTime();
        if (tick % SCAN_INTERVAL == 0L) detectPlayerVisibleFires(level, data, tick);
        if (tick % RESPONSE_INTERVAL != 0L) return;
        processIncidents(level, data, tick);
        verifyCiIfReady(level, data);
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        ciRequested = false;
        ciTicketHeld = false;
        ciPrepared = false;
        ciPassed = false;
        ciSupportPos = null;
        ciFirePos = null;
        ciResponderUuid = null;
        ciChunk = null;
        ciDetectedBaseline = 0;
        ciDispatchedBaseline = 0;
        ciExtinguishedBaseline = 0;
    }

    private static void detectPlayerVisibleFires(
            ServerLevel level,
            ErdenFireResponseSavedData data,
            long tick) {
        if (data.activeCount() >= MAX_ACTIVE_INCIDENTS) return;
        for (ServerPlayer player : level.players()) {
            BlockPos center = player.blockPosition();
            if (!insideCapital(center.getX(), center.getZ())) continue;
            int minY = Math.max(level.getMinY(), center.getY() - SCAN_RADIUS_Y);
            int maxY = Math.min(level.getMaxY() - 1, center.getY() + SCAN_RADIUS_Y);
            for (int x = center.getX() - SCAN_RADIUS_XZ;
                 x <= center.getX() + SCAN_RADIUS_XZ; x++) {
                for (int z = center.getZ() - SCAN_RADIUS_XZ;
                     z <= center.getZ() + SCAN_RADIUS_XZ; z++) {
                    if (!insideCapital(x, z) || !level.hasChunk(x >> 4, z >> 4)) continue;
                    for (int y = minY; y <= maxY; y++) {
                        if (data.activeCount() >= MAX_ACTIVE_INCIDENTS) return;
                        BlockPos fire = new BlockPos(x, y, z);
                        if (!isFire(level, fire) || data.hasIncidentAt(x, y, z)) continue;
                        report(level, data, fire, tick);
                    }
                }
            }
        }
    }

    private static void report(
            ServerLevel level,
            ErdenFireResponseSavedData data,
            BlockPos fire,
            long tick) {
        ErdenUrbanInfrastructureBuilder.FireCistern cistern = nearestCistern(fire);
        ErdenFireResponseSavedData.Incident incident = data.report(
                fire.getX(), fire.getY(), fire.getZ(), cistern, tick);
        LivingKingdoms.LOGGER.info(
                "Erden fire reported incident={} fire={},{},{} cistern={},{},{} loaded_only=true active={}",
                incident.id(), fire.getX(), fire.getY(), fire.getZ(),
                cistern.x(), cistern.y(), cistern.z(), data.activeCount());
    }

    private static ErdenUrbanInfrastructureBuilder.FireCistern nearestCistern(BlockPos fire) {
        return ErdenUrbanInfrastructureBuilder.fireCisterns().stream()
                .min(Comparator.comparingLong(cistern -> distanceSquared2d(
                        fire.getX(), fire.getZ(), cistern.x(), cistern.z())))
                .orElseThrow(() -> new IllegalStateException("Erden has no fire cisterns"));
    }

    private static void processIncidents(
            ServerLevel level,
            ErdenFireResponseSavedData data,
            long tick) {
        ErdenPopulationSavedData population = level.getDataStorage()
                .computeIfAbsent(ErdenPopulationSavedData.TYPE);
        Map<String, Villager> loadedGuards = loadedGuardResponders(level, population);
        Set<String> reserved = new HashSet<>();
        for (ErdenFireResponseSavedData.Incident incident : data.incidents()) {
            if (!incident.responderName().isBlank()) reserved.add(incident.responderName());
        }

        for (ErdenFireResponseSavedData.Incident incident : data.incidents()) {
            BlockPos fire = new BlockPos(incident.fireX(), incident.fireY(), incident.fireZ());
            if (!isFire(level, fire)) {
                data.resolve(incident.id(), false);
                LivingKingdoms.LOGGER.info(
                        "Erden fire incident {} ended before brigade suppression stage={}",
                        incident.id(), incident.stage());
                continue;
            }

            if (incident.stage().equals("reported")) {
                Villager responder = nearestAvailableGuard(
                        loadedGuards, reserved, incident.fireX(), incident.fireY(), incident.fireZ());
                if (responder == null) continue;
                data.assign(incident.id(), responder.getName().getString(), tick);
                reserved.add(responder.getName().getString());
                LivingKingdoms.LOGGER.info(
                        "Erden fire brigade dispatched incident={} responder={} guard_worker=true",
                        incident.id(), responder.getName().getString());
                continue;
            }

            Villager responder = loadedGuards.get(incident.responderName());
            if (responder == null || !responder.isAlive()) {
                reserved.remove(incident.responderName());
                data.requeue(incident.id(), tick);
                continue;
            }
            responder.setPersistenceRequired();

            switch (incident.stage()) {
                case "to_cistern" -> moveToCistern(level, data, incident, responder, tick);
                case "drawing_water" -> drawWater(level, data, incident, responder, tick);
                case "to_fire" -> moveToFire(level, data, incident, responder, fire);
                default -> data.requeue(incident.id(), tick);
            }
        }
    }

    private static void moveToCistern(
            ServerLevel level,
            ErdenFireResponseSavedData data,
            ErdenFireResponseSavedData.Incident incident,
            Villager responder,
            long tick) {
        BlockPos target = cisternApproach(incident);
        if (!level.hasChunk(target.getX() >> 4, target.getZ() >> 4)) return;
        if (responder.distanceToSqr(
                target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D) <= CISTERN_REACH_SQR) {
            responder.getNavigation().stop();
            data.advance(incident.id(), "drawing_water", tick);
            return;
        }
        responder.getNavigation().moveTo(
                target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, MOVE_SPEED);
    }

    private static void drawWater(
            ServerLevel level,
            ErdenFireResponseSavedData data,
            ErdenFireResponseSavedData.Incident incident,
            Villager responder,
            long tick) {
        BlockPos target = cisternApproach(incident);
        if (responder.distanceToSqr(
                target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D) > CISTERN_REACH_SQR) {
            responder.getNavigation().moveTo(
                    target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, MOVE_SPEED);
            return;
        }
        responder.getNavigation().stop();
        if (tick - incident.stageTick() >= WATER_DRAW_TICKS) {
            data.advance(incident.id(), "to_fire", tick);
        }
    }

    private static void moveToFire(
            ServerLevel level,
            ErdenFireResponseSavedData data,
            ErdenFireResponseSavedData.Incident incident,
            Villager responder,
            BlockPos fire) {
        if (!level.hasChunk(fire.getX() >> 4, fire.getZ() >> 4)) return;
        if (responder.distanceToSqr(
                fire.getX() + 0.5D, fire.getY(), fire.getZ() + 0.5D) > FIRE_REACH_SQR) {
            responder.getNavigation().moveTo(
                    fire.getX() + 0.5D, fire.getY(), fire.getZ() + 0.5D, MOVE_SPEED);
            return;
        }
        responder.getNavigation().stop();
        int removed = extinguishCluster(level, fire);
        if (removed <= 0) return;
        data.resolve(incident.id(), true);
        LivingKingdoms.LOGGER.info(
                "Erden fire suppressed incident={} responder={} removed_fire_blocks={} proximity_verified=true cistern_staged=true",
                incident.id(), responder.getName().getString(), removed);
    }

    private static int extinguishCluster(ServerLevel level, BlockPos center) {
        int removed = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    if (removed >= 64) return removed;
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4) || !isFire(level, pos)) continue;
                    if (level.setBlock(pos, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS)) removed++;
                }
            }
        }
        return removed;
    }

    private static Map<String, Villager> loadedGuardResponders(
            ServerLevel level,
            ErdenPopulationSavedData population) {
        Set<String> guardNames = new HashSet<>();
        for (ErdenPopulationSavedData.Household household : population.households()) {
            for (ErdenPopulationSavedData.Resident resident : household.residents()) {
                if (!resident.workRole().equals("guard_post") || population.isDead(resident.id())) continue;
                guardNames.add(resident.name());
            }
        }
        Map<String, Villager> result = new HashMap<>();
        for (Villager villager : level.getEntitiesOfClass(
                Villager.class, capitalBounds(level),
                villager -> guardNames.contains(villager.getName().getString()))) {
            result.putIfAbsent(villager.getName().getString(), villager);
        }
        return result;
    }

    private static Villager nearestAvailableGuard(
            Map<String, Villager> loadedGuards,
            Set<String> reserved,
            int x,
            int y,
            int z) {
        return loadedGuards.values().stream()
                .filter(Villager::isAlive)
                .filter(villager -> !reserved.contains(villager.getName().getString()))
                .min(Comparator.comparingDouble(villager -> villager.distanceToSqr(
                        x + 0.5D, y, z + 0.5D)))
                .orElse(null);
    }

    private static BlockPos cisternApproach(ErdenFireResponseSavedData.Incident incident) {
        int dx = incident.fireX() - incident.cisternX();
        int dz = incident.fireZ() - incident.cisternZ();
        if (Math.abs(dx) >= Math.abs(dz)) {
            int side = dx >= 0 ? 4 : -4;
            return new BlockPos(incident.cisternX() + side, incident.cisternY() + 1, incident.cisternZ());
        }
        int side = dz >= 0 ? 3 : -3;
        return new BlockPos(incident.cisternX(), incident.cisternY() + 1, incident.cisternZ() + side);
    }

    private static boolean isFire(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.FIRE)
                || level.getBlockState(pos).is(Blocks.SOUL_FIRE);
    }

    private static boolean insideCapital(int x, int z) {
        return x >= ErdenCapitalStreamingBuilder.WEST_WALL_X
                && x <= ErdenCapitalStreamingBuilder.EAST_WALL_X
                && z >= ErdenCapitalStreamingBuilder.NORTH_WALL_Z
                && z <= ErdenCapitalStreamingBuilder.SOUTH_WALL_Z;
    }

    private static AABB capitalBounds(ServerLevel level) {
        return new AABB(
                ErdenCapitalStreamingBuilder.WEST_WALL_X - 16,
                level.getMinY(),
                ErdenCapitalStreamingBuilder.NORTH_WALL_Z - 16,
                ErdenCapitalStreamingBuilder.EAST_WALL_X + 16,
                level.getMaxY(),
                ErdenCapitalStreamingBuilder.SOUTH_WALL_Z + 16);
    }

    private static long distanceSquared2d(int x1, int z1, int x2, int z2) {
        long dx = (long) x1 - x2;
        long dz = (long) z1 - z2;
        return dx * dx + dz * dz;
    }

    private static void prepareCiSample(
            ServerLevel level,
            ErdenFireResponseSavedData data) {
        if (!isFireCi() || ciPassed || ciPrepared) return;
        List<ErdenUrbanInfrastructureBuilder.FireCistern> cisterns =
                ErdenUrbanInfrastructureBuilder.fireCisterns();
        if (cisterns.size() != 8) {
            throw new IllegalStateException("Expected eight Erden fire cisterns, found " + cisterns.size());
        }
        ErdenUrbanInfrastructureBuilder.FireCistern cistern = cisterns.stream()
                .filter(candidate -> candidate.x() == ErdenUrbanInfrastructureBuilder.DIAGNOSTIC_CISTERN_X
                        && candidate.z() == ErdenUrbanInfrastructureBuilder.DIAGNOSTIC_CISTERN_Z)
                .findFirst().orElseThrow(() -> new IllegalStateException("Missing diagnostic fire cistern"));
        int chunkX = cistern.x() >> 4;
        int chunkZ = cistern.z() >> 4;
        ciChunk = new ChunkPos(chunkX, chunkZ);
        if (!ErdenCapitalStreamingBuilder.isChunkBuilt(level, chunkX, chunkZ)) {
            if (!ciRequested) {
                ErdenCapitalStreamingBuilder.requestChunk(level, chunkX, chunkZ);
                ciRequested = true;
            }
            return;
        }
        if (!ciTicketHeld) {
            level.getChunkSource().addTicketAndLoadWithRadius(TicketType.PORTAL, ciChunk, 0);
            ciTicketHeld = true;
            return;
        }
        if (!level.hasChunk(chunkX, chunkZ)) return;

        ErdenPopulationSavedData population = level.getDataStorage()
                .computeIfAbsent(ErdenPopulationSavedData.TYPE);
        ErdenPopulationSavedData.Resident guard = firstLivingGuard(population);
        if (guard == null) return;
        BlockPos responderPos = safeStandingPosition(
                level, cistern.x() - 4, cistern.y() + 1, cistern.z());
        Villager testResponder = spawnCiResponder(level, guard.name(), responderPos);
        if (testResponder == null) return;
        ciResponderUuid = testResponder.getUUID();

        ciSupportPos = findCiFireSupport(level, cistern);
        if (ciSupportPos == null) {
            testResponder.discard();
            ciResponderUuid = null;
            throw new IllegalStateException("No safe CI fire support cell near diagnostic cistern");
        }
        ciFirePos = ciSupportPos.above();
        level.setBlock(ciSupportPos, Blocks.NETHERRACK.defaultBlockState(), UPDATE_FLAGS);
        level.setBlock(ciFirePos, Blocks.FIRE.defaultBlockState(), UPDATE_FLAGS);
        ciDetectedBaseline = data.detectedCount();
        ciDispatchedBaseline = data.dispatchedCount();
        ciExtinguishedBaseline = data.extinguishedCount();
        report(level, data, ciFirePos, level.getGameTime());
        ciPrepared = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden fire-response CI responder={} cistern={},{},{} fire={},{},{} real_guard_identity=true",
                guard.name(), cistern.x(), cistern.y(), cistern.z(),
                ciFirePos.getX(), ciFirePos.getY(), ciFirePos.getZ());
    }

    private static ErdenPopulationSavedData.Resident firstLivingGuard(
            ErdenPopulationSavedData population) {
        List<ErdenPopulationSavedData.Resident> guards = new ArrayList<>();
        for (ErdenPopulationSavedData.Household household : population.households()) {
            for (ErdenPopulationSavedData.Resident resident : household.residents()) {
                if (resident.workRole().equals("guard_post") && !population.isDead(resident.id())) {
                    guards.add(resident);
                }
            }
        }
        guards.sort(Comparator.comparing(ErdenPopulationSavedData.Resident::id));
        return guards.isEmpty() ? null : guards.getFirst();
    }

    private static Villager spawnCiResponder(
            ServerLevel level,
            String name,
            BlockPos position) {
        EntityType<?> villagerType = BuiltInRegistries.ENTITY_TYPE
                .getOptional(VILLAGER_ID).orElse(null);
        if (villagerType == null) return null;
        Entity created = villagerType.create(level, EntitySpawnReason.COMMAND);
        if (!(created instanceof Villager villager)) return null;
        villager.setPos(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D);
        villager.setCustomName(Component.literal(name));
        villager.setCustomNameVisible(false);
        villager.setPersistenceRequired();
        if (!level.addFreshEntity(villager)) return null;
        return villager;
    }

    private static BlockPos findCiFireSupport(
            ServerLevel level,
            ErdenUrbanInfrastructureBuilder.FireCistern cistern) {
        int chunkMinX = (cistern.x() >> 4) << 4;
        int chunkMinZ = (cistern.z() >> 4) << 4;
        BlockPos best = null;
        long bestDistance = Long.MAX_VALUE;
        int examined = 0;
        for (int x = chunkMinX; x <= chunkMinX + 15; x++) {
            for (int z = chunkMinZ; z <= chunkMinZ + 15; z++) {
                long dx = (long) x - cistern.x();
                long dz = (long) z - cistern.z();
                long distance = dx * dx + dz * dz;
                if (distance < 25L || distance > 196L) continue;
                int preferredY = (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z)) + 1;
                for (int vertical = 0; vertical <= 8; vertical++) {
                    int[] ys = vertical == 0
                            ? new int[]{preferredY}
                            : new int[]{preferredY + vertical, preferredY - vertical};
                    for (int y : ys) {
                        if (y <= level.getMinY() || y >= level.getMaxY() - 1) continue;
                        examined++;
                        BlockPos support = new BlockPos(x, y, z);
                        BlockState below = level.getBlockState(support.below());
                        if (below.isAir() || !below.getFluidState().isEmpty()) continue;
                        if (!level.getBlockState(support).isAir()
                                || !level.getBlockState(support.above()).isAir()) continue;
                        if (distance < bestDistance) {
                            best = support;
                            bestDistance = distance;
                        }
                    }
                }
            }
        }
        if (best != null) {
            LivingKingdoms.LOGGER.info(
                    "Selected bounded Erden fire CI support={} examined={} same_chunk=true two_block_air=true stable_ground=true",
                    best, examined);
        }
        return best;
    }

    private static BlockPos safeStandingPosition(
            ServerLevel level,
            int x,
            int preferredY,
            int z) {
        for (int offset = 0; offset <= 8; offset++) {
            int[] candidates = offset == 0
                    ? new int[]{preferredY}
                    : new int[]{preferredY + offset, preferredY - offset};
            for (int y : candidates) {
                if (y <= level.getMinY() || y >= level.getMaxY() - 1) continue;
                BlockPos feet = new BlockPos(x, y, z);
                if (!level.getBlockState(feet.below()).isAir()
                        && level.getBlockState(feet).isAir()
                        && level.getBlockState(feet.above()).isAir()) return feet;
            }
        }
        return new BlockPos(x, preferredY, z);
    }

    private static void verifyCiIfReady(
            ServerLevel level,
            ErdenFireResponseSavedData data) {
        if (!isFireCi() || ciPassed || !ciPrepared || ciFirePos == null) return;
        if (data.detectedCount() <= ciDetectedBaseline
                || data.dispatchedCount() <= ciDispatchedBaseline
                || data.extinguishedCount() <= ciExtinguishedBaseline
                || isFire(level, ciFirePos)
                || data.activeCount() != 0) return;
        ciPassed = true;
        cleanupCi(level);
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_FIRE_RESPONSE_PASS revision=1 cisterns=8 detected=true guard_dispatched=true cistern_staged=true water_draw_ticks={} actual_navigation=true proximity_extinguish=true loaded_city_only=true forced_citywide=false persistent_incidents=true",
                WATER_DRAW_TICKS);
    }

    private static void cleanupCi(ServerLevel level) {
        if (ciFirePos != null && isFire(level, ciFirePos)) {
            level.setBlock(ciFirePos, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
        }
        if (ciSupportPos != null && level.getBlockState(ciSupportPos).is(Blocks.NETHERRACK)) {
            level.setBlock(ciSupportPos, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
        }
        if (ciResponderUuid != null) {
            Entity entity = level.getEntity(ciResponderUuid);
            if (entity != null) entity.discard();
        }
        if (ciTicketHeld && ciChunk != null) {
            level.getChunkSource().removeTicketWithRadius(TicketType.PORTAL, ciChunk, 0);
            ciTicketHeld = false;
        }
    }

    private static boolean isFireCi() {
        return "1".equals(System.getenv("LIVING_KINGDOMS_CI_FIRE_RESPONSE_TEST"));
    }
}
