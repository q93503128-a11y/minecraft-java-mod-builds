package dev.moonseungjun.openworldrpg.network;

import dev.moonseungjun.openworldrpg.combat.runtime.ProjectDodgeRuntime;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Narrow project-owned combat networking. Gameplay values are never accepted from the client. */
public final class ProjectCombatNetworking {
    private ProjectCombatNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(
                DodgeRequestPayload.TYPE,
                DodgeRequestPayload.CODEC
        );
        PayloadTypeRegistry.clientboundPlay().register(
                DodgeAcceptedPayload.TYPE,
                DodgeAcceptedPayload.CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
                DodgeRequestPayload.TYPE,
                (payload, context) ->
                        ProjectDodgeRuntime.request(
                                context.player(),
                                payload.forwardIntent(),
                                payload.strafeIntent(),
                                payload.sequence()
                        )
        );
    }

    public static void flushDodgeAccepted(
            MinecraftServer server
    ) {
        Objects.requireNonNull(server, "server");
        for (var event
                : ProjectDodgeRuntime.drainAcceptedEvents()) {
            ServerPlayer source = server.getPlayerList()
                    .getPlayer(event.playerId());
            if (source == null) {
                continue;
            }
            var payload = new DodgeAcceptedPayload(
                    event.entityId(),
                    event.directionCode()
            );
            var sent = new HashSet<UUID>();
            if (ServerPlayNetworking.canSend(
                    source,
                    DodgeAcceptedPayload.TYPE
            )) {
                ServerPlayNetworking.send(source, payload);
                sent.add(source.getUUID());
            }
            for (ServerPlayer watcher
                    : PlayerLookup.tracking(source)) {
                if (sent.add(watcher.getUUID())
                        && ServerPlayNetworking.canSend(
                                watcher,
                                DodgeAcceptedPayload.TYPE
                        )) {
                    ServerPlayNetworking.send(
                            watcher,
                            payload
                    );
                }
            }
        }
    }
}
