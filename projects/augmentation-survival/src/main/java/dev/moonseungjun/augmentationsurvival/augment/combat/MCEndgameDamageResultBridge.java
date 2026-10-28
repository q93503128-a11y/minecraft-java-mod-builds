package dev.moonseungjun.augmentationsurvival.augment.combat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

/**
 * Typed-at-runtime bridge for pinned MCEndgame's DamageCalculationResult.
 *
 * <p>Augmentation Survival can run without MCEndgame, so this class deliberately avoids a hard
 * compile/runtime reference to MCEndgame classes. When the pinned integration is present, the
 * result ABI is resolved once and then reused for every player hit.</p>
 */
public final class MCEndgameDamageResultBridge {
    private static volatile Access access;

    private MCEndgameDamageResultBridge() {
    }

    public static Object modify(
            Player player,
            ServerLevel level,
            DamageSource source,
            Object result
    ) {
        if (result == null) {
            return null;
        }

        Access resolved = access(result.getClass());
        try {
            if (!(boolean) resolved.isApplying().invoke(result)) {
                return result;
            }

            float original = (float) resolved.getAmount().invoke(result);
            float adjusted = MCEndgameDamageCompatibility.modifyCalculatedDamage(
                    player,
                    level,
                    source,
                    Optional.of(original)
            ).orElse(original);

            if (Float.compare(original, adjusted) == 0) {
                return result;
            }

            // Keep the original result in the "applying" state even when augment protection has
            // reduced the amount to zero. This preserves MCEndgame's accepted-hit/cooldown flow
            // while preventing the absorbed portion from reaching HP.
            return resolved.normalDamage().invoke(resolved.companion(), adjusted);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Pinned MCEndgame damage-result ABI invocation failed", exception);
        }
    }

    private static Access access(Class<?> resultClass) {
        Access current = access;
        if (current != null && current.resultClass() == resultClass) {
            return current;
        }
        synchronized (MCEndgameDamageResultBridge.class) {
            current = access;
            if (current != null && current.resultClass() == resultClass) {
                return current;
            }
            access = current = resolve(resultClass);
            return current;
        }
    }

    private static Access resolve(Class<?> resultClass) {
        try {
            Method isApplying = resultClass.getMethod("isApplying");
            Method getAmount = resultClass.getMethod("getAmount");
            Field companionField = resultClass.getField("Companion");
            Object companion = companionField.get(null);
            if (companion == null) {
                throw new IllegalStateException("Pinned MCEndgame DamageCalculationResult.Companion is null");
            }
            Method normalDamage = companion.getClass().getMethod("normalDamage", float.class);
            return new Access(resultClass, isApplying, getAmount, companion, normalDamage);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Pinned MCEndgame DamageCalculationResult ABI changed: " + resultClass.getName(),
                    exception
            );
        }
    }

    private record Access(
            Class<?> resultClass,
            Method isApplying,
            Method getAmount,
            Object companion,
            Method normalDamage
    ) {
    }
}
