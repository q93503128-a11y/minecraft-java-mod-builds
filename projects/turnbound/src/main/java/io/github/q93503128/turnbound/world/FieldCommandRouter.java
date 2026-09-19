package io.github.q93503128.turnbound.world;

import net.minecraft.server.level.ServerPlayer;

/**
 * Selects exactly one field-command authority.
 *
 * <p>Drehmal production and the retired Aster March compatibility runtime share a packet type, but never a command
 * namespace at runtime. External-world players are therefore consumed by ExternalWorldBootstrap before any legacy
 * relay/session router is reachable.</p>
 */
public final class FieldCommandRouter {
    private FieldCommandRouter() {}

    public static void command(ServerPlayer player, String rawCommand) {
        if (player == null || rawCommand == null || rawCommand.isBlank()) return;
        if (ExternalWorldBootstrap.active(player)) {
            ExternalWorldBootstrap.command(player, rawCommand);
            return;
        }
        WorldSessionRouter.command(player, rawCommand);
    }
}
