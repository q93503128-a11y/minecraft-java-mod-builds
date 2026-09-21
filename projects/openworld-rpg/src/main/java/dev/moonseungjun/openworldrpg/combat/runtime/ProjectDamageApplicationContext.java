package dev.moonseungjun.openworldrpg.combat.runtime;

import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.LivingEntity;

/**
 * One-shot server-thread authorization token for a project-owned Minecraft damage application.
 *
 * <p>The token is consumed at LivingEntity#hurtServer HEAD before donor/vanilla code runs, so any
 * recursive damage emitted from inside that call does not inherit project authority.</p>
 */
public final class ProjectDamageApplicationContext {
    private static final ThreadLocal<Authorization> AUTHORIZATION = new ThreadLocal<>();

    private ProjectDamageApplicationContext() {
    }

    public static boolean authorizeNext(
            LivingEntity target,
            BooleanSupplier operation
    ) {
        if (AUTHORIZATION.get() != null) {
            throw new IllegalStateException("Nested project damage authorization is not allowed.");
        }

        Authorization authorization = new Authorization(target.getUUID());
        AUTHORIZATION.set(authorization);
        try {
            return operation.getAsBoolean();
        } finally {
            AUTHORIZATION.remove();
        }
    }

    public static boolean consumeIfAuthorized(LivingEntity target) {
        Authorization authorization = AUTHORIZATION.get();
        if (authorization == null
                || authorization.consumed
                || !authorization.targetId.equals(target.getUUID())) {
            return false;
        }

        authorization.consumed = true;
        return true;
    }

    private static final class Authorization {
        private final UUID targetId;
        private boolean consumed;

        private Authorization(UUID targetId) {
            this.targetId = targetId;
        }
    }
}
