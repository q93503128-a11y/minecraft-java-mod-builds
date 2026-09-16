package kr.moonseungjun.earthtostars.fabric.ship;

import kr.moonseungjun.earthtostars.fabric.EarthToStarsFabric;
import kr.moonseungjun.earthtostars.fabric.networking.ShipControlInputPayload;
import kr.moonseungjun.earthtostars.fabric.networking.ShipControlSessionPayload;
import kr.moonseungjun.earthtostars.fabric.persistence.EarthToStarsFabricShipSavedData;
import kr.moonseungjun.earthtostars.ship.domain.ShipId;
import kr.moonseungjun.earthtostars.ship.domain.ShipState;
import kr.moonseungjun.earthtostars.ship.runtime.ShipControlInput;
import kr.moonseungjun.earthtostars.ship.runtime.ShipControlLease;
import kr.moonseungjun.earthtostars.ship.runtime.ShipFlightRuntime;
import kr.moonseungjun.earthtostars.ship.runtime.ShipRepository;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class EarthToStarsFabricShipAuthority {
    private static final ShipRepository REPOSITORY = new ShipRepository();
    private static final Map<ShipId, ShipFlightRuntime> ACTIVE_RUNTIMES = new LinkedHashMap<>();
    private static final Map<UUID, ShipId> CONTROLLER_BINDINGS = new LinkedHashMap<>();

    private static MinecraftServer activeServer;
    private static EarthToStarsFabricShipSavedData savedData;

    private EarthToStarsFabricShipAuthority() {
    }

    public static void initializeLifecycle() {
        ServerLifecycleEvents.SERVER_STARTED.register(EarthToStarsFabricShipAuthority::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPED.register(EarthToStarsFabricShipAuthority::onServerStopped);
    }

    private static synchronized void onServerStarted(MinecraftServer server) {
        resetRuntimeOnly();
        activeServer = server;
        savedData = EarthToStarsFabricShipSavedData.get(server);
        REPOSITORY.clear();
        for (ShipState ship : savedData.decodeAll(EarthToStarsFabric.bootstrapCatalog())) {
            REPOSITORY.add(ship);
        }
        EarthToStarsFabric.LOGGER.info(
                "EARTH TO STARS Fabric ship authority loaded persisted_ships={}",
                REPOSITORY.size()
        );
    }

    private static synchronized void onServerStopped(MinecraftServer server) {
        if (activeServer != server) {
            return;
        }
        resetRuntimeOnly();
        REPOSITORY.clear();
        savedData = null;
        activeServer = null;
    }

    public static synchronized Optional<ShipState> findShip(ShipId shipId) {
        requireInitialized();
        return REPOSITORY.find(shipId);
    }

    public static synchronized Optional<ShipState> findOwnedShip(UUID ownerId) {
        requireInitialized();
        return REPOSITORY.findOwnedBy(ownerId);
    }

    public static synchronized int persistedShipCount() {
        requireInitialized();
        return REPOSITORY.size();
    }

    public static synchronized void addPersistentShip(ShipState state) {
        requireInitialized();
        REPOSITORY.add(state);
        try {
            savedData.put(state);
        } catch (RuntimeException failedPersistence) {
            REPOSITORY.remove(state.shipId());
            throw failedPersistence;
        }
    }

    public static synchronized void persistShip(ShipState state) {
        requireInitialized();
        ShipState authoritative = REPOSITORY.find(state.shipId())
                .orElseThrow(() -> new IllegalArgumentException("unknown authoritative ship: " + state.shipId()));
        if (authoritative != state) {
            throw new IllegalArgumentException("attempted to persist a detached ship state: " + state.shipId());
        }
        savedData.put(state);
    }

    public static synchronized boolean removePersistentShip(ShipId shipId) {
        requireInitialized();
        ShipState state = REPOSITORY.find(shipId).orElse(null);
        if (state == null) {
            return false;
        }
        if (!savedData.remove(shipId)) {
            throw new IllegalStateException("authoritative repository/save mismatch for ship " + shipId);
        }
        deactivateRuntime(shipId);
        if (REPOSITORY.remove(shipId).isEmpty()) {
            throw new IllegalStateException("authoritative repository changed during ship removal " + shipId);
        }
        return true;
    }

    /**
     * Registers the real server-owned flight runtime after a physical craft has been deployed.
     * This method never creates a craft or grants control by itself.
     */
    public static synchronized void activateRuntime(ShipFlightRuntime runtime) {
        requireInitialized();
        ShipState authoritative = REPOSITORY.find(runtime.ship().shipId())
                .orElseThrow(() -> new IllegalArgumentException("cannot activate unpersisted ship " + runtime.ship().shipId()));
        if (authoritative != runtime.ship()) {
            throw new IllegalArgumentException("flight runtime must use the authoritative ShipState instance");
        }
        ShipFlightRuntime previous = ACTIVE_RUNTIMES.putIfAbsent(runtime.ship().shipId(), runtime);
        if (previous != null && previous != runtime) {
            throw new IllegalStateException("ship already has an active flight runtime: " + runtime.ship().shipId());
        }
    }

    public static synchronized boolean deactivateRuntime(ShipId shipId) {
        requireInitialized();
        ShipFlightRuntime runtime = ACTIVE_RUNTIMES.remove(shipId);
        if (runtime == null) {
            return false;
        }
        runtime.lease().map(ShipControlLease::controllerId).ifPresent(controllerId -> {
            CONTROLLER_BINDINGS.remove(controllerId, shipId);
            runtime.releaseControl(controllerId);
            ServerPlayer player = activeServer.getPlayerList().getPlayer(controllerId);
            if (player != null) {
                ServerPlayNetworking.send(player, ShipControlSessionPayload.inactive(shipId));
            }
        });
        return true;
    }

    /**
     * Called only after the future physical craft layer has validated seat/range/world state.
     * The client has no packet that can mint its own session.
     */
    public static synchronized Optional<UUID> grantControl(ServerPlayer player, ShipId shipId, long tick) {
        if (!belongsToActiveServer(player)) {
            return Optional.empty();
        }
        ShipFlightRuntime runtime = ACTIVE_RUNTIMES.get(shipId);
        if (runtime == null) {
            return Optional.empty();
        }

        ShipId previousShip = CONTROLLER_BINDINGS.get(player.getUUID());
        if (previousShip != null && !previousShip.equals(shipId)) {
            releaseControlInternal(player.getUUID(), true);
        }

        Optional<UUID> session = runtime.requestControl(player.getUUID(), tick);
        session.ifPresent(sessionId -> {
            CONTROLLER_BINDINGS.put(player.getUUID(), shipId);
            ServerPlayNetworking.send(player, ShipControlSessionPayload.active(shipId, sessionId));
        });
        return session;
    }

    public static synchronized boolean acceptControlInput(ServerPlayer player, ShipControlInputPayload payload) {
        if (!belongsToActiveServer(player)) {
            return false;
        }
        ShipId shipId = payload.shipId();
        if (!shipId.equals(CONTROLLER_BINDINGS.get(player.getUUID()))) {
            return false;
        }
        ShipFlightRuntime runtime = ACTIVE_RUNTIMES.get(shipId);
        if (runtime == null) {
            CONTROLLER_BINDINGS.remove(player.getUUID(), shipId);
            return false;
        }

        ShipControlInput input;
        try {
            input = new ShipControlInput(payload.throttle(), payload.yaw(), payload.lift());
        } catch (IllegalArgumentException invalidInput) {
            return false;
        }

        boolean accepted = runtime.acceptInput(
                player.getUUID(),
                payload.sessionId(),
                payload.sequence(),
                input,
                player.level().getGameTime()
        );
        if (!accepted && runtime.lease().map(ShipControlLease::controllerId).filter(player.getUUID()::equals).isEmpty()) {
            CONTROLLER_BINDINGS.remove(player.getUUID(), shipId);
        }
        return accepted;
    }

    public static synchronized boolean releaseControl(ServerPlayer player) {
        if (!belongsToActiveServer(player)) {
            return false;
        }
        return releaseControlInternal(player.getUUID(), true);
    }

    /**
     * The physical runtime calls this after ShipFlightRuntime.tick reports an expired controller.
     */
    public static synchronized void onControlLeaseExpired(ShipId shipId, UUID controllerId) {
        requireInitialized();
        CONTROLLER_BINDINGS.remove(controllerId, shipId);
        ServerPlayer player = activeServer.getPlayerList().getPlayer(controllerId);
        if (player != null) {
            ServerPlayNetworking.send(player, ShipControlSessionPayload.inactive(shipId));
        }
    }

    private static boolean releaseControlInternal(UUID playerId, boolean notifyClient) {
        ShipId shipId = CONTROLLER_BINDINGS.remove(playerId);
        if (shipId == null) {
            return false;
        }
        ShipFlightRuntime runtime = ACTIVE_RUNTIMES.get(shipId);
        boolean released = runtime != null && runtime.releaseControl(playerId);
        if (notifyClient && activeServer != null) {
            ServerPlayer player = activeServer.getPlayerList().getPlayer(playerId);
            if (player != null) {
                ServerPlayNetworking.send(player, ShipControlSessionPayload.inactive(shipId));
            }
        }
        return released;
    }

    private static boolean belongsToActiveServer(ServerPlayer player) {
        return activeServer != null && player.level().getServer() == activeServer;
    }

    private static void requireInitialized() {
        if (activeServer == null || savedData == null) {
            throw new IllegalStateException("Fabric ship authority is not attached to an active server");
        }
    }

    private static void resetRuntimeOnly() {
        for (ShipFlightRuntime runtime : ACTIVE_RUNTIMES.values()) {
            runtime.lease().map(ShipControlLease::controllerId).ifPresent(runtime::releaseControl);
        }
        ACTIVE_RUNTIMES.clear();
        CONTROLLER_BINDINGS.clear();
    }
}
