package io.github.q93503128.turnbound;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.github.q93503128.turnbound.combat.BattleDefeatRecoveryLedger;
import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.BattleSessionAuthority;
import io.github.q93503128.turnbound.combat.BattleSessionRegistry;
import io.github.q93503128.turnbound.combat.BattleState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server-owned battle transport. It cannot create a fight from a client packet.
 * Encounter code must explicitly open a session using server-validated character definitions.
 */
public final class TurnboundBattleBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger(TurnboundBattleBridge.class);
    public static final Identifier ACTION = new Identifier("turnbound", "battle_action");
    public static final Identifier SNAPSHOT = new Identifier("turnbound", "battle_snapshot");
    public static final Identifier RECEIPT = new Identifier("turnbound", "battle_receipt");
    private static final Map<MinecraftServer, BattleSessionRegistry> SERVERS = new IdentityHashMap<>();
    private static final Map<MinecraftServer, BattleDefeatRecoveryLedger> DEFEAT_RECOVERY = new IdentityHashMap<>();
    private static final int MAX_TARGETS = 8;

    private TurnboundBattleBridge() {}

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (TurnboundWorldGate.active(server)) {
                SERVERS.put(server, new BattleSessionRegistry());
                DEFEAT_RECOVERY.put(server, new BattleDefeatRecoveryLedger());
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            BattleSessionRegistry registry = SERVERS.remove(server);
            if (registry != null) registry.clear();
            BattleDefeatRecoveryLedger recovery = DEFEAT_RECOVERY.remove(server);
            if (recovery != null) recovery.clear();
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.player));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!TurnboundWorldGate.active(server)) return;
            BattleSessionRegistry registry = SERVERS.get(server);
            if (registry == null) return;
            // At most one enemy action per battle per tick. Snapshot is
            // synchronized only for sessions that actually changed.
            for (BattleSessionAuthority session : registry.activeSessions()) {
                try {
                    if (session.advanceEnemyTurn()) syncParticipants(server, session);
                } catch (RuntimeException invalidEnemyKit) {
                    // Invalid enemy logic closes only its own session without rewards.
                    LOGGER.error("Invalid enemy action in TURNBOUND session {}. Closing without rewards.",
                            session.id(), invalidEnemyKit);
                    close(server, session.id());
                    continue;
                }
                if (session.outcome() == BattleOutcome.ENEMY_VICTORY) {
                    try {
                        settleDefeat(server, session);
                    } catch (RuntimeException unsafeRecovery) {
                        // Never delete a completed encounter or mint rewards because a
                        // target checkpoint cannot currently be reached.
                        LOGGER.error("TURNBOUND checkpoint return failed for session {}.",
                                session.id(), unsafeRecovery);
                    }
                }
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(ACTION, (server, player, handler, buf, responseSender) -> {
            // Decode on the network thread, then execute state changes on the server thread.
            final BattleSessionAuthority.Action action;
            try {
                long sessionId = buf.readLong();
                long revision = buf.readLong();
                String actor = buf.readString(128);
                String skill = buf.readString(128);
                int count = buf.readVarInt();
                if (count < 0 || count > MAX_TARGETS) return;
                java.util.List<String> targets = new java.util.ArrayList<>(count);
                for (int i = 0; i < count; i++) targets.add(buf.readString(128));
                if (buf.isReadable()) return; // No trailing client-controlled fields.
                action = new BattleSessionAuthority.Action(sessionId, revision, actor, skill, targets);
            } catch (RuntimeException malformed) {
                return;
            }

            server.execute(() -> {
                if (!TurnboundWorldGate.active(server)
                        || server.getPlayerManager().getPlayer(player.getUuid()) != player) return;
                BattleSessionRegistry registry = SERVERS.get(server);
                if (registry == null) return;
                String world = player.getServerWorld().getRegistryKey().getValue().toString();
                BattleSessionAuthority.Status status = registry.submit(player.getUuid(), world, action);
                sendReceipt(player, action.sessionId(), status);
                if (status == BattleSessionAuthority.Status.ACCEPTED) {
                    BattleSessionAuthority session = registry.sessionFor(player.getUuid());
                    if (session != null) {
                        syncParticipants(server, session);
                        if (session.outcome() == BattleOutcome.ENEMY_VICTORY) settleDefeat(server, session);
                    }
                } else {
                    // A rejected request cannot change any other participant's view.
                    sync(player);
                }
            });
        });
    }

    /** Called only by trusted server-side encounter code, never a network receiver. */
    public static BattleSessionAuthority open(MinecraftServer server, String worldId,
                                               BattleState battle, Map<UUID, Set<String>> actors) {
        if (!TurnboundWorldGate.active(server) || !SERVERS.containsKey(server)) {
            throw new IllegalStateException("TURNBOUND not activated on this copied world");
        }
        if (actors == null || actors.isEmpty()) throw new IllegalArgumentException("Empty party");
        for (UUID uuid : actors.keySet()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player == null || !worldId.equals(player.getServerWorld().getRegistryKey().getValue().toString())) {
                throw new IllegalStateException("Participant absent or not in battle dimension");
            }
        }
        BattleSessionAuthority session = SERVERS.get(server).open(worldId, battle, actors);
        syncParticipants(server, session);
        return session;
    }

    /** Read-only server-side eligibility: an already engaged player cannot be claimed twice. */
    public static boolean inBattle(MinecraftServer server, UUID playerId) {
        BattleSessionRegistry registry = SERVERS.get(server);
        return registry != null && registry.sessionFor(playerId) != null;
    }

    public static void close(MinecraftServer server, long sessionId) {
        BattleSessionRegistry registry = SERVERS.get(server);
        if (registry == null) return;
        // Clear clients before removing their membership.
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            BattleSessionAuthority s = registry.sessionFor(player.getUuid());
            if (s != null && s.id() == sessionId && ServerPlayNetworking.canSend(player, SNAPSHOT)) {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeBoolean(false);
                ServerPlayNetworking.send(player, SNAPSHOT, buf);
            }
        }
        registry.close(sessionId);
        TurnboundFieldEncounterService.onBattleClosed(server, sessionId);
        BattleDefeatRecoveryLedger recovery = DEFEAT_RECOVERY.get(server);
        if (recovery != null) recovery.discard(sessionId);
    }

    /**
     * Only server-verified ENEMY_VICTORY may recover participants. The terminal
     * snapshot is retained until a trusted encounter owner closes this session;
     * no implicit loot, battle closure or additional death is introduced here.
     * Offline participants or players who changed dimensions are not teleported.
     */
    private static void settleDefeat(MinecraftServer server, BattleSessionAuthority session) {
        if (!TurnboundWorldGate.active(server) || session.closed()
                || session.outcome() != BattleOutcome.ENEMY_VICTORY) return;
        BattleDefeatRecoveryLedger recovery = DEFEAT_RECOVERY.get(server);
        if (recovery == null) return;
        for (UUID participant : session.participants()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(participant);
            if (player == null) continue;
            String currentWorld = player.getServerWorld().getRegistryKey().getValue().toString();
            // A failed move stays pending, e.g. until the target chunk is loaded.
            if (recovery.recover(session, participant, currentWorld,
                    () -> TurnboundCheckpointService.returnAfterDefeat(player))) {
                LOGGER.info("TURNBOUND session {} completed a checkpoint return for participant.",
                        session.id());
            }
        }
    }

    private static void syncParticipants(MinecraftServer server, BattleSessionAuthority session) {
        for (UUID uuid : session.participants()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player != null) sync(player);
        }
    }

    public static void sync(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null || !TurnboundWorldGate.active(server)
                || !ServerPlayNetworking.canSend(player, SNAPSHOT)) return;
        BattleSessionRegistry registry = SERVERS.get(server);
        BattleSessionAuthority session = registry == null ? null : registry.sessionFor(player.getUuid());
        PacketByteBuf buf = PacketByteBufs.create();
        if (session == null || session.closed()) {
            buf.writeBoolean(false);
            ServerPlayNetworking.send(player, SNAPSHOT, buf);
            return;
        }
        BattleSessionAuthority.View snapshot = session.viewFor(player.getUuid());
        buf.writeBoolean(true);
        buf.writeLong(snapshot.sessionId());
        buf.writeLong(snapshot.revision());
        buf.writeString(snapshot.currentActorId(), 128);
        buf.writeVarInt(snapshot.outcome().ordinal());
        buf.writeVarInt(snapshot.combatants().size());
        for (var unit : snapshot.combatants()) {
            buf.writeString(unit.id(), 128);
            buf.writeString(unit.name(), 128);
            buf.writeBoolean(unit.side() == io.github.q93503128.turnbound.combat.CombatantSide.ALLY);
            buf.writeBoolean(unit.summon());
            buf.writeInt(unit.hp());
            buf.writeInt(unit.maxHp());
            buf.writeInt(unit.barrier());
            buf.writeLong(unit.gauge());
            buf.writeBoolean(unit.downed());
            buf.writeVarInt(unit.ownedSkills().size());
            for (var skill : unit.ownedSkills()) {
                buf.writeString(skill.id(), 128);
                buf.writeString(skill.name(), 128);
                buf.writeVarInt(skill.cooldown());
            }
        }
        ServerPlayNetworking.send(player, SNAPSHOT, buf);
    }

    private static void sendReceipt(ServerPlayerEntity player, long sessionId,
                                    BattleSessionAuthority.Status status) {
        if (!ServerPlayNetworking.canSend(player, RECEIPT)) return;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeLong(sessionId);
        buf.writeVarInt(status.ordinal());
        ServerPlayNetworking.send(player, RECEIPT, buf);
    }
}
