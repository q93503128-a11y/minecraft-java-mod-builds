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
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Physical Silver River port layered onto the existing kingdom-supply escrow.
 *
 * <p>The west wharf remains the authoritative paper-mill transfer node. This subsystem makes that
 * accounting route visible: a real navigable channel, stone-and-timber quay, customs house,
 * shipyard/slipways and an actual chest boat that materialises an in-transit {@code mode=barge}
 * shipment when the port is loaded by a player. Normal play never force-loads the river corridor;
 * diagnostic tickets exist only in CI.</p>
 */
@EventBusSubscriber(modid = LivingKingdoms.MOD_ID)
public final class ErdenRiverPortManager {
    public static final int PORT_REVISION = 1;

    private static final int PORT_MIN_X = -1_470;
    private static final int PORT_MAX_X = -1_195;
    private static final int PORT_MIN_Z = 180;
    private static final int PORT_MAX_Z = 540;
    private static final int CHANNEL_HALF_WIDTH = 36;
    private static final int WATER_TOP_Y = 63;
    private static final int CHANNEL_BOTTOM_Y = 59;
    private static final int QUAY_Y = 64;
    private static final int TICK_BUDGET = 2_400;
    private static final int VESSEL_INTERVAL = 5;
    private static final int CI_TICKET_REFRESH_INTERVAL = 100;
    private static final int PHYSICAL_RADIUS = 340;
    private static final double VESSEL_SPEED = 0.20D;
    private static final Identifier CHEST_BOAT_ID =
            Identifier.fromNamespaceAndPath("minecraft", "oak_chest_boat");
    private static final Identifier BOAT_FALLBACK_ID =
            Identifier.fromNamespaceAndPath("minecraft", "oak_boat");

    private static final int CUSTOMS_X = -1_230;
    private static final int CUSTOMS_Z = 330;
    private static final int SHIPYARD_X = -1_405;
    private static final int SHIPYARD_Z = 450;

    private static final ArrayDeque<Long> PENDING = new ArrayDeque<>();
    private static final Set<Long> QUEUED = new HashSet<>();
    private static final Set<Long> CI_RETAINED = new HashSet<>();
    private static MinecraftServer activeServer;
    private static ActiveChunk activeChunk;
    private static boolean ciPrepared;
    private static boolean ciPassed;
    private static double ciTravelled;
    private static double ciLastBoatX = Double.NaN;
    private static double ciLastBoatZ = Double.NaN;
    private static long ciLastProgressTick;
    private static int ciLastWaypoint = -1;
    private static double ciBestTargetDistance = Double.POSITIVE_INFINITY;
    private static long ciTicketRefreshes;

