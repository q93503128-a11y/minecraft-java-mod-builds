package kr.moonseungjun.earthtostars.ship.runtime.minecraft;

import kr.moonseungjun.earthtostars.content.EarthToStarsEntities;
import kr.moonseungjun.earthtostars.content.EarthToStarsItems;
import kr.moonseungjun.earthtostars.ship.domain.ModuleCatalog;
import kr.moonseungjun.earthtostars.ship.domain.ShipId;
import kr.moonseungjun.earthtostars.ship.domain.ShipPermission;
import kr.moonseungjun.earthtostars.ship.domain.ShipState;
import kr.moonseungjun.earthtostars.ship.gameplay.LaunchCraftBlueprint;
import kr.moonseungjun.earthtostars.ship.networking.ShipControlInputPayload;
import kr.moonseungjun.earthtostars.ship.networking.ShipControlSessionPayload;
import kr.moonseungjun.earthtostars.ship.persistence.ShipBootstrapCatalog;
import kr.moonseungjun.earthtostars.ship.persistence.minecraft.InteriorSavedData;
import kr.moonseungjun.earthtostars.ship.persistence.minecraft.ShipSavedData;
import kr.moonseungjun.earthtostars.ship.persistence.minecraft.ShipSystemsSavedData;
import kr.moonseungjun.earthtostars.ship.runtime.ShipControlInput;
import kr.moonseungjun.earthtostars.ship.runtime.ShipControlLease;
import kr.moonseungjun.earthtostars.ship.runtime.ShipFlightRuntime;
import kr.moonseungjun.earthtostars.ship.runtime.ShipFlightTuning;
import kr.moonseungjun.earthtostars.ship.runtime.ShipRepository;
import kr.moonseungjun.earthtostars.ship.runtime.ShipTransform;
import kr.moonseungjun.earthtostars.ship.runtime.ShipTransitionPolicy;
import kr.moonseungjun.earthtostars.ship.runtime.ShipVec3;
import kr.moonseungjun.earthtostars.space.SpaceLevels;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class ShipRuntimeManager {
    private static final double CONTROL_RANGE_SQUARED = 8.0D * 8.0D;
    private static final double RETIRE_MAX_SPEED_SQUARED = 0.01D;
    private static final long READINESS_WARNING_COOLDOWN_TICKS = 100L;
    private static final Map<Integer, Entry> ENTRIES = new LinkedHashMap<>();
    private static final Map<ShipId, Long> LAST_READINESS_WARNING = new LinkedHashMap<>();
    private static final ShipRepository REPOSITORY = new ShipRepository();
    private static final ModuleCatalog CATALOG = ShipBootstrapCatalog.create();

    private ShipRuntimeManager() {
    }

    public static void initialize(MinecraftServer server) {
        ENTRIES.clear();
        LAST_READINESS_WARNING.clear();
        REPOSITORY.clear();
        for (ShipState ship : ShipSavedData.get(server).decodeAll(CATALOG)) {
            REPOSITORY.add(ship);
        }
    }

    public static int spawnAndControl(ServerPlayer player, ServerLevel level, long tick) {
        ShipState existing = REPOSITORY.findOwnedBy(player.getUUID()).orElse(null);
        if (existing != null) {
            if (!restoreAndControl(player, level, tick)) {
                throw new IllegalStateException("failed to restore owned ship exterior");
            }
            return player.getVehicle() == null ? -1 : player.getVehicle().getId();
        }
        Vec3 spawn = player.position().add(player.getLookAngle().scale(4.0D)).add(0.0D, 1.0D, 0.0D);
        Entry entry = createStarterCraft(player, level, spawn);
        if (entry == null || !boardAndControl(player, entry.exterior(), tick)) {
            throw new IllegalStateException("failed to create and board ship exterior");
        }
        return entry.exterior().getId();
    }

    /**
     * Survival construction creates the craft but never fakes a pilot seat. The
     * player boards the actual vehicle shell by right-clicking it.
     */
    public static LaunchDeploymentResult deployLaunchCraft(ServerPlayer player, ServerLevel level, Vec3 spawn, long tick) {
        if (REPOSITORY.findOwnedBy(player.getUUID()).isPresent()) {
            return LaunchDeploymentResult.ALREADY_OWNS_CRAFT;
        }
        return createStarterCraft(player, level, spawn) == null
                ? LaunchDeploymentResult.DEPLOYMENT_FAILED
                : LaunchDeploymentResult.DEPLOYED;
    }

    public static boolean restoreAndControl(ServerPlayer player, ServerLevel level, long tick) {
        ShipState ship = REPOSITORY.findOwnedBy(player.getUUID()).orElse(null);
        if (ship == null) {
            return false;
        }
        Entry live = findEntry(ship.shipId()).orElse(null);
        if (live != null) {
            return boardAndControl(player, live.exterior(), tick);
        }

        Vec3 spawn = player.position().add(player.getLookAngle().scale(4.0D)).add(0.0D, 1.0D, 0.0D);
        VehiclePair pair = createVehiclePair(level, spawn, player.getYRot(), 0.0F);
        if (pair == null) {
            return false;
        }
        ShipFlightRuntime runtime = new ShipFlightRuntime(
                ship,
                new ShipTransform(new ShipVec3(spawn.x, spawn.y, spawn.z), ShipVec3.ZERO, player.getYRot(), 0.0D),
                ShipFlightTuning.P0
        );
        Entry entry = new Entry(runtime, pair.exterior(), pair.visual());
        ENTRIES.put(pair.exterior().getId(), entry);
        return boardAndControl(player, pair.exterior(), tick);
    }

    public static boolean boardAndControl(ServerPlayer player, ShipExteriorEntity exterior) {
        return boardAndControl(player, exterior, player.level().getGameTime());
    }

    private static boolean boardAndControl(ServerPlayer player, ShipExteriorEntity exterior, long tick) {
        Entry entry = ENTRIES.get(exterior.getId());
        if (entry == null || entry.exterior() != exterior || exterior.isRemoved()) {
            return false;
        }
        if (exterior.level() != player.level() || exterior.distanceToSqr(player) > CONTROL_RANGE_SQUARED) {
            player.sendSystemMessage(Component.translatable("message.earth_to_stars.ship.too_far"), true);
            return false;
        }
        if (!entry.runtime().ship().can(player.getUUID(), ShipPermission.PILOT)) {
            player.sendSystemMessage(Component.translatable("message.earth_to_stars.ship.no_pilot_permission"), true);
            return false;
        }
        if (!exterior.getPassengers().isEmpty() && exterior.getFirstPassenger() != player) {
            player.sendSystemMessage(Component.translatable("message.earth_to_stars.ship.seat_occupied"), true);
            return false;
        }

        boolean newlyMounted = player.getVehicle() != exterior;
        if (newlyMounted && !player.startRiding(exterior)) {
            return false;
        }
        if (!grantControl(player, entry, tick)) {
            if (newlyMounted) {
                player.stopRiding();
            }
            return false;
        }
        player.sendSystemMessage(Component.translatable("message.earth_to_stars.ship.boarded"), true);
        return true;
    }

    public static boolean controlNearest(ServerPlayer player, long tick) {
        Entry nearest = ENTRIES.values().stream()
                .filter(entry -> !entry.exterior().isRemoved())
                .filter(entry -> entry.exterior().level() == player.level())
                .filter(entry -> entry.runtime().ship().can(player.getUUID(), ShipPermission.PILOT))
                .filter(entry -> entry.exterior().distanceToSqr(player) <= CONTROL_RANGE_SQUARED)
                .min((left, right) -> Double.compare(
                        left.exterior().distanceToSqr(player),
                        right.exterior().distanceToSqr(player)
                ))
                .orElse(null);
        return nearest != null && boardAndControl(player, nearest.exterior(), tick);
    }

    static Optional<ShipState> accessibleShip(ServerPlayer player, ShipPermission permission) {
        Optional<ShipId> interiorShip = ShipInteriorManager.linkedShip(player);
        if (interiorShip.isPresent()) {
            return REPOSITORY.find(interiorShip.orElseThrow())
                    .filter(ship -> ship.can(player.getUUID(), permission));
        }
        return ENTRIES.values().stream()
                .filter(entry -> !entry.exterior().isRemoved())
                .filter(entry -> entry.exterior().level() == player.level())
                .filter(entry -> entry.runtime().ship().can(player.getUUID(), permission))
                .filter(entry -> entry.exterior().distanceToSqr(player) <= CONTROL_RANGE_SQUARED)
                .min((left, right) -> Double.compare(
                        left.exterior().distanceToSqr(player),
                        right.exterior().distanceToSqr(player)
                ))
                .map(entry -> entry.runtime().ship());
    }

    static Optional<ShipState> nearestInteriorAccessible(ServerPlayer player) {
        return accessibleShip(player, ShipPermission.INTERIOR_ACCESS);
    }

    static List<ShipState> liveShips() {
        return ENTRIES.values().stream()
                .filter(entry -> !entry.exterior().isRemoved())
                .map(entry -> entry.runtime().ship())
                .toList();
    }

    static Optional<ExteriorAnchor> exteriorAnchor(ShipId shipId) {
        return findEntry(shipId)
                .map(entry -> new ExteriorAnchor((ServerLevel) entry.exterior().level(), entry.runtime().transform()));
    }

    static List<ServerPlayer> activeCrewPlayers(MinecraftServer server, ShipId shipId) {
        Map<UUID, ServerPlayer> active = new LinkedHashMap<>();
        for (Entry entry : ENTRIES.values()) {
            if (!entry.exterior().isRemoved() && entry.runtime().ship().shipId().equals(shipId)) {
                entry.runtime().lease()
                        .map(ShipControlLease::controllerId)
                        .map(server.getPlayerList()::getPlayer)
                        .ifPresent(player -> active.put(player.getUUID(), player));
            }
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ShipInteriorManager.linkedShip(player)
                    .filter(shipId::equals)
                    .ifPresent(ignored -> active.put(player.getUUID(), player));
        }
        return List.copyOf(active.values());
    }

    static int activeCrewCount(MinecraftServer server, ShipId shipId) {
        return activeCrewPlayers(server, shipId).size();
    }

    public static boolean releaseController(ServerPlayer player) {
        boolean released = false;
        for (Map.Entry<Integer, Entry> mapEntry : ENTRIES.entrySet()) {
            if (mapEntry.getValue().runtime().releaseControl(player.getUUID())) {
                PacketDistributor.sendToPlayer(player, ShipControlSessionPayload.inactive(mapEntry.getKey()));
                released = true;
            }
        }
        if (player.getVehicle() instanceof ShipExteriorEntity) {
            player.stopRiding();
        }
        return released;
    }

    public static void releaseController(UUID playerId) {
        for (Entry entry : ENTRIES.values()) {
            entry.runtime().releaseControl(playerId);
        }
    }

    public static void acceptControlInput(ServerPlayer player, ShipControlInputPayload payload) {
        Entry entry = ENTRIES.get(payload.entityId());
        if (entry == null || entry.exterior().isRemoved()) {
            return;
        }
        if (entry.exterior().level() != player.level() || player.getVehicle() != entry.exterior()) {
            return;
        }

        ShipControlInput input;
        try {
            input = new ShipControlInput(payload.throttle(), payload.yaw(), payload.pitch());
        } catch (IllegalArgumentException rejectedInput) {
            return;
        }
        entry.runtime().acceptInput(
                player.getUUID(),
                payload.sessionId(),
                payload.sequence(),
                input,
                player.level().getGameTime()
        );
    }

    public static void tick(MinecraftServer server) {
        List<Integer> removed = new ArrayList<>();
        List<TransitionRequest> transitions = new ArrayList<>();
        for (Map.Entry<Integer, Entry> mapEntry : List.copyOf(ENTRIES.entrySet())) {
            int entityId = mapEntry.getKey();
            Entry entry = mapEntry.getValue();
            if (entry.exterior().isRemoved()) {
                SpaceVisualFactory.discard(entry.visual());
                removed.add(entityId);
                continue;
            }

            validateMountedController(server, entityId, entry);
            long gameTime = entry.exterior().level().getGameTime();
            ExteriorAnchor anchorBeforeTick = new ExteriorAnchor((ServerLevel) entry.exterior().level(), entry.runtime().transform());
            boolean propulsionPowered = ShipSystemsManager.allowPropulsion(
                    entry.runtime().ship(),
                    entry.runtime().currentInput(),
                    anchorBeforeTick
            );
            Optional<UUID> expiredController = entry.runtime().tick(gameTime, propulsionPowered);
            expiredController.ifPresent(playerId -> {
                ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                if (player != null) {
                    PacketDistributor.sendToPlayer(player, ShipControlSessionPayload.inactive(entityId));
                }
            });

            ShipTransform transform = entry.runtime().transform();
            ShipTransitionPolicy.Transition transition = ShipTransitionPolicy.evaluate(
                    entry.exterior().level().dimension().equals(Level.OVERWORLD),
                    entry.exterior().level().dimension().equals(SpaceLevels.ORBITAL_SPACE),
                    transform
            );
            if (transition == ShipTransitionPolicy.Transition.EARTH_TO_ORBIT
                    && !ShipSystemsManager.canEnterOrbit(entry.runtime().ship())) {
                ShipTransform held = holdBelowOrbitBoundary(transform);
                entry.runtime().relocate(held);
                applyTransform(entry, held);
                warnReadiness(server, entry, gameTime);
                continue;
            }
            if (transition != ShipTransitionPolicy.Transition.NONE && entry.runtime().lease().isPresent()) {
                transitions.add(new TransitionRequest(entityId, entry, transition));
                continue;
            }

            applyTransform(entry, transform);
        }
        removed.forEach(ENTRIES::remove);
        for (TransitionRequest request : transitions) {
            executeTransition(server, request);
        }
    }

    public static boolean retireCraft(ServerPlayer player, ShipExteriorEntity exterior) {
        Entry entry = ENTRIES.get(exterior.getId());
        if (entry == null || entry.exterior() != exterior) {
            return false;
        }
        ShipState ship = entry.runtime().ship();
        if (!ship.ownerId().equals(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.earth_to_stars.ship.retire_owner_only"), true);
            return false;
        }
        if (!exterior.level().dimension().equals(Level.OVERWORLD)) {
            player.sendSystemMessage(Component.translatable("message.earth_to_stars.ship.retire_earth_only"), true);
            return false;
        }
        if (entry.runtime().transform().velocity().lengthSquared() > RETIRE_MAX_SPEED_SQUARED) {
            player.sendSystemMessage(Component.translatable("message.earth_to_stars.ship.retire_moving"), true);
            return false;
        }
        if (!exterior.getPassengers().isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.earth_to_stars.ship.retire_occupied"), true);
            return false;
        }
        boolean otherCrewInside = activeCrewPlayers(player.level().getServer(), ship.shipId()).stream()
                .anyMatch(crew -> !crew.getUUID().equals(player.getUUID()));
        if (otherCrewInside) {
            player.sendSystemMessage(Component.translatable("message.earth_to_stars.ship.retire_occupied"), true);
            return false;
        }

        int entityId = exterior.getId();
        entry.runtime().releaseControl(player.getUUID());
        PacketDistributor.sendToPlayer(player, ShipControlSessionPayload.inactive(entityId));
        ENTRIES.remove(entityId);
        discardPair(entry);

        MinecraftServer server = player.level().getServer();
        ShipId shipId = ship.shipId();
        REPOSITORY.remove(shipId);
        ShipSystemsManager.removeShip(shipId);
        ShipTurretManager.removeShip(shipId);
        OrbitalMissionManager.removeShip(shipId);
        LAST_READINESS_WARNING.remove(shipId);
        ShipSavedData.get(server).remove(shipId);
        ShipSystemsSavedData.get(server).remove(shipId);
        InteriorSavedData.get(server).release(shipId);
        ShipSystemsManager.flush(server);

        ItemStack packed = new ItemStack(EarthToStarsItems.LAUNCH_CRAFT_KIT.get());
        if (!player.getInventory().add(packed)) {
            player.drop(packed, false);
        }
        player.sendSystemMessage(Component.translatable("message.earth_to_stars.ship.retired"));
        return true;
    }

    public static void prepareForShutdown() {
        for (Entry entry : ENTRIES.values()) {
            SpaceVisualFactory.discard(entry.visual());
        }
    }

    public static void clear() {
        for (Entry entry : ENTRIES.values()) {
            discardPair(entry);
        }
        ENTRIES.clear();
        LAST_READINESS_WARNING.clear();
        REPOSITORY.clear();
    }

    private static Entry createStarterCraft(ServerPlayer player, ServerLevel level, Vec3 spawn) {
        ShipState ship = ShipState.create(ShipId.random(), player.getUUID(), LaunchCraftBlueprint.slots());
        LaunchCraftBlueprint.installStarterModules(ship, CATALOG);

        VehiclePair pair = createVehiclePair(level, spawn, player.getYRot(), 0.0F);
        if (pair == null) {
            return null;
        }

        try {
            REPOSITORY.add(ship);
            ShipSavedData.get(level.getServer()).put(ship);
            ShipSystemsManager.systems(ship);
            ShipSystemsManager.flush(level.getServer());
        } catch (RuntimeException failure) {
            discardPair(pair);
            REPOSITORY.remove(ship.shipId());
            throw failure;
        }

        ShipTransform transform = new ShipTransform(
                new ShipVec3(spawn.x, spawn.y, spawn.z),
                ShipVec3.ZERO,
                player.getYRot(),
                0.0D
        );
        ShipFlightRuntime runtime = new ShipFlightRuntime(ship, transform, ShipFlightTuning.P0);
        Entry entry = new Entry(runtime, pair.exterior(), pair.visual());
        ENTRIES.put(pair.exterior().getId(), entry);
        applyTransform(entry, transform);
        return entry;
    }

    private static VehiclePair createVehiclePair(ServerLevel level, Vec3 position, float yaw, float pitch) {
        ShipExteriorEntity exterior = new ShipExteriorEntity(EarthToStarsEntities.SHIP_EXTERIOR.get(), level);
        exterior.setPos(position.x, position.y, position.z);
        exterior.setYRot(yaw);
        exterior.setXRot(pitch);
        if (!level.addFreshEntity(exterior)) {
            return null;
        }

        Display.ItemDisplay visual = SpaceVisualFactory.create(
                level,
                EarthToStarsItems.STARTER_CRAFT_VISUAL.get(),
                new ShipVec3(position.x, position.y, position.z),
                yaw,
                pitch
        );
        if (!level.addFreshEntity(visual)) {
            exterior.discard();
            return null;
        }
        return new VehiclePair(exterior, visual);
    }

    private static void executeTransition(MinecraftServer server, TransitionRequest request) {
        Entry entry = ENTRIES.get(request.entityId());
        if (entry == null || entry != request.entry() || entry.exterior().isRemoved()) {
            return;
        }
        ShipControlLease lease = entry.runtime().lease().orElse(null);
        if (lease == null) {
            return;
        }
        ServerPlayer pilot = server.getPlayerList().getPlayer(lease.controllerId());
        if (pilot == null || pilot.level() != entry.exterior().level() || pilot.getVehicle() != entry.exterior()) {
            return;
        }

        ServerLevel origin = (ServerLevel) entry.exterior().level();
        ServerLevel target = server.getLevel(
                request.transition() == ShipTransitionPolicy.Transition.EARTH_TO_ORBIT
                        ? SpaceLevels.ORBITAL_SPACE
                        : Level.OVERWORLD
        );
        if (target == null) {
            pilot.sendSystemMessage(Component.literal("항로를 열 수 없습니다. 우주 구역이 서버에 등록되지 않았습니다."));
            return;
        }

        ShipTransform oldTransform = entry.runtime().transform();
        ShipTransform destination = ShipTransitionPolicy.destination(request.transition(), oldTransform);
        Vec3 oldPlayerPosition = pilot.position();
        float oldPlayerYaw = pilot.getYRot();
        float oldPlayerPitch = pilot.getXRot();

        pilot.stopRiding();
        entry.runtime().releaseControl(pilot.getUUID());
        PacketDistributor.sendToPlayer(pilot, ShipControlSessionPayload.inactive(request.entityId()));

        Vec3 pilotDestination = new Vec3(
                destination.position().x(),
                destination.position().y() + 1.0D,
                destination.position().z()
        );
        ServerPlayer teleported = pilot.teleport(new TeleportTransition(
                target,
                pilotDestination,
                Vec3.ZERO,
                oldPlayerYaw,
                oldPlayerPitch,
                Set.of(),
                TeleportTransition.DO_NOTHING
        ));
        if (teleported == null) {
            pilot.startRiding(entry.exterior());
            grantControl(pilot, entry, origin.getGameTime());
            return;
        }

        VehiclePair nextPair = createVehiclePair(
                target,
                new Vec3(destination.position().x(), destination.position().y(), destination.position().z()),
                (float) destination.yawDegrees(),
                (float) destination.pitchDegrees()
        );
        if (nextPair == null || !teleported.startRiding(nextPair.exterior())) {
            if (nextPair != null) {
                discardPair(nextPair);
            }
            rollbackPilot(teleported, origin, oldPlayerPosition, oldPlayerYaw, oldPlayerPitch, entry);
            return;
        }

        entry.runtime().relocate(destination);
        Entry nextEntry = new Entry(entry.runtime(), nextPair.exterior(), nextPair.visual());
        if (!grantControl(teleported, nextEntry, target.getGameTime())) {
            teleported.stopRiding();
            discardPair(nextPair);
            entry.runtime().relocate(oldTransform);
            rollbackPilot(teleported, origin, oldPlayerPosition, oldPlayerYaw, oldPlayerPitch, entry);
            return;
        }

        ENTRIES.remove(request.entityId());
        discardPair(entry);
        ENTRIES.put(nextPair.exterior().getId(), nextEntry);
        applyTransform(nextEntry, destination);
        ShipSavedData.get(server).put(entry.runtime().ship());
        ShipSystemsManager.flush(server);
        teleported.sendSystemMessage(Component.translatable(
                request.transition() == ShipTransitionPolicy.Transition.EARTH_TO_ORBIT
                        ? "message.earth_to_stars.flight.orbit_entered"
                        : "message.earth_to_stars.flight.earth_returned"
        ));
    }

    private static void rollbackPilot(
            ServerPlayer player,
            ServerLevel origin,
            Vec3 position,
            float yaw,
            float pitch,
            Entry entry
    ) {
        ServerPlayer returned = player.teleport(new TeleportTransition(
                origin,
                position,
                Vec3.ZERO,
                yaw,
                pitch,
                Set.of(),
                TeleportTransition.DO_NOTHING
        ));
        if (returned != null && returned.startRiding(entry.exterior())) {
            grantControl(returned, entry, origin.getGameTime());
        }
    }

    private static void validateMountedController(MinecraftServer server, int entityId, Entry entry) {
        ShipControlLease lease = entry.runtime().lease().orElse(null);
        if (lease == null) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(lease.controllerId());
        if (player != null && player.getVehicle() == entry.exterior() && player.level() == entry.exterior().level()) {
            return;
        }
        entry.runtime().releaseControl(lease.controllerId());
        if (player != null) {
            PacketDistributor.sendToPlayer(player, ShipControlSessionPayload.inactive(entityId));
        }
    }

    private static void warnReadiness(MinecraftServer server, Entry entry, long tick) {
        ShipId shipId = entry.runtime().ship().shipId();
        long last = LAST_READINESS_WARNING.getOrDefault(shipId, Long.MIN_VALUE / 2L);
        if (tick - last < READINESS_WARNING_COOLDOWN_TICKS) {
            return;
        }
        LAST_READINESS_WARNING.put(shipId, tick);
        ShipSystemsManager.OrbitReadiness readiness = ShipSystemsManager.orbitReadiness(entry.runtime().ship());
        entry.runtime().lease().map(ShipControlLease::controllerId)
                .map(server.getPlayerList()::getPlayer)
                .ifPresent(player -> player.sendSystemMessage(Component.literal(String.format(
                        Locale.ROOT,
                        "궤도 진입 준비 부족 — 추진제 %.0f/%.0f · 산소 %.0f/%.0f · 생명유지 %s",
                        readiness.propellant(),
                        readiness.requiredPropellant(),
                        readiness.oxygen(),
                        readiness.requiredOxygen(),
                        readiness.lifeSupportInstalled() ? "정상" : "없음"
                )), true));
    }

    private static ShipTransform holdBelowOrbitBoundary(ShipTransform current) {
        return new ShipTransform(
                new ShipVec3(
                        current.position().x(),
                        Math.min(current.position().y(), ShipTransitionPolicy.EARTH_EXIT_ALTITUDE - 1.0D),
                        current.position().z()
                ),
                new ShipVec3(current.velocity().x(), Math.min(0.0D, current.velocity().y()), current.velocity().z()),
                current.yawDegrees(),
                current.pitchDegrees()
        );
    }

    private static void applyTransform(Entry entry, ShipTransform transform) {
        entry.exterior().setDeltaMovement(Vec3.ZERO);
        entry.exterior().setPos(transform.position().x(), transform.position().y(), transform.position().z());
        entry.exterior().setYRot((float) transform.yawDegrees());
        entry.exterior().setXRot((float) transform.pitchDegrees());
        SpaceVisualFactory.apply(entry.visual(), transform);
    }

    private static boolean grantControl(ServerPlayer player, Entry entry, long tick) {
        if (player.getVehicle() != entry.exterior()) {
            return false;
        }
        Optional<UUID> session = entry.runtime().requestControl(player.getUUID(), tick);
        if (session.isEmpty()) {
            return false;
        }
        PacketDistributor.sendToPlayer(
                player,
                ShipControlSessionPayload.active(entry.exterior().getId(), session.orElseThrow())
        );
        return true;
    }

    private static Optional<Entry> findEntry(ShipId shipId) {
        return ENTRIES.values().stream()
                .filter(entry -> !entry.exterior().isRemoved())
                .filter(entry -> entry.runtime().ship().shipId().equals(shipId))
                .findFirst();
    }

    private static void discardPair(Entry entry) {
        if (!entry.exterior().isRemoved()) {
            entry.exterior().discard();
        }
        SpaceVisualFactory.discard(entry.visual());
    }

    private static void discardPair(VehiclePair pair) {
        if (!pair.exterior().isRemoved()) {
            pair.exterior().discard();
        }
        SpaceVisualFactory.discard(pair.visual());
    }

    public enum LaunchDeploymentResult {
        DEPLOYED,
        ALREADY_OWNS_CRAFT,
        CONTROL_UNAVAILABLE,
        DEPLOYMENT_FAILED
    }

    record ExteriorAnchor(ServerLevel level, ShipTransform transform) {
    }

    private record Entry(ShipFlightRuntime runtime, ShipExteriorEntity exterior, Display.ItemDisplay visual) {
    }

    private record VehiclePair(ShipExteriorEntity exterior, Display.ItemDisplay visual) {
    }

    private record TransitionRequest(int entityId, Entry entry, ShipTransitionPolicy.Transition transition) {
    }
}
