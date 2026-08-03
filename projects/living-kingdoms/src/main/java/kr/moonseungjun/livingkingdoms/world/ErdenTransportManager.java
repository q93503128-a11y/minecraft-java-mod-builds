package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;

/**
 * Turns Erden's daily delivery ledger into persistent road manifests. The complete capital remains
 * numerically simulated while only routes near a player are materialised as porters and cargo carts.
 * Loaded roads are inspected for real obstructions; blocked routes are delayed and retried instead
 * of teleporting couriers through walls.
 */
public final class ErdenTransportManager {
    public static final int TRANSPORT_REVISION = 1;

    private static final Identifier VILLAGER_ID =
            Identifier.fromNamespaceAndPath("minecraft", "villager");
    private static final Identifier CART_ID =
            Identifier.fromNamespaceAndPath("minecraft", "chest_minecart");
    private static final int TICK_INTERVAL = 10;
    private static final int LOADING_TICKS = 60;
    private static final int UNLOADING_TICKS = 60;
    private static final int PHYSICAL_RADIUS = 224;
    private static final int DESPAWN_RADIUS = 288;
    private static final int MAX_PHYSICAL_JOBS = 18;
    private static final int MAX_ROUTE_SEARCH = 120_000;
    private static final int MAX_ROUTE_POINTS = 320;
    private static final int MAX_RETRIES = 3;
    private static final int STALL_TICKS = 240;
    private static final long TERMINAL_RETENTION = 24_000L;
    private static final double PORTER_SPEED = 0.68D;

    private static MinecraftServer activeServer;
    private static boolean ciPassed;
    private static final Map<String, ProgressState> PROGRESS = new HashMap<>();

    private ErdenTransportManager() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        ErdenPhysicalEconomySavedData economy = level.getDataStorage()
                .computeIfAbsent(ErdenPhysicalEconomySavedData.TYPE);
        ErdenPopulationSavedData population = level.getDataStorage()
                .computeIfAbsent(ErdenPopulationSavedData.TYPE);
        if (economy.lastProcessedDay() < 0L
                || economy.sites().size() != ErdenPhysicalEconomyManager.EXPECTED_SITES
                || population.households().size() != ErdenPopulationManager.EXPECTED_HOUSEHOLDS) {
            return;
        }

