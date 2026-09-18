package dev.moonseungjun.openworldrpg.integration.bettercombat;

import dev.moonseungjun.openworldrpg.integration.bootstrap.RuntimeProfile;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

/**
 * Reflection-isolated Better Combat binding.
 *
 * <p>Openworld RPG intentionally does not compile against Better Combat's dependency-only API. The gameplay runtime
 * validates the exact mod separately; this adapter then verifies the two tiny runtime interfaces used to identify an
 * attack currently being executed by Better Combat.</p>
 */
public final class BetterCombatAuthorityAdapter {
    private static final String MOD_ID = "bettercombat";
    private static final String PLAYER_API = "net.bettercombat.api.EntityPlayer_BetterCombat";
    private static final String ATTACK_PROPERTIES_API = "net.bettercombat.logic.PlayerAttackProperties";

    private static volatile Binding binding = Binding.disabled();

    private BetterCombatAuthorityAdapter() {
    }

    public static synchronized void initialize(RuntimeProfile profile, Logger logger) {
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            binding = Binding.disabled();
            logger.info(
                    "Openworld RPG Better Combat authority adapter inactive for profile {} because {} is not loaded.",
                    profile.id(),
                    MOD_ID
            );
            return;
        }

        try {
            ClassLoader loader = BetterCombatAuthorityAdapter.class.getClassLoader();
            Class<?> playerApi = Class.forName(PLAYER_API, false, loader);
            Class<?> attackPropertiesApi = Class.forName(ATTACK_PROPERTIES_API, false, loader);
            Method getCurrentAttack = playerApi.getMethod("getCurrentAttack");
            Method getComboCount = attackPropertiesApi.getMethod("getComboCount");

            if (getComboCount.getReturnType() != int.class) {
                throw new IllegalStateException(
                        "Better Combat PlayerAttackProperties#getComboCount no longer returns int."
                );
            }

            binding = new Binding(playerApi, attackPropertiesApi, getCurrentAttack, getComboCount);
            logger.info(
                    "Openworld RPG Better Combat authority adapter armed for profile {} using {} and {}.",
                    profile.id(),
                    PLAYER_API,
                    ATTACK_PROPERTIES_API
            );
        } catch (ReflectiveOperationException exception) {
            binding = Binding.disabled();
            throw new IllegalStateException(
                    "Better Combat is loaded, but the pinned Openworld RPG authority API contract could not be resolved.",
                    exception
            );
        }
    }

    public static AttackContext currentAttack(Player player) {
        Binding current = binding;
        if (!current.enabled()) {
            return AttackContext.inactive();
        }

        if (!current.playerApi().isInstance(player) || !current.attackPropertiesApi().isInstance(player)) {
            throw new IllegalStateException(
                    "Better Combat is loaded, but the server Player does not expose the pinned authority interfaces."
            );
        }

        try {
            Object attack = current.getCurrentAttack().invoke(player);
            if (attack == null) {
                return AttackContext.inactive();
            }

            int comboCount = (int) current.getComboCount().invoke(player);
            if (comboCount < 0) {
                return AttackContext.inactive();
            }
            return new AttackContext(true, comboCount);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException(
                    "Better Combat attack state could not be read by the Openworld RPG authority adapter.",
                    exception
            );
        }
    }

    public record AttackContext(boolean active, int comboCount) {
        public static AttackContext inactive() {
            return new AttackContext(false, -1);
        }
    }

    private record Binding(
            Class<?> playerApi,
            Class<?> attackPropertiesApi,
            Method getCurrentAttack,
            Method getComboCount
    ) {
        private static Binding disabled() {
            return new Binding(null, null, null, null);
        }

        private boolean enabled() {
            return playerApi != null;
        }
    }
}
