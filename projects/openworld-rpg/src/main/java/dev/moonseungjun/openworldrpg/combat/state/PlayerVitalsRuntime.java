package dev.moonseungjun.openworldrpg.combat.state;

import java.util.Objects;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Server-authoritative player HP projection and natural recovery.
 *
 * <p>Minecraft MAX_HEALTH is the live presentation/collision-compatible projection of the project
 * build. The project formula is the authority. Rebuilding a character preserves current HP
 * percentage so VIT/equipment changes cannot become free healing.</p>
 */
public final class PlayerVitalsRuntime {
    public static final double NATURAL_RECOVERY_FRACTION_PER_SECOND = 0.004;
    private static volatile boolean initialized;

    private PlayerVitalsRuntime() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tickPlayer(player);
            }
        });
        initialized = true;
    }

    public static void synchronizeMaxHealth(Player player, int canonicalMaxHealth) {
        Objects.requireNonNull(player, "player");
        if (canonicalMaxHealth <= 0) {
            throw new IllegalArgumentException("canonicalMaxHealth must be positive.");
        }
        if (player.level().isClientSide()) {
            throw new IllegalStateException("Player vital authority is server-only.");
        }

        var maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            throw new IllegalStateException("Server player has no MAX_HEALTH attribute.");
        }

        float oldMax = player.getMaxHealth();
        float oldHealth = player.getHealth();
        double fraction = oldMax > 0.0F
                ? Math.max(0.0, Math.min(1.0, oldHealth / oldMax))
                : 1.0;

        maxHealth.setBaseValue(canonicalMaxHealth);
        float projectedMax = player.getMaxHealth();
        player.setHealth((float) Math.max(
                0.0,
                Math.min(projectedMax, projectedMax * fraction)
        ));
    }

    static void tickPlayer(ServerPlayer player) {
        if (!player.isAlive()) {
            return;
        }

        long gameTick = player.level().getGameTime();
        PlayerCombatState state = CombatStateServices.states()
                .getOrCreate(player.getUUID(), gameTick);

        boolean sprintAllowed = state.updateSprinting(player.isSprinting(), gameTick);
        if (!sprintAllowed && player.isSprinting()) {
            player.setSprinting(false);
        }

        if (!state.canNaturalHpRecover(gameTick)) {
            return;
        }

        float current = player.getHealth();
        float max = player.getMaxHealth();
        if (current <= 0.0F || current >= max) {
            return;
        }

        double perTick = max
                * NATURAL_RECOVERY_FRACTION_PER_SECOND
                / 20.0;
        float heal = (float) Math.min(max - current, perTick);
        if (heal > 0.0F) {
            player.heal(heal);
        }
    }
}
