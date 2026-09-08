package kr.moonseungjun.earthtostars.ship.runtime.minecraft;

import kr.moonseungjun.earthtostars.ship.domain.ModuleSlot;
import kr.moonseungjun.earthtostars.ship.domain.ModuleSlotType;
import kr.moonseungjun.earthtostars.ship.domain.ShipId;
import kr.moonseungjun.earthtostars.ship.domain.ShipState;
import kr.moonseungjun.earthtostars.ship.networking.ShipControlInputPayload;
import kr.moonseungjun.earthtostars.ship.networking.ShipControlSessionPayload;
import kr.moonseungjun.earthtostars.ship.runtime.ShipControlInput;
import kr.moonseungjun.earthtostars.ship.runtime.ShipFlightRuntime;
import kr.moonseungjun.earthtostars.ship.runtime.ShipFlightTuning;
import kr.moonseungjun.earthtostars.ship.runtime.ShipTransform;
import kr.moonseungjun.earthtostars.ship.runtime.ShipVec3;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ShipRuntimeManager {
    private static final double CONTROL_RANGE_SQUARED = 64.0D * 64.0D;
    private static final Map<Integer, Entry> ENTRIES = new LinkedHashMap<>();

    private ShipRuntimeManager() {
    }

    public static int spawnAndControl(ServerPlayer player, ServerLevel level, long tick) {
        removeOwnedCraft(player.getUUID());

        Vec3 spawn = player.position().add(player.getLookAngle().scale(4.0D)).add(0.0D, 1.0D, 0.0D);
        ArmorStand exterior = new ArmorStand(level, spawn.x, spawn.y, spawn.z);
        exterior.setNoGravity(true);
        exterior.setInvulnerable(true);
        exterior.setCustomName(Component.literal("개척선 선체"));
        exterior.setCustomNameVisible(true);
        exterior.setYRot(player.getYRot());
        if (!level.addFreshEntity(exterior)) {
            throw new IllegalStateException("failed to add ship exterior proxy to level");
        }

        ShipState ship = ShipState.create(ShipId.random(), player.getUUID(), initialSlots());
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

    public static boolean controlNearest(ServerPlayer player, long tick) {
        Entry nearest = ENTRIES.values().stream()
                .filter(entry -> !entry.exterior().isRemoved())
                .filter(entry -> entry.exterior().level() == player.level())
                .filter(entry -> entry.runtime().ship().can(player.getUUID(), kr.moonseungjun.earthtostars.ship.domain.ShipPermission.PILOT))
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
            entry.exterior().setDeltaMovement(Vec3.ZERO);
            entry.exterior().setPos(transform.position().x(), transform.position().y(), transform.position().z());
            entry.exterior().setYRot((float) transform.yawDegrees());
            entry.exterior().setXRot((float) transform.pitchDegrees());
        }
        removed.forEach(ENTRIES::remove);
    }

    public static void clear() {
        ENTRIES.clear();
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
}
