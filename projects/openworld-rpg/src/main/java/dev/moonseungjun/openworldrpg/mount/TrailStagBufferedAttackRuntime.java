package dev.moonseungjun.openworldrpg.mount;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/**
 * Preserves one Better Combat basic-attack request across Trail Stag's authored safe-dismount
 * boundary. The request is replayed two server ticks later so Better Combat recomputes its normal
 * hand/combo/range authority instead of Openworld RPG fabricating donor combat state.
 */
public final class TrailStagBufferedAttackRuntime {
    public static final int BUFFER_TICKS = 2;

    private static final String BETTER_COMBAT_SERVER_NETWORK =
            "net.bettercombat.network.ServerNetwork";
    private static final ConcurrentHashMap<UUID, PendingAttack> PENDING =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Method> REPLAY_METHODS =
            new ConcurrentHashMap<>();

    private TrailStagBufferedAttackRuntime() {
    }

    /**
     * Buffers exactly one donor request for this player. A duplicate pending request fails closed.
     */
    public static boolean buffer(
            Object request,
            MinecraftServer server,
            ServerPlayer player,
            ServerGamePacketListenerImpl handler
    ) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(handler, "handler");
        PendingAttack pending = new PendingAttack(
                request,
                player,
                handler,
                server.getTickCount() + BUFFER_TICKS
        );
        return PENDING.putIfAbsent(player.getUUID(), pending) == null;
    }

    public static void tick(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        int now = server.getTickCount();
        for (var entry : PENDING.entrySet()) {
            PendingAttack pending = entry.getValue();
            if (now < pending.executeServerTick()) {
                continue;
            }
            if (!PENDING.remove(entry.getKey(), pending)) {
                continue;
            }

            ServerPlayer current = server.getPlayerList().getPlayer(entry.getKey());
            if (current == null
                    || current != pending.player()
                    || !current.isAlive()
                    || current.isSpectator()) {
                continue;
            }
            replay(server, pending);
        }
    }

    public static void disconnect(UUID playerId) {
        PENDING.remove(Objects.requireNonNull(playerId, "playerId"));
    }

    static int pendingCount() {
        return PENDING.size();
    }

    private static void replay(MinecraftServer server, PendingAttack pending) {
        try {
            Method method = REPLAY_METHODS.computeIfAbsent(
                    pending.request().getClass(),
                    TrailStagBufferedAttackRuntime::resolveReplayMethod
            );
            method.invoke(
                    null,
                    pending.request(),
                    server,
                    pending.player(),
                    pending.handler()
            );
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            OpenworldRpgMod.LOGGER.error(
                    "Trail Stag buffered Better Combat attack replay failed closed for {}.",
                    pending.player().getUUID(),
                    cause
            );
        } catch (ReflectiveOperationException | RuntimeException exception) {
            OpenworldRpgMod.LOGGER.error(
                    "Trail Stag buffered Better Combat attack replay could not resolve the pinned donor contract for {}.",
                    pending.player().getUUID(),
                    exception
            );
        }
    }

    private static Method resolveReplayMethod(Class<?> requestClass) {
        try {
            Class<?> serverNetwork = Class.forName(
                    BETTER_COMBAT_SERVER_NETWORK,
                    false,
                    TrailStagBufferedAttackRuntime.class.getClassLoader()
            );
            for (Method method : serverNetwork.getDeclaredMethods()) {
                if (!method.getName().equals("handleAttackRequest")
                        || !Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                Class<?>[] parameters = method.getParameterTypes();
                if (parameters.length == 4
                        && parameters[0].isAssignableFrom(requestClass)
                        && parameters[1] == MinecraftServer.class
                        && parameters[2] == ServerPlayer.class
                        && parameters[3] == ServerGamePacketListenerImpl.class) {
                    return method;
                }
            }
            throw new IllegalStateException(
                    "Pinned Better Combat ServerNetwork#handleAttackRequest contract not found for "
                            + requestClass.getName()
            );
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException(
                    "Better Combat server network class disappeared while a Trail Stag attack was buffered.",
                    exception
            );
        }
    }

    private record PendingAttack(
            Object request,
            ServerPlayer player,
            ServerGamePacketListenerImpl handler,
            int executeServerTick
    ) {
    }
}