        ErdenTransportSavedData transport = level.getDataStorage()
                .computeIfAbsent(ErdenTransportSavedData.TYPE);
        if (transport.lastManifestDay() < economy.lastProcessedDay()) {
            scheduleManifestDay(level, population, economy, transport, economy.lastProcessedDay());
        }
        if (level.getGameTime() % TICK_INTERVAL == 0L) {
            tickPhysicalJobs(level, economy, transport);
            transport.pruneTerminalJobs(level.getGameTime() - TERMINAL_RETENTION);
        }
        verifyCi(level, population, economy, transport);
    }

    public static void handleInteraction(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)
                || !level.dimension().equals(StarterRealmManager.REALM_KEY)) {
            return;
        }
        ErdenTransportSavedData transport = level.getDataStorage()
                .computeIfAbsent(ErdenTransportSavedData.TYPE);
        String uuid = event.getTarget().getUUID().toString();
        ErdenTransportSavedData.DeliveryJob job = transport.jobs().stream()
                .filter(candidate -> candidate.porterUuid().equals(uuid)
                        || candidate.cartUuid().equals(uuid))
                .findFirst().orElse(null);
        if (job == null) return;
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        player.sendSystemMessage(Component.literal(
                "§6[왕도 운송] §f" + resourceName(job.resource()) + " " + job.amount()
                        + " | " + siteLabel(job.sourceId()) + " → " + siteLabel(job.targetId())
                        + " | 상태 " + statusName(job.status())
                        + " | 경로 " + job.route().size() + "구간"
                        + (job.cart() ? " | 화물 수레" : " | 짐꾼 운반")));
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        ciPassed = false;
        PROGRESS.clear();
    }

    private static void scheduleManifestDay(
            ServerLevel level,
            ErdenPopulationSavedData population,
            ErdenPhysicalEconomySavedData economy,
            ErdenTransportSavedData transport,
            long day) {
        List<Manifest> manifests = buildManifests(population, economy.sites());
        long modeledTicks = 0L;
        for (Manifest manifest : manifests) modeledTicks += estimatedTravelTicks(manifest);
        transport.beginManifestDay(day, manifests.size(), modeledTicks);

        List<Manifest> ordered = new ArrayList<>(manifests);
        ordered.sort(Comparator.comparingDouble((Manifest manifest) -> proximityScore(level, manifest))
                .thenComparing(Manifest::sourceId)
                .thenComparing(Manifest::targetId)
                .thenComparing(Manifest::resource));

        int capacity = Math.max(0, MAX_PHYSICAL_JOBS - transport.activeJobCount());
        int physicalized = 0;
        int blocked = 0;
        for (Manifest manifest : ordered) {
            if (physicalized >= capacity || !nearAnyPlayer(level, manifest, PHYSICAL_RADIUS)) continue;
            ErdenPhysicalEconomySavedData.SiteState source = findSite(economy.sites(), manifest.sourceId());
            ErdenPhysicalEconomySavedData.SiteState target = findSite(economy.sites(), manifest.targetId());
            if (source == null || target == null) continue;
            List<ErdenTransportSavedData.RoutePoint> route = findRoute(level, source, target);
            if (route.isEmpty()) {
                blocked++;
                transport.markBlocked();
                economy.replaceSite(source.addMetric("blocked_shipments", 1L));
                economy.replaceSite(target.addMetric("delivery_delays", 1L));
                continue;
            }
            boolean cart = requiresCart(manifest.resource(), manifest.amount());
            ErdenTransportSavedData.DeliveryJob job = new ErdenTransportSavedData.DeliveryJob(
                    transport.nextJobId(day), manifest.sourceId(), manifest.targetId(),
                    manifest.resource(), manifest.amount(), level.getGameTime(), level.getGameTime(),
                    "loading", route, 0, 0, cart, "", "", 0L);
            transport.addJob(job);
            physicalized++;
        }

        LivingKingdoms.LOGGER.info(
                "Prepared Erden road manifests day={} manifests={} modeled_travel_ticks={} physicalized={} blocked={} active_jobs={} unloaded_routes=aggregate",
                day, manifests.size(), modeledTicks, physicalized, blocked, transport.activeJobCount());
    }

    private static List<Manifest> buildManifests(
            ErdenPopulationSavedData population,
            List<ErdenPhysicalEconomySavedData.SiteState> sites) {
        Map<String, VirtualSite> virtual = new LinkedHashMap<>();
        for (ErdenPhysicalEconomySavedData.SiteState site : sites) {
            virtual.put(site.id(), new VirtualSite(site));
        }
        Set<Long> workingSites = livingWorkerSites(population);
        List<Manifest> manifests = new ArrayList<>();

        for (VirtualSite site : virtual.values()) {
            if (!site.role.equals("warehouse")) continue;
            site.add("wheat", 96L);
            site.add("coal", 32L);
            site.add("leather", 24L);
            site.add("paper", 32L);
            site.add("iron", 20L);
            site.add("hay", 40L);
        }

        for (VirtualSite target : List.copyOf(virtual.values())) {
            if (!workingSites.contains(positionKey(target.x, target.z))) continue;
            for (Requirement requirement : requirements(target.role)) {
                long missing = Math.max(0L, requirement.amount() - target.stock(requirement.resource()));
                transferFromNearestWarehouse(virtual, target.id, requirement.resource(), missing, manifests);
            }
        }

        for (VirtualSite site : virtual.values()) {
            if (site.role.equals("bakery")
                    && workingSites.contains(positionKey(site.x, site.z))
                    && site.stock("wheat") >= 6L
                    && site.stock("coal") >= 1L) {
                site.add("wheat", -6L);
                site.add("coal", -1L);
                site.add("bread", 13L);
            }
        }

        List<String> bakeryIds = virtual.values().stream()
                .filter(site -> site.role.equals("bakery"))
                .map(site -> site.id)
                .toList();
        distributeBread(virtual, bakeryIds, "shop", 4L, manifests);
        distributeBread(virtual, bakeryIds, "inn", 2L, manifests);
        distributeBread(virtual, bakeryIds, "shop", 8L, manifests);
        return List.copyOf(manifests);
    }

    private static List<Requirement> requirements(String role) {
        return switch (role) {
            case "bakery" -> List.of(new Requirement("wheat", 6L), new Requirement("coal", 1L));
            case "shop" -> List.of(new Requirement("leather", 2L), new Requirement("paper", 2L));
            case "stable" -> List.of(new Requirement("hay", 3L));
            case "guard_post" -> List.of(new Requirement("iron", 1L), new Requirement("coal", 1L));
            case "bathhouse" -> List.of(new Requirement("coal", 2L));
            default -> List.of();
        };
    }

    private static void transferFromNearestWarehouse(
            Map<String, VirtualSite> sites,
            String targetId,
            String resource,
            long requested,
            List<Manifest> manifests) {
        long remaining = requested;
        while (remaining > 0L) {
            VirtualSite target = sites.get(targetId);
            VirtualSite warehouse = sites.values().stream()
                    .filter(site -> site.role.equals("warehouse") && site.stock(resource) > 0L)
                    .min(Comparator.comparingLong((VirtualSite site) ->
                                    distanceSquared(target.x, target.z, site.x, site.z))
                            .thenComparing(site -> site.id))
                    .orElse(null);
            if (warehouse == null) return;
            long amount = Math.min(remaining, warehouse.stock(resource));
            moveVirtual(warehouse, target, resource, amount, manifests);
            remaining -= amount;
        }
    }

    private static void distributeBread(
            Map<String, VirtualSite> sites,
            List<String> bakeryIds,
            String targetRole,
            long targetStock,
            List<Manifest> manifests) {
        for (String bakeryId : bakeryIds) {
            while (sites.get(bakeryId).stock("bread") > 0L) {
                VirtualSite bakery = sites.get(bakeryId);
                VirtualSite target = sites.values().stream()
                        .filter(site -> site.role.equals(targetRole) && site.stock("bread") < targetStock)
                        .min(Comparator.comparingLong((VirtualSite site) ->
                                        distanceSquared(bakery.x, bakery.z, site.x, site.z))
                                .thenComparing(site -> site.id))
                        .orElse(null);
                if (target == null) break;
                long amount = Math.min(bakery.stock("bread"), targetStock - target.stock("bread"));
                if (amount <= 0L) break;
                moveVirtual(bakery, target, "bread", amount, manifests);
            }
        }
    }

    private static void moveVirtual(
            VirtualSite source,
            VirtualSite target,
            String resource,
            long requested,
            List<Manifest> manifests) {
        long moved = Math.min(Math.max(0L, requested), source.stock(resource));
        if (moved <= 0L || source.id.equals(target.id)) return;
        source.add(resource, -moved);
        target.add(resource, moved);
        manifests.add(new Manifest(
                source.id, target.id, resource, moved,
                source.x, source.z, target.x, target.z));
    }

    private static Set<Long> livingWorkerSites(ErdenPopulationSavedData population) {
        Set<Long> result = new HashSet<>();
        for (ErdenPopulationSavedData.Household household : population.households()) {
            for (ErdenPopulationSavedData.Resident resident : household.residents()) {
                if (resident.worker() && !population.isDead(resident.id())) {
                    result.add(positionKey(resident.workX(), resident.workZ()));
                }
            }
        }
        return result;
    }

    private static List<ErdenTransportSavedData.RoutePoint> findRoute(
            ServerLevel level,
            ErdenPhysicalEconomySavedData.SiteState source,
            ErdenPhysicalEconomySavedData.SiteState target) {
        ExternalUrbanFabricBuilder.UrbanEntrance sourceEntrance = findEntrance(source.x(), source.z());
        ExternalUrbanFabricBuilder.UrbanEntrance targetEntrance = findEntrance(target.x(), target.z());
        if (sourceEntrance == null || targetEntrance == null) return List.of();
        ErdenTransportSavedData.RoutePoint start = nearestRoad(sourceEntrance.roadX(), sourceEntrance.roadZ());
        ErdenTransportSavedData.RoutePoint goal = nearestRoad(targetEntrance.roadX(), targetEntrance.roadZ());
        return start == null || goal == null ? List.of() : findRoute(level, start, goal);
    }

    private static List<ErdenTransportSavedData.RoutePoint> findRoute(
            ServerLevel level,
            ErdenTransportSavedData.RoutePoint start,
            ErdenTransportSavedData.RoutePoint goal) {
        if (start.equals(goal)) return List.of(start, goal);
        long startKey = positionKey(start.x(), start.z());
        long goalKey = positionKey(goal.x(), goal.z());
        PriorityQueue<SearchNode> open = new PriorityQueue<>(
                Comparator.comparingDouble(SearchNode::score)
                        .thenComparingLong(SearchNode::key));
        Map<Long, Integer> costs = new HashMap<>();
        Map<Long, Long> previous = new HashMap<>();
        Set<Long> closed = new HashSet<>();
        costs.put(startKey, 0);
        open.add(new SearchNode(startKey, 0, heuristic(start.x(), start.z(), goal.x(), goal.z())));

        int searched = 0;
        while (!open.isEmpty() && searched++ < MAX_ROUTE_SEARCH) {
            SearchNode current = open.remove();
            if (!closed.add(current.key())) continue;
            if (current.key() == goalKey) {
                return compressRoute(reconstruct(previous, startKey, goalKey));
            }
            int x = unpackX(current.key());
            int z = unpackZ(current.key());
            for (int[] direction : DIRECTIONS) {
                int nextX = x + direction[0];
                int nextZ = z + direction[1];
                ErdenCapitalStreamingBuilder.RoadClass road =
                        ErdenCapitalStreamingBuilder.roadClassAt(nextX, nextZ);
                if (road == ErdenCapitalStreamingBuilder.RoadClass.NONE
                        || !roadPassable(level, nextX, nextZ)) continue;
                long nextKey = positionKey(nextX, nextZ);
                if (closed.contains(nextKey)) continue;
                int step = switch (road) {
                    case ROYAL -> 8;
                    case DISTRICT -> 9;
                    case LOCAL -> 10;
                    default -> 12;
                };
                int candidate = current.cost() + step;
                if (candidate >= costs.getOrDefault(nextKey, Integer.MAX_VALUE)) continue;
                costs.put(nextKey, candidate);
                previous.put(nextKey, current.key());
                double score = candidate + heuristic(nextX, nextZ, goal.x(), goal.z()) * 8.5D;
                open.add(new SearchNode(nextKey, candidate, score));
            }
        }
        return List.of();
    }

    private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private static ErdenTransportSavedData.RoutePoint nearestRoad(int x, int z) {
        if (ErdenCapitalStreamingBuilder.roadClassAt(x, z)
                != ErdenCapitalStreamingBuilder.RoadClass.NONE) {
            return new ErdenTransportSavedData.RoutePoint(x, z);
        }
        for (int radius = 1; radius <= 12; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz : new int[]{-radius, radius}) {
                    if (ErdenCapitalStreamingBuilder.roadClassAt(x + dx, z + dz)
                            != ErdenCapitalStreamingBuilder.RoadClass.NONE) {
                        return new ErdenTransportSavedData.RoutePoint(x + dx, z + dz);
                    }
                }
            }
            for (int dz = -radius + 1; dz < radius; dz++) {
                for (int dx : new int[]{-radius, radius}) {
                    if (ErdenCapitalStreamingBuilder.roadClassAt(x + dx, z + dz)
                            != ErdenCapitalStreamingBuilder.RoadClass.NONE) {
                        return new ErdenTransportSavedData.RoutePoint(x + dx, z + dz);
                    }
                }
            }
        }
        return null;
    }

    private static boolean roadPassable(ServerLevel level, int x, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        if (!level.hasChunk(chunkX, chunkZ)
                || !ErdenCapitalStreamingBuilder.isChunkBuilt(level, chunkX, chunkZ)) {
            return true;
        }
        int surfaceY = RealmSitePlanner.surfaceY(level, x, z);
        BlockPos floor = new BlockPos(x, surfaceY, z);
        return !level.getBlockState(floor).isAir()
                && level.getBlockState(floor.above()).isAir()
                && level.getBlockState(floor.above(2)).isAir();
    }

    private static List<ErdenTransportSavedData.RoutePoint> reconstruct(
            Map<Long, Long> previous,
            long start,
            long goal) {
        List<ErdenTransportSavedData.RoutePoint> reversed = new ArrayList<>();
        long cursor = goal;
        reversed.add(new ErdenTransportSavedData.RoutePoint(unpackX(cursor), unpackZ(cursor)));
        while (cursor != start) {
            Long parent = previous.get(cursor);
            if (parent == null) return List.of();
            cursor = parent;
            reversed.add(new ErdenTransportSavedData.RoutePoint(unpackX(cursor), unpackZ(cursor)));
        }
        List<ErdenTransportSavedData.RoutePoint> route = new ArrayList<>(reversed.size());
        for (int index = reversed.size() - 1; index >= 0; index--) route.add(reversed.get(index));
        return route;
    }

    private static List<ErdenTransportSavedData.RoutePoint> compressRoute(
            List<ErdenTransportSavedData.RoutePoint> raw) {
        if (raw.size() <= 2) return raw;
        List<ErdenTransportSavedData.RoutePoint> result = new ArrayList<>();
        result.add(raw.getFirst());
        int lastDx = 0;
        int lastDz = 0;
        int sincePoint = 0;
        for (int index = 1; index < raw.size(); index++) {
            ErdenTransportSavedData.RoutePoint previous = raw.get(index - 1);
            ErdenTransportSavedData.RoutePoint current = raw.get(index);
            int dx = Integer.compare(current.x(), previous.x());
            int dz = Integer.compare(current.z(), previous.z());
            sincePoint++;
            boolean turn = index > 1 && (dx != lastDx || dz != lastDz);
            if (turn || sincePoint >= 8) {
                ErdenTransportSavedData.RoutePoint point = turn ? previous : current;
                if (!result.getLast().equals(point)) result.add(point);
                sincePoint = 0;
            }
            lastDx = dx;
            lastDz = dz;
        }
        if (!result.getLast().equals(raw.getLast())) result.add(raw.getLast());
        if (result.size() <= MAX_ROUTE_POINTS) return List.copyOf(result);
        List<ErdenTransportSavedData.RoutePoint> reduced = new ArrayList<>();
        double stride = (double) (result.size() - 1) / (MAX_ROUTE_POINTS - 1);
        for (int index = 0; index < MAX_ROUTE_POINTS; index++) {
            reduced.add(result.get((int) Math.round(index * stride)));
        }
        return List.copyOf(reduced);
    }

    private static void tickPhysicalJobs(
            ServerLevel level,
            ErdenPhysicalEconomySavedData economy,
            ErdenTransportSavedData transport) {
        for (ErdenTransportSavedData.DeliveryJob snapshot : transport.jobs()) {
            if (snapshot.terminal() || snapshot.route().isEmpty()) continue;
            ErdenTransportSavedData.DeliveryJob job = snapshot;
            ErdenTransportSavedData.RoutePoint currentPoint = job.route().get(
                    Math.min(job.waypointIndex(), job.route().size() - 1));
            if (!nearAnyPlayer(level, currentPoint.x(), currentPoint.z(), DESPAWN_RADIUS)) {
                discardEntities(level, job);
                if (!job.porterUuid().isEmpty() || !job.cartUuid().isEmpty()) {
                    transport.replaceJob(job.withoutEntities());
                }
                PROGRESS.remove(job.id());
                continue;
            }

            SpawnState spawned = ensureEntities(level, transport, job);
            job = spawned.job();
            Villager porter = spawned.porter();
            Entity cart = spawned.cart();
            if (porter == null) continue;

            if (job.status().equals("loading")) {
                if (level.getGameTime() - job.phaseTick() >= LOADING_TICKS) {
                    job = job.withStatus("moving", level.getGameTime());
                    transport.replaceJob(job);
                }
                alignCart(porter, cart);
                continue;
            }
            if (job.status().equals("unloading")) {
                alignCart(porter, cart);
                if (level.getGameTime() - job.phaseTick() >= UNLOADING_TICKS) {
                    completeJob(level, economy, transport, job);
                }
                continue;
            }

            int index = Math.min(job.waypointIndex(), job.route().size() - 1);
            ErdenTransportSavedData.RoutePoint target = job.route().get(index);
            int targetY = safeStandingY(level, target.x(), target.z());
            double distance = porter.distanceToSqr(target.x() + 0.5D, targetY, target.z() + 0.5D);
            if (distance <= 3.0D) {
                int next = index + 1;
                if (next >= job.route().size()) {
                    job = job.withStatus("unloading", level.getGameTime());
                } else {
                    job = job.withWaypoint(next, TICK_INTERVAL);
                }
                transport.replaceJob(job);
                PROGRESS.remove(job.id());
                alignCart(porter, cart);
                continue;
            }

            porter.getNavigation().moveTo(
                    target.x() + 0.5D, targetY, target.z() + 0.5D, PORTER_SPEED);
            alignCart(porter, cart);
            ProgressState progress = PROGRESS.get(job.id());
            if (progress == null || distance + 0.25D < progress.distance()) {
                PROGRESS.put(job.id(), new ProgressState(distance, 0));
            } else {
                int stalled = progress.stalledTicks() + TICK_INTERVAL;
                PROGRESS.put(job.id(), new ProgressState(progress.distance(), stalled));
                if (stalled >= STALL_TICKS) retryOrFail(level, economy, transport, job, porter);
            }
        }
    }

    private static SpawnState ensureEntities(
            ServerLevel level,
            ErdenTransportSavedData transport,
            ErdenTransportSavedData.DeliveryJob original) {
        ErdenTransportSavedData.DeliveryJob job = original;
        Entity existingPorter = resolveEntity(level, job.porterUuid());
        Villager porter = existingPorter instanceof Villager villager ? villager : null;
        Entity cart = resolveEntity(level, job.cartUuid());
        ErdenTransportSavedData.RoutePoint spawnPoint = job.route().get(
                Math.min(job.waypointIndex(), job.route().size() - 1));
        int standingY = safeStandingY(level, spawnPoint.x(), spawnPoint.z());

        if (porter == null) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(VILLAGER_ID).orElse(null);
            Entity created = type == null ? null : type.create(level, EntitySpawnReason.COMMAND);
            if (created instanceof Villager villager) {
                villager.setPos(spawnPoint.x() + 0.5D, standingY, spawnPoint.z() + 0.5D);
                villager.setCustomName(Component.literal("왕도 짐꾼"));
                villager.setCustomNameVisible(false);
                villager.setPersistenceRequired();
                if (level.addFreshEntity(villager)) {
                    porter = villager;
                    job = job.withEntities(villager.getUUID().toString(), job.cartUuid());
                    transport.replaceJob(job);
                }
            }
        }
        if (job.cart() && cart == null) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(CART_ID).orElse(null);
            Entity created = type == null ? null : type.create(level, EntitySpawnReason.COMMAND);
            if (created != null) {
                created.setPos(spawnPoint.x() + 0.5D, standingY, spawnPoint.z() + 0.5D);
                created.setCustomName(Component.literal("왕도 화물 수레"));
                created.setCustomNameVisible(false);
                if (level.addFreshEntity(created)) {
                    cart = created;
                    job = job.withEntities(job.porterUuid(), created.getUUID().toString());
                    transport.replaceJob(job);
                }
            }
        }
        return new SpawnState(job, porter, cart);
    }

    private static void alignCart(Villager porter, Entity cart) {
        if (cart == null) return;
        double dx = porter.getDeltaMovement().x;
        double dz = porter.getDeltaMovement().z;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.01D) {
            dx = Math.sin(Math.toRadians(porter.getYRot()));
            dz = -Math.cos(Math.toRadians(porter.getYRot()));
            length = Math.max(0.01D, Math.sqrt(dx * dx + dz * dz));
        }
        cart.setPos(
                porter.getX() - dx / length * 1.35D,
                porter.getY(),
                porter.getZ() - dz / length * 1.35D);
        cart.setDeltaMovement(0.0D, 0.0D, 0.0D);
    }

    private static void retryOrFail(
            ServerLevel level,
            ErdenPhysicalEconomySavedData economy,
            ErdenTransportSavedData transport,
            ErdenTransportSavedData.DeliveryJob job,
            Villager porter) {
        PROGRESS.remove(job.id());
        int attempts = job.attempts() + 1;
        ErdenPhysicalEconomySavedData.SiteState target = findSite(economy.sites(), job.targetId());
        ErdenPhysicalEconomySavedData.SiteState source = findSite(economy.sites(), job.sourceId());
        ErdenTransportSavedData.RoutePoint current = nearestRoad(
                (int) Math.floor(porter.getX()), (int) Math.floor(porter.getZ()));
        if (attempts <= MAX_RETRIES && current != null && target != null) {
            ExternalUrbanFabricBuilder.UrbanEntrance targetEntrance = findEntrance(target.x(), target.z());
            ErdenTransportSavedData.RoutePoint goal = targetEntrance == null
                    ? null : nearestRoad(targetEntrance.roadX(), targetEntrance.roadZ());
            List<ErdenTransportSavedData.RoutePoint> reroute = goal == null
                    ? List.of() : findRoute(level, current, goal);
            if (!reroute.isEmpty()) {
                transport.markBlocked();
                transport.replaceJob(job.withAttemptAndRoute(
                        attempts, reroute, Math.min(1, reroute.size() - 1), level.getGameTime()));
                if (source != null) economy.replaceSite(source.addMetric("route_retries", 1L));
                if (target != null) economy.replaceSite(target.addMetric("delivery_delays", 1L));
                return;
            }
        }
        discardEntities(level, job);
        ErdenTransportSavedData.DeliveryJob failed = job.withStatus("failed", level.getGameTime())
                .withEntities("", "");
        transport.markBlocked();
        transport.markFailed(failed);
        if (source != null) economy.replaceSite(source.addMetric("blocked_shipments", 1L));
        if (target != null) economy.replaceSite(target.addMetric("delivery_failures", 1L));
    }

    private static void completeJob(
            ServerLevel level,
            ErdenPhysicalEconomySavedData economy,
            ErdenTransportSavedData transport,
            ErdenTransportSavedData.DeliveryJob job) {
        discardEntities(level, job);
        ErdenTransportSavedData.DeliveryJob completed = job.withStatus("completed", level.getGameTime())
                .withEntities("", "");
        transport.markCompleted(completed);
        PROGRESS.remove(job.id());
        ErdenPhysicalEconomySavedData.SiteState source = findSite(economy.sites(), job.sourceId());
        ErdenPhysicalEconomySavedData.SiteState target = findSite(economy.sites(), job.targetId());
        if (source != null) economy.replaceSite(source.addMetric("transport_completed", 1L));
        if (target != null) {
            economy.replaceSite(target
                    .addMetric("transport_received", job.amount())
                    .addMetric("transport_ticks", Math.max(0L, level.getGameTime() - job.createdTick())));
        }
    }

    private static void discardEntities(ServerLevel level, ErdenTransportSavedData.DeliveryJob job) {
        Entity porter = resolveEntity(level, job.porterUuid());
        Entity cart = resolveEntity(level, job.cartUuid());
        if (porter != null) porter.discard();
        if (cart != null) cart.discard();
    }

    private static Entity resolveEntity(ServerLevel level, String uuid) {
        if (uuid == null || uuid.isBlank()) return null;
        try {
            return level.getEntity(UUID.fromString(uuid));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static int safeStandingY(ServerLevel level, int x, int z) {
        int preferred = RealmSitePlanner.surfaceY(level, x, z) + 1;
        for (int offset = 0; offset <= 8; offset++) {
            int[] candidates = offset == 0
                    ? new int[]{preferred}
                    : new int[]{preferred + offset, preferred - offset};
            for (int y : candidates) {
                BlockPos feet = new BlockPos(x, y, z);
                if (!level.getBlockState(feet.below()).isAir()
                        && level.getBlockState(feet).isAir()
                        && level.getBlockState(feet.above()).isAir()) {
                    return y;
                }
            }
        }
        return preferred;
    }

    private static boolean nearAnyPlayer(ServerLevel level, Manifest manifest, int radius) {
        int midX = (manifest.sourceX() + manifest.targetX()) / 2;
        int midZ = (manifest.sourceZ() + manifest.targetZ()) / 2;
        return nearAnyPlayer(level, manifest.sourceX(), manifest.sourceZ(), radius)
                || nearAnyPlayer(level, manifest.targetX(), manifest.targetZ(), radius)
                || nearAnyPlayer(level, midX, midZ, radius);
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

    private static double proximityScore(ServerLevel level, Manifest manifest) {
        double best = Double.MAX_VALUE;
        int midX = (manifest.sourceX() + manifest.targetX()) / 2;
        int midZ = (manifest.sourceZ() + manifest.targetZ()) / 2;
        for (ServerPlayer player : level.players()) {
            double dx = player.getX() - midX;
            double dz = player.getZ() - midZ;
            best = Math.min(best, dx * dx + dz * dz);
        }
        return best;
    }

    private static boolean requiresCart(String resource, long amount) {
        return amount >= 4L || resource.equals("wheat") || resource.equals("iron")
                || resource.equals("hay") || resource.equals("bread");
    }

    private static long estimatedTravelTicks(Manifest manifest) {
        double distance = Math.sqrt(distanceSquared(
                manifest.sourceX(), manifest.sourceZ(), manifest.targetX(), manifest.targetZ()));
        return LOADING_TICKS + UNLOADING_TICKS + Math.max(40L, Math.round(distance / PORTER_SPEED));
    }

    private static void verifyCi(
            ServerLevel level,
            ErdenPopulationSavedData population,
            ErdenPhysicalEconomySavedData economy,
            ErdenTransportSavedData transport) {
        if (ciPassed
                || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))
                || transport.lastManifestDay() < 0L
                || transport.totalManifests() <= 0L
                || transport.modeledTravelTicks() <= 0L) return;
        List<Manifest> manifests = buildManifests(population, economy.sites());
        if (manifests.isEmpty()) return;
        Manifest sample = manifests.getFirst();
        ErdenPhysicalEconomySavedData.SiteState source = findSite(economy.sites(), sample.sourceId());
        ErdenPhysicalEconomySavedData.SiteState target = findSite(economy.sites(), sample.targetId());
        if (source == null || target == null || findRoute(level, source, target).isEmpty()) return;
        ciPassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_TRANSPORT_PASS revision={} manifests={} modeled_travel_ticks={} route_planning=true loaded_obstacle_checks=true persistent_jobs=true physical_radius={} max_jobs={}",
                TRANSPORT_REVISION, manifests.size(), transport.modeledTravelTicks(),
                PHYSICAL_RADIUS, MAX_PHYSICAL_JOBS);
    }

    private static ErdenPhysicalEconomySavedData.SiteState findSite(
            List<ErdenPhysicalEconomySavedData.SiteState> sites,
            String id) {
        for (ErdenPhysicalEconomySavedData.SiteState site : sites) {
            if (site.id().equals(id)) return site;
        }
        return null;
    }

    private static ExternalUrbanFabricBuilder.UrbanEntrance findEntrance(int x, int z) {
        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance : ExternalUrbanFabricBuilder.entrances()) {
            if (entrance.x() == x && entrance.z() == z) return entrance;
        }
        return null;
    }

    private static String resourceName(String resource) {
        return switch (resource) {
            case "wheat" -> "밀";
            case "coal" -> "석탄";
            case "leather" -> "가죽";
            case "paper" -> "종이";
            case "iron" -> "철";
            case "hay" -> "건초";
            case "bread" -> "빵";
            default -> resource;
        };
    }

    private static String statusName(String status) {
        return switch (status) {
            case "loading" -> "적재 중";
            case "moving" -> "운송 중";
            case "unloading" -> "하역 중";
            case "completed" -> "배송 완료";
            case "failed" -> "경로 차단";
            default -> status;
        };
    }

    private static String siteLabel(String siteId) {
        if (siteId.contains("warehouse")) return "창고";
        if (siteId.contains("bakery")) return "제빵소";
        if (siteId.contains("shop")) return "상점";
        if (siteId.contains("inn")) return "여관";
        if (siteId.contains("stable")) return "마구간";
        if (siteId.contains("guard_post")) return "경비초소";
        if (siteId.contains("bathhouse")) return "목욕시설";
        return siteId;
    }

    private static double heuristic(int x1, int z1, int x2, int z2) {
        return Math.abs((double) x1 - x2) + Math.abs((double) z1 - z2);
    }

    private static long positionKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    private static int unpackZ(long packed) {
        return (int) packed;
    }

    private static long distanceSquared(int x1, int z1, int x2, int z2) {
        long dx = (long) x1 - x2;
        long dz = (long) z1 - z2;
        return dx * dx + dz * dz;
    }

    private record Manifest(
            String sourceId,
            String targetId,
            String resource,
            long amount,
            int sourceX,
            int sourceZ,
            int targetX,
            int targetZ) {
    }

    private record Requirement(String resource, long amount) {
    }

    private record SearchNode(long key, int cost, double score) {
    }

    private record ProgressState(double distance, int stalledTicks) {
    }

    private record SpawnState(
            ErdenTransportSavedData.DeliveryJob job,
            Villager porter,
            Entity cart) {
    }

    private static final class VirtualSite {
        final String id;
        final String role;
        final int x;
        final int z;
        final Map<String, Long> stock = new HashMap<>();

        VirtualSite(ErdenPhysicalEconomySavedData.SiteState site) {
            id = site.id();
            role = site.role();
            x = site.x();
            z = site.z();
            for (ErdenPhysicalEconomySavedData.StockEntry entry : site.stocks()) {
                if (entry.amount() > 0L) stock.put(entry.resource(), entry.amount());
            }
        }

        long stock(String resource) {
            return stock.getOrDefault(resource, 0L);
        }

        void add(String resource, long amount) {
            long next = Math.max(0L, stock(resource) + amount);
            if (next == 0L) stock.remove(resource);
            else stock.put(resource, next);
        }
    }
}
