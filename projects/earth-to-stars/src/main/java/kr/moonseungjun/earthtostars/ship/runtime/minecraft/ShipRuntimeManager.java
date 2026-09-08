package kr.moonseungjun.earthtostars.ship.runtime.minecraft;

import kr.moonseungjun.earthtostars.ship.domain.ModuleCatalog;
import kr.moonseungjun.earthtostars.ship.domain.ShipId;
import kr.moonseungjun.earthtostars.ship.domain.ShipPermission;
import kr.moonseungjun.earthtostars.ship.domain.ShipState;
import kr.moonseungjun.earthtostars.ship.gameplay.LaunchCraftBlueprint;
import kr.moonseungjun.earthtostars.ship.networking.ShipControlInputPayload;
import kr.moonseungjun.earthtostars.ship.networking.ShipControlSessionPayload;
import kr.moonseungjun.earthtostars.ship.persistence.ShipBootstrapCatalog;
import kr.moonseungjun.earthtostars.ship.persistence.minecraft.ShipSavedData;
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
import net.minecraft.world.entity.decoration.ArmorStand;
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
    private static final double CONTROL_RANGE_SQUARED = 64.0D * 64.0D;
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

    /**
     * P0 command-side spawn path. Kept for technical recovery, but it now uses the
     * same M1 starter blueprint so command-created craft cannot diverge from the
     * real survival construction contract.
     */
    public static int spawnAndControl(ServerPlayer player, ServerLevel level, long tick) {
        removeOwnedCraft(player.getUUID());
        Vec3 spawn = player.position().add(player.getLookAngle().scale(4.0D)).add(0.0D, 1.0D, 0.0D);
        Entry entry = createStarterCraft(player, level, spawn);
        if (entry == null) {
            throw new IllegalStateException("failed to add ship exterior proxy to level");
        }
        grantControl(player, entry, tick);
        return entry.exterior().getId();
    }

    /**
     * Player-facing M1 construction path. A crafted launch package creates one
     * authoritative starter craft, persists it immediately, and grants the owner
     * the normal server-issued pilot lease. Existing ownership is never overwritten.
     */
    public static LaunchDeploymentResult deployLaunchCraft(ServerPlayer player, ServerLevel level, Vec3 spawn, long tick) {
        if (REPOSITORY.findOwnedBy(player.getUUID()).isPresent()) {
            return LaunchDeploymentResult.ALREADY_OWNS_CRAFT;
        }
        Entry entry = createStarterCraft(player, level, spawn);
        if (entry == null) {
            return LaunchDeploymentResult.DEPLOYMENT_FAILED;
        }
        if (!grantControl(player, entry, tick)) {
            return LaunchDeploymentResult.CONTROL_UNAVAILABLE;
        }
        return LaunchDeploymentResult.DEPLOYED;
    }

    public static boolean restoreAndControl(ServerPlayer player, ServerLevel level, long tick) {
        ShipState ship = REPOSITORY.findOwnedBy(player.getUUID()).orElse(null);
        if (ship == null) {
            return false;
        }
        removeOwnedCraft(player.getUUID());
        Vec3 spawn = player.position().add(player.getLookAngle().scale(4.0D)).add(0.0D, 1.0D, 0.0D);
        ArmorStand exterior = createExterior(level, spawn, player.getYRot(), player.getXRot());
        if (!level.addFreshEntity(exterior)) {
            return false;
        }
        ShipFlightRuntime runtime = new ShipFlightRuntime(
                ship,
                new ShipTransform(new ShipVec3(spawn.x, spawn.y, spawn.z), ShipVec3.ZERO, player.getYRot(), 0.0D),
                ShipFlightTuning.P0
        );
        Entry entry = new Entry(runtime, exterior);
        ENTRIES.put(exterior.getId(), entry);
        return grantControl(player, entry, tick);
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
        return nearest != null && grantControl(player, nearest, tick);
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
        return ENTRIES.values().stream()
                .filter(entry -> !entry.exterior().isRemoved())
                .filter(entry -> entry.runtime().ship().shipId().equals(shipId))
                .findFirst()
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
        if (entry.exterior().level() != player.level() || entry.exterior().distanceToSqr(player) > CONTROL_RANGE_SQUARED) {
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
        for (Map.Entry<Integer, Entry> mapEntry : ENTRIES.entrySet()) {
            int entityId = mapEntry.getKey();
            Entry entry = mapEntry.getValue();
            if (entry.exterior().isRemoved()) {
                removed.add(entityId);
                continue;
            }

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
                applyTransform(entry.exterior(), held);
                tetherController(server, entry, held);
                warnReadiness(server, entry, gameTime);
                continue;
            }
            if (transition != ShipTransitionPolicy.Transition.NONE && entry.runtime().lease().isPresent()) {
                transitions.add(new TransitionRequest(entityId, entry, transition));
                continue;
            }

            applyTransform(entry.exterior(), transform);
            tetherController(server, entry, transform);
        }
        removed.forEach(ENTRIES::remove);
        for (TransitionRequest request : transitions) {
            executeTransition(server, request);
        }
    }

    public static void clear() {
        ENTRIES.clear();
        LAST_READINESS_WARNING.clear();
        REPOSITORY.clear();
    }

    private static Entry createStarterCraft(ServerPlayer player, ServerLevel level, Vec3 spawn) {
        ShipState ship = ShipState.create(ShipId.random(), player.getUUID(), LaunchCraftBlueprint.slots());
        LaunchCraftBlueprint.installStarterModules(ship, CATALOG);

        ArmorStand exterior = createExterior(level, spawn, player.getYRot(), 0.0F);
        if (!level.addFreshEntity(exterior)) {
            return null;
        }

        try {
            REPOSITORY.add(ship);
            ShipSavedData.get(level.getServer()).put(ship);
            ShipSystemsManager.systems(ship);
            ShipSystemsManager.flush(level.getServer());
        } catch (RuntimeException failure) {
            exterior.discard();
            throw failure;
        }

        ShipTransform transform = new ShipTransform(
                new ShipVec3(spawn.x, spawn.y, spawn.z),
                ShipVec3.ZERO,
                player.getYRot(),
                0.0D
        );
        ShipFlightRuntime runtime = new ShipFlightRuntime(ship, transform, ShipFlightTuning.P0);
        Entry entry = new Entry(runtime, exterior);
        ENTRIES.put(exterior.getId(), entry);
        return entry;
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
        if (pilot == null || pilot.level() != entry.exterior().level()) {
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

        entry.runtime().releaseControl(pilot.getUUID());
        PacketDistributor.sendToPlayer(pilot, ShipControlSessionPayload.inactive(request.entityId()));

        Vec3 pilotDestination = new Vec3(
                destination.position().x(),
                destination.position().y() + 1.5D,
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
            grantControl(pilot, entry, origin.getGameTime());
            return;
        }

        ArmorStand nextExterior = createExterior(
                target,
                new Vec3(destination.position().x(), destination.position().y(), destination.position().z()),
                (float) destination.yawDegrees(),
                (float) destination.pitchDegrees()
        );
        if (!target.addFreshEntity(nextExterior)) {
            teleported.teleport(new TeleportTransition(
                    origin,
                    oldPlayerPosition,
                    Vec3.ZERO,
                    oldPlayerYaw,
                    oldPlayerPitch,
                    Set.of(),
                    TeleportTransition.DO_NOTHING
            ));
            grantControl(pilot, entry, origin.getGameTime());
            return;
        }

        entry.runtime().relocate(destination);
        entry.exterior().discard();
        ENTRIES.remove(request.entityId());
        Entry nextEntry = new Entry(entry.runtime(), nextExterior);
        ENTRIES.put(nextExterior.getId(), nextEntry);
        applyTransform(nextExterior, destination);
        ShipSavedData.get(server).put(entry.runtime().ship());
        ShipSystemsManager.flush(server);
        grantControl(teleported, nextEntry, target.getGameTime());
        teleported.sendSystemMessage(Component.translatable(
                request.transition() == ShipTransitionPolicy.Transition.EARTH_TO_ORBIT
                        ? "message.earth_to_stars.flight.orbit_entered"
                        : "message.earth_to_stars.flight.earth_returned"
        ));
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
                        "궤도 진입 준비 부족 — 추진제 %.0f / %.0f, 산소 %.0f / %.0f, 생명유지 %s",
                        readiness.propellant(),
                        readiness.requiredPropellant(),
                        readiness.oxygen(),
                        readiness.requiredOxygen(),
                        readiness.lifeSupportInstalled() ? "정상" : "없음"
                ))));
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

    private static void applyTransform(ArmorStand exterior, ShipTransform transform) {
        exterior.setDeltaMovement(Vec3.ZERO);
        exterior.setPos(transform.position().x(), transform.position().y(), transform.position().z());
        exterior.setYRot((float) transform.yawDegrees());
        exterior.setXRot((float) transform.pitchDegrees());
    }

    /**
     * M1 technical cockpit tether. Until the production cockpit/interior camera is
     * implemented, the controlling player is kept with the authoritative exterior
     * so a complete ascent and re-entry can actually be exercised without the ship
     * flying outside the 64-block input authority range.
     */
    private static void tetherController(MinecraftServer server, Entry entry, ShipTransform transform) {
        entry.runtime().lease()
                .map(ShipControlLease::controllerId)
                .map(server.getPlayerList()::getPlayer)
                .filter(player -> player.level() == entry.exterior().level())
                .ifPresent(player -> {
                    player.setDeltaMovement(Vec3.ZERO);
                    player.setPos(
                            transform.position().x(),
                            transform.position().y() + 1.5D,
                            transform.position().z()
                    );
                });
    }

    private static ArmorStand createExterior(ServerLevel level, Vec3 position, float yaw, float pitch) {
        ArmorStand exterior = new ArmorStand(level, position.x, position.y, position.z);
        exterior.setNoGravity(true);
        exterior.setInvulnerable(true);
        exterior.setCustomName(Component.literal("개척선 선체"));
        exterior.setCustomNameVisible(true);
        exterior.setYRot(yaw);
        exterior.setXRot(pitch);
        return exterior;
    }

    private static boolean grantControl(ServerPlayer player, Entry entry, long tick) {
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

    private static void removeOwnedCraft(UUID ownerId) {
        List<Integer> removed = new ArrayList<>();
        for (Map.Entry<Integer, Entry> mapEntry : ENTRIES.entrySet()) {
            if (mapEntry.getValue().runtime().ship().ownerId().equals(ownerId)) {
                mapEntry.getValue().exterior().discard();
                removed.add(mapEntry.getKey());
            }
        }
        removed.forEach(ENTRIES::remove);
    }

    public enum LaunchDeploymentResult {
        DEPLOYED,
        ALREADY_OWNS_CRAFT,
        CONTROL_UNAVAILABLE,
        DEPLOYMENT_FAILED
    }

    record ExteriorAnchor(ServerLevel level, ShipTransform transform) {
    }

    private record Entry(ShipFlightRuntime runtime, ArmorStand exterior) {
    }

    private record TransitionRequest(int entityId, Entry entry, ShipTransitionPolicy.Transition transition) {
    }
}
