package dev.moonseungjun.augmentationsurvival.integration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Optional runtime bridge to MCEndgame's exact true-damage API without a compile-time dependency. */
public final class McEndgameDamageAdapter {
    private static final String MOD_ID = "mcendgame";
    private static final String DAMAGE_DEALING_CLASS =
            "de.fuballer.mcendgame.main.component.damage.dealing.DamageDealing";

    private static volatile Binding binding;

    private McEndgameDamageAdapter() {
    }

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    public static boolean dealTrueDamage(LivingEntity target, float amount, Entity causingEntity) {
        if (!isLoaded()) {
            throw new IllegalStateException("MCEndgame damage adapter requested while mcendgame is not loaded");
        }
        Binding current = binding();
        try {
            Object result = current.method().invoke(current.instance(), target, amount, causingEntity);
            return Boolean.TRUE.equals(result);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("MCEndgame true-damage invocation failed", cause);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("MCEndgame true-damage invocation failed", exception);
        }
    }

    private static Binding binding() {
        Binding current = binding;
        if (current != null) {
            return current;
        }
        synchronized (McEndgameDamageAdapter.class) {
            current = binding;
            if (current != null) {
                return current;
            }
            binding = current = resolveBinding();
            return current;
        }
    }

    private static Binding resolveBinding() {
        try {
            ClassLoader loader = McEndgameDamageAdapter.class.getClassLoader();
            Class<?> damageDealing = Class.forName(DAMAGE_DEALING_CLASS, false, loader);
            Object instance = damageDealing.getField("INSTANCE").get(null);
            Method method = damageDealing.getMethod(
                    "dealTrueDamage",
                    Entity.class,
                    float.class,
                    Entity.class
            );
            if (method.getReturnType() != boolean.class && method.getReturnType() != Boolean.class) {
                throw new IllegalStateException(
                        "MCEndgame dealTrueDamage has unexpected return type: " + method.getReturnType().getName()
                );
            }
            return new Binding(instance, method);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Failed to bind pinned MCEndgame true-damage API",
                    exception
            );
        }
    }

    private record Binding(Object instance, Method method) {
    }
}
