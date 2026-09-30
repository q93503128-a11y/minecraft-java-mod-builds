package dev.moonseungjun.openworldrpg.integration.spellengine;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.runtime.HunterQuickstepVolleyRuntime;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;

public final class SpellEngineHunterQuickstepProjectileBridge {
    private SpellEngineHunterQuickstepProjectileBridge() {
    }

    public static boolean preparePierce(
            Object rawProjectile,
            Entity target,
            boolean pierceAlreadySpent
    ) {
        if (!(rawProjectile instanceof Projectile projectile)
                || !(target instanceof LivingEntity livingTarget)) {
            return false;
        }

        try {
            Object spellEntry = invoke(rawProjectile, "getSpellEntry");
            if (spellEntry == null
                    || !ProjectSpellSpec.HUNTER_QUICKSTEP_VOLLEY_ID.equals(spellId(spellEntry))) {
                return false;
            }

            Object perks = invoke(rawProjectile, "mutablePerks");
            if (perks == null) {
                return false;
            }
            var pierce = perks.getClass().getField("pierce");
            pierce.setInt(perks, 0);

            if (pierceAlreadySpent
                    || !(projectile.getOwner() instanceof ServerPlayer hunter)
                    || !HunterQuickstepVolleyRuntime.canEmpoweredPierce(hunter, livingTarget)) {
                return false;
            }

            pierce.setInt(perks, 1);
            return true;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Pinned Spell Engine Quickstep projectile contract is unavailable.",
                    exception
            );
        }
    }

    private static Object invoke(Object target, String name)
            throws ReflectiveOperationException {
        try {
            Method method = target.getClass().getMethod(name);
            return method.invoke(target);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof ReflectiveOperationException reflective) {
                throw reflective;
            }
            throw new ReflectiveOperationException(cause);
        }
    }

    private static String spellId(Object spellEntry)
            throws ReflectiveOperationException {
        Object optionalKey = invokeFirst(spellEntry, "unwrapKey", "getKey");
        if (!(optionalKey instanceof Optional<?> optional) || optional.isEmpty()) {
            throw new IllegalStateException(
                    "Spell Engine Quickstep projectile has no registry key."
            );
        }
        Object key = optional.orElseThrow();
        Object id = invokeFirst(key, "location", "identifier", "getValue");
        return String.valueOf(id);
    }

    private static Object invokeFirst(Object target, String... names)
            throws ReflectiveOperationException {
        NoSuchMethodException missing = null;
        for (String name : names) {
            try {
                return invoke(target, name);
            } catch (NoSuchMethodException exception) {
                missing = exception;
            }
        }
        throw missing != null ? missing : new NoSuchMethodException();
    }
}
