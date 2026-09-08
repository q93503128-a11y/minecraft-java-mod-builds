package kr.moonseungjun.earthtostars.ship.runtime.minecraft;

import kr.moonseungjun.earthtostars.ship.domain.ModuleCatalog;
import kr.moonseungjun.earthtostars.ship.domain.ModuleSlot;
import kr.moonseungjun.earthtostars.ship.domain.ModuleSlotType;
import kr.moonseungjun.earthtostars.ship.domain.ShipPermission;
import kr.moonseungjun.earthtostars.ship.domain.ShipState;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class ShipRuntimeManager {
    private static final double CONTROL_RANGE_SQUARED = 64.0D * 64.0D;
    private static final Map<Integer, Entry> ENTRIES = new LinkedHashMap<>();
    private static final ShipRepository REPOSITORY = new ShipRepository();
    private static final ModuleCatalog CATALOG = ShipBootstrapCatalog.create();

    private ShipRuntimeManager() {
    }

    public static void initialize(MinecraftServer server) {
        ENTRIES.clear();
        REPOSITORY.clear();
        for (ShipState ship : ShipSavedData.get(server).decodeAll(CATALOG)) {
            REPOSITORY.add(ship);
        }
    }

    public static int spawnAndControl(ServerPlayer player, ServerLevel level, long tick) {
        removeOwnedCraft(player.getUUID());

        Vec3 spawn = player.position().add(player.getLookAngle().scale(4.0D)).add(0.0D, 1.0D, 0.0D);
        ArmorStand exterior = createExterior(level, spawn, player.getYRot(), player.getXRot());
        if (!level.addFreshEntity(exterior)) {
            throw new IllegalStateException("failed to add ship exterior proxy to level");
        }

        ShipState ship = REPOSITORY.create(player.getUUID(), initialSlots());
        ShipSavedData.get(level.getServer()).put(ship);
        ShipTransform transform = new ShipTransform(
                new ShipVec3(spawn.x, spawn.y, spawn.z),
                ShipVec3.ZERO,
                player.getYRot(),
                0.0D
        );
        ShipFlightRuntime runtime = new ShipFlightRuntime(ship, transform, ShipFlightTuning.P0);
        Entry entry = new Entry(runtime, exterior);
        ENTRIES.put(exterior.getId(), entry);
        grantControl(player, entry, tick);
        return exterior.getId();
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

            Optional<UUID> expiredController = entry.runtime().tick(entry.exterior().level().getGameTime());
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
            if (transition != ShipTransitionPolicy.Transition.NONE && entry.runtime().lease().isPresent()) {
                transitions.add(new TransitionRequest(entityId, entry, transition));
                continue;
            }

            applyTransform(entry.exterior(), transform);
        }
        removed.forEach(ENTRIES::remove);
        for (TransitionRequest request : transitions) {
            executeTransition(server, request);
        }
    }

    public static void clear() {
        ENTRIES.clear();
        REPOSITORY.clear();
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
        grantControl(teleported, nextEntry, target.getGameTime());
    }

    private static void applyTransform(ArmorStand exterior, ShipTransform transform) {
        exterior.setDeltaMovement(Vec3.ZERO);
        exterior.setPos(transform.position().x(), transform.position().y(), transform.position().z());
        exterior.setYRot((float) transform.yawDegrees());
        exterior.setXRot((float) transform.pitchDegrees());
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

    private static List<ModuleSlot> initialSlots() {
        return List.of(
                new ModuleSlot("core", ModuleSlotType.CORE, 1),
                new ModuleSlot("engine", ModuleSlotType.PROPULSION, 1),
                new ModuleSlot("power", ModuleSlotType.POWER, 1),
                new ModuleSlot("cargo", ModuleSlotType.CARGO, 1),
                new ModuleSlot("turret", ModuleSlotType.WEAPON_HARDPOINT, 1)
        );
    }

    private record Entry(ShipFlightRuntime runtime, ArmorStand exterior) {
    }

    private record TransitionRequest(int entityId, Entry entry, ShipTransitionPolicy.Transition transition) {
    }
}
