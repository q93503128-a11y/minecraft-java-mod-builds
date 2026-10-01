package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatBuildPublisher;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server bridge from Minecraft's real blocking state to project guard/perfect-guard authority.
 *
 * <p>The client never submits guard timing or perfect-guard success. A held, server-observed block
 * opens project guard as soon as the shared action gate permits it; release, guard break and dodge
 * close the same server-owned state.</p>
 */
public final class ProjectGuardRuntime {
    private ProjectGuardRuntime() {
    }

    public static void tick(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            synchronize(player);
        }
    }

    public static void synchronize(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        long nowTick = player.level().getGameTime();
        var defense = CombatStateServices.defenseStates()
                .getOrCreate(player.getUUID());
        boolean before = defense.isGuardHeld();

        boolean guardCapable = CombatStateServices.defenseSnapshots()
                .snapshot(player.getUUID())
                .flatMap(snapshot -> snapshot.guardType())
                .isPresent();
        boolean requested = guardCapable
                && player.isBlocking()
                && !ProjectPlayerActionRuntime.hardReactionActive(
                        player.getUUID(),
                        nowTick
                );

        if (requested) {
            defense.pressGuard(nowTick);
        } else {
            defense.releaseGuard();
        }

        if (before != defense.isGuardHeld()) {
            PlayerCombatBuildPublisher.refresh(player);
        }
    }
}