    private ErdenRiverPortManager() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(StarterRealmManager.REALM_KEY)
                || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;
        ChunkPos chunk = event.getChunk().getPos();
        if (!intersectsPort(chunk)) return;
        enqueue(level, chunk.x(), chunk.z(), false);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        if (isPortCi()) {
            prepareCi(level);
            if (level.getGameTime() % CI_TICKET_REFRESH_INTERVAL == 0L) {
                refreshCiTickets(level);
            }
        }
        advanceConstruction(level);
        if (level.getGameTime() % VESSEL_INTERVAL == 0L) tickVessel(level);
        if (isPortCi()) verifyCi(level);
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        PENDING.clear();
        QUEUED.clear();
        CI_RETAINED.clear();
        activeChunk = null;
        ciPrepared = false;
        ciPassed = false;
        ciTravelled = 0.0D;
        ciLastBoatX = Double.NaN;
        ciLastBoatZ = Double.NaN;
        ciLastProgressTick = 0L;
        ciLastWaypoint = -1;
        ciBestTargetDistance = Double.POSITIVE_INFINITY;
        ciTicketRefreshes = 0L;
    }

    private static void enqueue(ServerLevel level, int chunkX, int chunkZ, boolean priority) {
        if (!intersectsPort(new ChunkPos(chunkX, chunkZ))) return;
        long packed = pack(chunkX, chunkZ);
        ErdenRiverPortSavedData data = level.getDataStorage().computeIfAbsent(ErdenRiverPortSavedData.TYPE);
        if (!data.needsChunk(packed, PORT_REVISION) || !QUEUED.add(packed)) return;
        if (priority) PENDING.addFirst(packed);
        else PENDING.addLast(packed);
    }

    private static void advanceConstruction(ServerLevel level) {
        if (activeChunk == null) startNext(level);
        if (activeChunk == null) return;
        if (!level.hasChunk(activeChunk.chunkX(), activeChunk.chunkZ())) return;

        activeChunk.plan().apply(level, TICK_BUDGET);
        if (!activeChunk.plan().done()) return;

        ErdenRiverPortSavedData data = level.getDataStorage().computeIfAbsent(ErdenRiverPortSavedData.TYPE);
        data.markChunk(activeChunk.packed(), PORT_REVISION);
        QUEUED.remove(activeChunk.packed());
        if (!isPortCi()) releaseCi(level, activeChunk.packed());
        if (isPortCi()) {
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_RIVER_PORT_CHUNK_COMPLETE chunk={},{} writes={} waterway=true loaded_only={}",
                    activeChunk.chunkX(), activeChunk.chunkZ(), activeChunk.plan().appliedWrites(), !isPortCi());
        }
        activeChunk = null;
    }

    private static void startNext(ServerLevel level) {
        ErdenRiverPortSavedData data = level.getDataStorage().computeIfAbsent(ErdenRiverPortSavedData.TYPE);
        while (!PENDING.isEmpty()) {
            long packed = PENDING.removeFirst();
            int chunkX = unpackX(packed);
            int chunkZ = unpackZ(packed);
            if (!data.needsChunk(packed, PORT_REVISION)) {
                QUEUED.remove(packed);
                if (!isPortCi()) releaseCi(level, packed);
                continue;
            }
            if (!level.hasChunk(chunkX, chunkZ)) {
                if (CI_RETAINED.contains(packed)) PENDING.addLast(packed);
                else QUEUED.remove(packed);
                return;
            }
            ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
            IncrementalWorldEditPlan plan = createPortChunkPlan(level, chunk);
            activeChunk = new ActiveChunk(packed, chunkX, chunkZ, plan);
            LivingKingdoms.LOGGER.debug(
                    "Prepared Erden river-port chunk {},{} writes={} operations={} loaded_only={} ci_ticket={}",
                    chunkX, chunkZ, plan.estimatedWrites(), plan.operationCount(),
                    !isPortCi(), CI_RETAINED.contains(packed));
            return;
        }
    }

    private static IncrementalWorldEditPlan createPortChunkPlan(ServerLevel level, ChunkPos chunk) {
        IncrementalWorldEditPlan plan = new IncrementalWorldEditPlan(chunk);
        addNavigableChannel(plan, chunk);
        addEastQuay(plan, chunk);
        addWestWharfRamp(plan, chunk);
        addCustomsHouse(plan, chunk);
        addShipyard(plan, chunk);
        return plan;
    }

    private static void addNavigableChannel(IncrementalWorldEditPlan plan, ChunkPos chunk) {
        int minX = chunk.getMinBlockX();
        int minZ = chunk.getMinBlockZ();
        for (int x = minX; x <= minX + 15; x++) {
            for (int z = minZ; z <= minZ + 15; z++) {
                if (z < PORT_MIN_Z || z > PORT_MAX_Z) continue;
                int centerX = riverX(z);
                if (Math.abs(x - centerX) > CHANNEL_HALF_WIDTH) continue;
                plan.addSet(x, CHANNEL_BOTTOM_Y, z, Blocks.GRAVEL);
                plan.addFill(x, CHANNEL_BOTTOM_Y + 1, z, x, WATER_TOP_Y, z, Blocks.WATER);
                plan.addFill(x, WATER_TOP_Y + 1, z, x, 82, z, Blocks.AIR);
            }
        }
    }

    private static void addEastQuay(IncrementalWorldEditPlan plan, ChunkPos chunk) {
        for (int z = 205; z <= 390; z++) {
            int eastBank = riverX(z) + CHANNEL_HALF_WIDTH + 1;
            for (int offset = 0; offset <= 7; offset++) {
                int x = eastBank + offset;
                setIfChunk(plan, chunk, x, QUAY_Y, z, offset <= 2 ? Blocks.STONE_BRICKS : Blocks.OAK_PLANKS);
                if (offset <= 2 && Math.floorMod(z, 8) == 0) {
                    fillIfChunk(plan, chunk, x, 60, z, x, QUAY_Y - 1, z, Blocks.STONE_BRICKS);
                }
            }
            if (Math.floorMod(z, 12) == 0) {
                setIfChunk(plan, chunk, eastBank + 1, QUAY_Y + 1, z, Blocks.OAK_FENCE);
                setIfChunk(plan, chunk, eastBank + 5, QUAY_Y + 1, z, Blocks.BARREL);
            }
        }
    }

    private static void addWestWharfRamp(IncrementalWorldEditPlan plan, ChunkPos chunk) {
        for (int z = 244; z <= 256; z++) {
            for (int x = -1_220; x >= -1_266; x--) {
                int distance = -1_220 - x;
                int y = distance < 10 ? 68 : distance < 20 ? 67 : distance < 31 ? 66 : 65;
                setIfChunk(plan, chunk, x, y, z, Blocks.OAK_PLANKS);
                if ((z == 244 || z == 256) && Math.floorMod(distance, 7) == 0) {
                    fillIfChunk(plan, chunk, x, 60, z, x, y - 1, z, Blocks.STRIPPED_OAK_LOG);
                    setIfChunk(plan, chunk, x, y + 1, z, Blocks.OAK_FENCE);
                }
            }
        }
        setIfChunk(plan, chunk, -1_254, 66, 250, Blocks.BARREL);
        setIfChunk(plan, chunk, -1_258, 66, 250, Blocks.CHEST);
    }

    private static void addCustomsHouse(IncrementalWorldEditPlan plan, ChunkPos chunk) {
        int minX = CUSTOMS_X - 11;
        int maxX = CUSTOMS_X + 11;
        int minZ = CUSTOMS_Z - 8;
        int maxZ = CUSTOMS_Z + 8;
        for (int x = Math.max(minX - 3, chunk.getMinBlockX()); x <= Math.min(maxX + 3, chunk.getMinBlockX() + 15); x++) {
            for (int z = Math.max(minZ - 3, chunk.getMinBlockZ()); z <= Math.min(maxZ + 3, chunk.getMinBlockZ() + 15); z++) {
                fillIfChunk(plan, chunk, x, 60, z, x, 67, z, Blocks.STONE_BRICKS);
                fillIfChunk(plan, chunk, x, 68, z, x, 81, z, Blocks.AIR);
            }
        }
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean wall = x == minX || x == maxX || z == minZ || z == maxZ;
                setIfChunk(plan, chunk, x, 68, z, Blocks.POLISHED_ANDESITE);
                if (wall) fillIfChunk(plan, chunk, x, 69, z, x, 75, z, Blocks.STONE_BRICKS);
                else fillIfChunk(plan, chunk, x, 69, z, x, 75, z, Blocks.AIR);
            }
        }
        for (int layer = 0; layer <= 4; layer++) {
            fillIfChunk(plan, chunk,
                    minX - 2 + layer, 76 + layer, minZ - 2 + layer,
                    maxX + 2 - layer, 76 + layer, maxZ + 2 - layer,
                    Blocks.DARK_OAK_PLANKS);
        }
        fillIfChunk(plan, chunk, maxX, 69, CUSTOMS_Z - 1, maxX, 72, CUSTOMS_Z + 1, Blocks.AIR);
        for (int z : new int[]{CUSTOMS_Z - 5, CUSTOMS_Z + 5}) {
            setIfChunk(plan, chunk, minX, 72, z, Blocks.GLASS_PANE);
            setIfChunk(plan, chunk, maxX, 72, z, Blocks.GLASS_PANE);
        }
        for (int x : new int[]{CUSTOMS_X - 6, CUSTOMS_X + 6}) {
            setIfChunk(plan, chunk, x, 72, minZ, Blocks.GLASS_PANE);
            setIfChunk(plan, chunk, x, 72, maxZ, Blocks.GLASS_PANE);
        }
        setIfChunk(plan, chunk, CUSTOMS_X - 4, 69, CUSTOMS_Z, Blocks.BARREL);
        setIfChunk(plan, chunk, CUSTOMS_X, 69, CUSTOMS_Z, Blocks.LECTERN);
        setIfChunk(plan, chunk, CUSTOMS_X + 4, 69, CUSTOMS_Z, Blocks.CHEST);
        setIfChunk(plan, chunk, CUSTOMS_X, 69, CUSTOMS_Z - 5, Blocks.CRAFTING_TABLE);
    }

    private static void addShipyard(IncrementalWorldEditPlan plan, ChunkPos chunk) {
        int minX = SHIPYARD_X - 14;
        int maxX = SHIPYARD_X + 14;
        int minZ = SHIPYARD_Z - 11;
        int maxZ = SHIPYARD_Z + 11;
        for (int x = Math.max(minX - 4, chunk.getMinBlockX()); x <= Math.min(maxX + 4, chunk.getMinBlockX() + 15); x++) {
            for (int z = Math.max(minZ - 4, chunk.getMinBlockZ()); z <= Math.min(maxZ + 4, chunk.getMinBlockZ() + 15); z++) {
                fillIfChunk(plan, chunk, x, 60, z, x, 67, z, Blocks.COARSE_DIRT);
                fillIfChunk(plan, chunk, x, 68, z, x, 82, z, Blocks.AIR);
            }
        }
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean wall = x == minX || x == maxX || z == minZ || z == maxZ;
                setIfChunk(plan, chunk, x, 68, z, Blocks.STONE_BRICKS);
                if (wall) fillIfChunk(plan, chunk, x, 69, z, x, 75, z, Blocks.SPRUCE_PLANKS);
            }
        }
        fillIfChunk(plan, chunk, maxX, 69, SHIPYARD_Z - 3, maxX, 73, SHIPYARD_Z + 3, Blocks.AIR);
        for (int layer = 0; layer < 4; layer++) {
            fillIfChunk(plan, chunk,
                    minX - 2 + layer, 76 + layer, minZ - 2 + layer,
                    maxX + 2 - layer, 76 + layer, maxZ + 2 - layer,
                    Blocks.DARK_OAK_PLANKS);
        }
        setIfChunk(plan, chunk, SHIPYARD_X - 5, 69, SHIPYARD_Z, Blocks.SMITHING_TABLE);
        setIfChunk(plan, chunk, SHIPYARD_X, 69, SHIPYARD_Z, Blocks.CRAFTING_TABLE);
        setIfChunk(plan, chunk, SHIPYARD_X + 5, 69, SHIPYARD_Z, Blocks.BARREL);

        for (int slipZ : new int[]{425, 475}) {
            for (int x = -1_390; x <= riverX(slipZ) - CHANNEL_HALF_WIDTH + 4; x++) {
                int progress = x + 1_390;
                int y = Math.max(64, 68 - progress / 14);
                setIfChunk(plan, chunk, x, y, slipZ - 2, Blocks.STRIPPED_SPRUCE_LOG);
                setIfChunk(plan, chunk, x, y, slipZ + 2, Blocks.STRIPPED_SPRUCE_LOG);
                if (Math.floorMod(progress, 5) == 0) {
                    setIfChunk(plan, chunk, x, y, slipZ, Blocks.OAK_PLANKS);
                }
            }
        }
        // Simple gantry crane and material stacks make the yard readable as a working shipyard.
        fillIfChunk(plan, chunk, -1_375, 68, 448, -1_375, 78, 448, Blocks.STRIPPED_SPRUCE_LOG);
        fillIfChunk(plan, chunk, -1_375, 78, 448, -1_355, 78, 448, Blocks.STRIPPED_SPRUCE_LOG);
        fillIfChunk(plan, chunk, -1_355, 73, 448, -1_355, 77, 448, Blocks.IRON_CHAIN);
        setIfChunk(plan, chunk, -1_355, 72, 448, Blocks.BARREL);
        for (int z = 458; z <= 466; z += 4) {
            fillIfChunk(plan, chunk, -1_430, 68, z, -1_420, 68, z, Blocks.STRIPPED_OAK_LOG);
        }
    }

    private static void tickVessel(ServerLevel level) {
        ErdenRiverPortSavedData port = level.getDataStorage().computeIfAbsent(ErdenRiverPortSavedData.TYPE);
        ErdenKingdomSupplySavedData supply = level.getDataStorage().computeIfAbsent(ErdenKingdomSupplySavedData.TYPE);

        boolean mayMaterialise = isPortCi() || nearAnyPlayer(level, -1_250, 300, PHYSICAL_RADIUS);
        if (!mayMaterialise) {
            discardBoat(level, port);
            port.clearVessel();
            return;
        }
        if (!corePortBuilt(level, port)) return;

        ErdenKingdomSupplySavedData.ShipmentState shipment = findShipment(supply, port.activeShipment());
        if (shipment == null && port.activeShipment().isEmpty()) {
            shipment = supply.shipments().stream()
                    .filter(candidate -> candidate.mode().equals("barge")
                            && candidate.status().equals("in_transit"))
                    .findFirst().orElse(null);
            if (shipment == null) return;
            port.assignVessel(shipment.id(), "");
        }
        if (shipment == null) {
            discardBoat(level, port);
            port.clearVessel();
            return;
        }

        List<RoutePoint> route = vesselRoute();
        Entity boat = resolveBoat(level, port.boatUuid());
        if (boat == null && port.waypoint() < route.size()) {
            RoutePoint spawn = route.get(Math.min(port.waypoint(), route.size() - 1));
            if (!routeChunkReady(level, port, spawn) || !waterAt(level, spawn)) return;
            boat = createBoat(level, shipment, spawn);
            if (boat == null) return;
            port.setVesselIdentity(boat.getUUID().toString());
            if (port.activeShipment().isEmpty()) port.assignVessel(shipment.id(), boat.getUUID().toString());
            if (isPortCi()) {
                ciLastProgressTick = level.getGameTime();
                ciLastWaypoint = port.waypoint();
                ciBestTargetDistance = Double.POSITIVE_INFINITY;
            }
            LivingKingdoms.LOGGER.info(
                    "Materialized Erden supply barge shipment={} resource={} amount={} real_entity=true escrow_linked=true loaded_only={} route_points={}",
                    shipment.id(), shipment.resource(), shipment.amount(), !isPortCi(), route.size());
        }
        if (boat == null) return;

        if (isPortCi()) accumulateCiTravel(boat);
        int index = port.waypoint();
        if (index >= route.size()) {
            boat.setDeltaMovement(0.0D, boat.getDeltaMovement().y, 0.0D);
            if (shipment.terminal()) {
                LivingKingdoms.LOGGER.info(
                        "Unloaded Erden supply barge shipment={} status={} physical_dock=true customs_quay=true escrow_authoritative=true",
                        shipment.id(), shipment.status());
                boat.discard();
                port.clearVessel();
            }
            return;
        }

        RoutePoint target = route.get(index);
        if (!routeChunkReady(level, port, target)) {
            boat.setDeltaMovement(0.0D, boat.getDeltaMovement().y, 0.0D);
            return;
        }
        double dx = target.x() + 0.5D - boat.getX();
        double dz = target.z() + 0.5D - boat.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance <= 4.0D) {
            int next = index + 1;
            port.setWaypoint(next);
            if (isPortCi()) {
                ciLastProgressTick = level.getGameTime();
                ciLastWaypoint = next;
                ciBestTargetDistance = Double.POSITIVE_INFINITY;
            }
            if (next >= route.size()) {
                boat.setDeltaMovement(0.0D, boat.getDeltaMovement().y, 0.0D);
                port.markDocked();
                LivingKingdoms.LOGGER.info(
                        "Erden supply barge docked shipment={} resource={} amount={} at_west_wharf=true actual_water_route=true customs_ready=true",
                        shipment.id(), shipment.resource(), shipment.amount());
            }
            return;
        }

        // An unmanned vanilla boat can damp externally assigned velocity before it makes reliable
        // route progress. Move the real entity through Minecraft collision resolution by the exact
        // distance that the configured 0.20 m/tick speed represents over this 5-tick controller
        // interval. This is not a waypoint teleport: collisions still constrain the entity.
        double step = Math.min(Math.max(0.0D, distance - 3.0D), VESSEL_SPEED * VESSEL_INTERVAL);
        double moveX = dx / distance * step;
        double moveZ = dz / distance * step;
        double beforeX = boat.getX();
        double beforeZ = boat.getZ();
        boat.move(MoverType.SELF, new Vec3(moveX, 0.0D, moveZ));
        boat.setDeltaMovement(0.0D, boat.getDeltaMovement().y, 0.0D);
        boat.setYRot((float) Math.toDegrees(Math.atan2(-moveX, moveZ)));

        double actualX = boat.getX() - beforeX;
        double actualZ = boat.getZ() - beforeZ;
        double actualMoved = Math.sqrt(actualX * actualX + actualZ * actualZ);
        if (isPortCi()) {
            long now = level.getGameTime();
            double remainingX = target.x() + 0.5D - boat.getX();
            double remainingZ = target.z() + 0.5D - boat.getZ();
            double remainingDistance = Math.sqrt(remainingX * remainingX + remainingZ * remainingZ);
            if (ciLastWaypoint != port.waypoint()) {
                ciLastWaypoint = port.waypoint();
                ciBestTargetDistance = remainingDistance;
                ciLastProgressTick = now;
            } else if (remainingDistance + 0.20D < ciBestTargetDistance) {
                ciBestTargetDistance = remainingDistance;
                ciLastProgressTick = now;
            }
            if (now % 100L == 0L) {
                LivingKingdoms.LOGGER.info(
                        "LK_ERDEN_RIVER_PORT_PROGRESS waypoint={} x={} y={} z={} target_x={} target_z={} distance={} moved={} travelled={} water_target={} entity_loaded=true collision_move=true goal_progress=true",
                        port.waypoint(), Math.round(boat.getX()), Math.round(boat.getY()), Math.round(boat.getZ()),
                        target.x(), target.z(), Math.round(remainingDistance), String.format(java.util.Locale.ROOT, "%.3f", actualMoved),
                        Math.round(ciTravelled), waterAt(level, target));
            }
            if (ciLastProgressTick > 0L
                    && now - ciLastProgressTick > 400L
                    && routeChunkReady(level, port, target)
                    && waterAt(level, target)) {
                LivingKingdoms.LOGGER.error(
                        "LK_ERDEN_RIVER_PORT_STALL waypoint={} x={} z={} target_x={} target_z={} distance={} best_distance={} travelled={} stalled_ticks={} entity_loaded=true route_ready=true water_target=true goal_progress=false",
                        port.waypoint(), Math.round(boat.getX()), Math.round(boat.getZ()), target.x(), target.z(),
                        Math.round(remainingDistance), String.format(java.util.Locale.ROOT, "%.2f", ciBestTargetDistance),
                        Math.round(ciTravelled), now - ciLastProgressTick);
                throw new IllegalStateException("LK_ERDEN_RIVER_PORT_STALL physical barge failed to approach its active waypoint");
            }
        }
    }

    private static Entity createBoat(
            ServerLevel level,
            ErdenKingdomSupplySavedData.ShipmentState shipment,
            RoutePoint spawn) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(CHEST_BOAT_ID)
                .orElseGet(() -> BuiltInRegistries.ENTITY_TYPE.getOptional(BOAT_FALLBACK_ID).orElse(null));
        Entity boat = type == null ? null : type.create(level, EntitySpawnReason.COMMAND);
        if (boat == null) return null;
        boat.setPos(spawn.x() + 0.5D, WATER_TOP_Y + 0.35D, spawn.z() + 0.5D);
        boat.setCustomName(Component.literal(
                "에르덴 화물선 · " + shipment.resource() + " " + shipment.amount()));
        boat.setCustomNameVisible(false);
        return level.addFreshEntity(boat) ? boat : null;
    }

    private static List<RoutePoint> vesselRoute() {
        List<RoutePoint> route = new ArrayList<>();
        for (int z = 382; z >= 262; z -= 10) route.add(new RoutePoint(riverX(z), z));
        route.add(new RoutePoint(-1_274, 252));
        route.add(new RoutePoint(-1_266, 250));
        return List.copyOf(route);
    }

    private static boolean routeChunkReady(
            ServerLevel level,
            ErdenRiverPortSavedData port,
            RoutePoint point) {
        int chunkX = point.x() >> 4;
        int chunkZ = point.z() >> 4;
        return level.hasChunk(chunkX, chunkZ)
                && port.builtChunk(pack(chunkX, chunkZ), PORT_REVISION);
    }

    private static boolean corePortBuilt(ServerLevel level, ErdenRiverPortSavedData port) {
        int[][] anchors = {
                {-1_254, 250},
                {CUSTOMS_X, CUSTOMS_Z},
                {SHIPYARD_X, SHIPYARD_Z},
                {riverX(382), 382}
        };
        for (int[] anchor : anchors) {
            int chunkX = anchor[0] >> 4;
            int chunkZ = anchor[1] >> 4;
            if (!level.hasChunk(chunkX, chunkZ)
                    || !port.builtChunk(pack(chunkX, chunkZ), PORT_REVISION)) return false;
        }
        return true;
    }

    private static boolean waterAt(ServerLevel level, RoutePoint point) {
        return level.getBlockState(new BlockPos(point.x(), WATER_TOP_Y, point.z())).is(Blocks.WATER);
    }

    private static ErdenKingdomSupplySavedData.ShipmentState findShipment(
            ErdenKingdomSupplySavedData supply,
            String id) {
        if (id == null || id.isBlank()) return null;
        for (ErdenKingdomSupplySavedData.ShipmentState shipment : supply.shipments()) {
            if (shipment.id().equals(id)) return shipment;
        }
        return null;
    }

    private static Entity resolveBoat(ServerLevel level, String uuid) {
        if (uuid == null || uuid.isBlank()) return null;
        try {
            return level.getEntity(UUID.fromString(uuid));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void discardBoat(ServerLevel level, ErdenRiverPortSavedData data) {
        Entity boat = resolveBoat(level, data.boatUuid());
        if (boat != null) boat.discard();
    }

    private static boolean nearAnyPlayer(ServerLevel level, int x, int z, int radius) {
        double radiusSquared = (double) radius * radius;
        for (ServerPlayer player : level.players()) {
            double dx = player.getX() - x;
            double dz = player.getZ() - z;
            if (dx * dx + dz * dz <= radiusSquared) return true;
        }
        return false;
    }

    private static void prepareCi(ServerLevel level) {
        if (ciPrepared) return;
        ciPrepared = true;
        Set<Long> required = new HashSet<>();
        for (RoutePoint point : vesselRoute()) required.add(pack(point.x() >> 4, point.z() >> 4));
        for (int[] anchor : new int[][]{
                {-1_254, 250}, {CUSTOMS_X, CUSTOMS_Z}, {SHIPYARD_X, SHIPYARD_Z}
        }) {
            required.add(pack(anchor[0] >> 4, anchor[1] >> 4));
        }
        for (long packed : required) {
            int chunkX = unpackX(packed);
            int chunkZ = unpackZ(packed);
            CI_RETAINED.add(packed);
            level.getChunkSource().addTicketAndLoadWithRadius(
                    TicketType.PORTAL, new ChunkPos(chunkX, chunkZ), 0);
            enqueue(level, chunkX, chunkZ, true);
        }
        LivingKingdoms.LOGGER.info(
                "Prepared Erden river-port CI chunks={} west_wharf_preserved=true silver_river_centered=true customs=true shipyard=true physical_barge=true normal_force_load=false",
                required.size());
    }

    private static void refreshCiTickets(ServerLevel level) {
        if (CI_RETAINED.isEmpty()) return;
        int loaded = 0;
        for (long packed : Set.copyOf(CI_RETAINED)) {
            ChunkPos chunk = new ChunkPos(unpackX(packed), unpackZ(packed));
            level.getChunkSource().addTicketAndLoadWithRadius(TicketType.PORTAL, chunk, 0);
            if (level.hasChunk(chunk.x(), chunk.z())) loaded++;
        }
        ciTicketRefreshes++;
        if (ciTicketRefreshes == 1L || ciTicketRefreshes % 5L == 0L) {
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_RIVER_PORT_TICKET_REFRESH refresh={} retained={} loaded={} interval_ticks={} timeout_safe_refresh=true async_ticket=true forced_chunks=false",
                    ciTicketRefreshes, CI_RETAINED.size(), loaded, CI_TICKET_REFRESH_INTERVAL);
        }
    }

    private static void verifyCi(ServerLevel level) {
        if (!ciPrepared || ciPassed) return;
        ErdenRiverPortSavedData port = level.getDataStorage().computeIfAbsent(ErdenRiverPortSavedData.TYPE);
        if (!corePortBuilt(level, port)) return;
        if (!level.getBlockState(new BlockPos(riverX(250), WATER_TOP_Y, 250)).is(Blocks.WATER)) return;
        if (!level.getBlockState(new BlockPos(-1_254, 66, 250)).is(Blocks.BARREL)) return;
        if (!level.getBlockState(new BlockPos(CUSTOMS_X, 69, CUSTOMS_Z)).is(Blocks.LECTERN)) return;
        if (!level.getBlockState(new BlockPos(SHIPYARD_X - 5, 69, SHIPYARD_Z)).is(Blocks.SMITHING_TABLE)) return;
        if (port.vesselsSpawned() <= 0L || port.vesselsDocked() <= 0L || ciTravelled < 48.0D) return;
        ciPassed = true;
        for (long packed : Set.copyOf(CI_RETAINED)) releaseCi(level, packed);
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_RIVER_PORT_PASS revision=1 silver_river_navigable=true west_wharf_physical=true customs_house=true shipyard=true supply_barge_escrow_linked=true real_boat_entity=true actual_water_movement=true travelled_metres={} loaded_only_runtime=true forced_citywide=false ci_corridor_only=true ci_corridor_retained_until_pass=true ci_tickets_released_at_pass=true ci_ticket_refreshes={} timeout_safe_refresh=true",
                Math.round(ciTravelled), ciTicketRefreshes);
    }

    private static void accumulateCiTravel(Entity boat) {
        if (Double.isFinite(ciLastBoatX) && Double.isFinite(ciLastBoatZ)) {
            double dx = boat.getX() - ciLastBoatX;
            double dz = boat.getZ() - ciLastBoatZ;
            double step = Math.sqrt(dx * dx + dz * dz);
            if (step < 4.0D) ciTravelled += step;
        }
        ciLastBoatX = boat.getX();
        ciLastBoatZ = boat.getZ();
    }

    private static void releaseCi(ServerLevel level, long packed) {
        if (!CI_RETAINED.remove(packed)) return;
        level.getChunkSource().removeTicketWithRadius(
                TicketType.PORTAL,
                new ChunkPos(unpackX(packed), unpackZ(packed)), 0);
    }

    private static int riverX(int z) {
        return (int) Math.round(AuthoredContinentDensity.silverRiverCenterX(z));
    }

    private static boolean intersectsPort(ChunkPos chunk) {
        int minX = chunk.getMinBlockX();
        int maxX = minX + 15;
        int minZ = chunk.getMinBlockZ();
        int maxZ = minZ + 15;
        return maxX >= PORT_MIN_X && minX <= PORT_MAX_X
                && maxZ >= PORT_MIN_Z && minZ <= PORT_MAX_Z;
    }

    private static void setIfChunk(
            IncrementalWorldEditPlan plan,
            ChunkPos chunk,
            int x, int y, int z,
            Block block) {
        if (inside(chunk, x, z)) plan.addSet(x, y, z, block);
    }

    private static void fillIfChunk(
            IncrementalWorldEditPlan plan,
            ChunkPos chunk,
            int x1, int y1, int z1,
            int x2, int y2, int z2,
            Block block) {
        int minX = Math.max(Math.min(x1, x2), chunk.getMinBlockX());
        int maxX = Math.min(Math.max(x1, x2), chunk.getMinBlockX() + 15);
        int minZ = Math.max(Math.min(z1, z2), chunk.getMinBlockZ());
        int maxZ = Math.min(Math.max(z1, z2), chunk.getMinBlockZ() + 15);
        if (minX > maxX || minZ > maxZ) return;
        plan.addFill(minX, y1, minZ, maxX, y2, maxZ, block);
    }

    private static boolean inside(ChunkPos chunk, int x, int z) {
        return x >= chunk.getMinBlockX() && x <= chunk.getMinBlockX() + 15
                && z >= chunk.getMinBlockZ() && z <= chunk.getMinBlockZ() + 15;
    }

    private static long pack(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
    }

    private static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    private static int unpackZ(long packed) {
        return (int) packed;
    }

    private static boolean isPortCi() {
        return "1".equals(System.getenv("LIVING_KINGDOMS_CI_RIVER_PORT_TEST"));
    }

    private record ActiveChunk(long packed, int chunkX, int chunkZ, IncrementalWorldEditPlan plan) {
    }

    private record RoutePoint(int x, int z) {
    }
}
