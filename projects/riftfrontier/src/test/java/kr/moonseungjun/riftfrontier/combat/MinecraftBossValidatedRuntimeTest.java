package kr.moonseungjun.riftfrontier.combat;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-Java structural contract for the production boss runtime boundary.
 *
 * <p>Behavior that needs Minecraft classes is covered by native GameTest. Keeping this test API-free avoids silently
 * widening the JUnit classpath just to instantiate a Minecraft hit-volume SAM.</p>
 */
final class MinecraftBossValidatedRuntimeTest {
    @Test
    void productionFactoryRequiresValidatedSemanticsAndReturnsSealedRuntime() {
        Method factory = Arrays.stream(MinecraftBossCombatAdapter.class.getDeclaredMethods())
            .filter(method -> method.getName().equals("validated"))
            .filter(method -> Modifier.isStatic(method.getModifiers()))
            .findFirst()
            .orElseThrow();

        assertEquals(MinecraftBossCombatAdapter.ValidatedRuntime.class, factory.getReturnType());
        assertEquals(4, factory.getParameterCount());
        assertEquals(CombatRuntimeCatalog.class, factory.getParameterTypes()[0]);
        assertEquals(ValidatedBossCombatSemantics.class, factory.getParameterTypes()[1]);

        Constructor<?>[] runtimeConstructors = MinecraftBossCombatAdapter.ValidatedRuntime.class.getDeclaredConstructors();
        assertEquals(1, runtimeConstructors.length);
        assertTrue(Modifier.isPrivate(runtimeConstructors[0].getModifiers()),
            "production validated runtime must not be forgeable outside MinecraftBossCombatAdapter");

        Constructor<?>[] resultConstructors = MinecraftBossCombatAdapter.ValidatedTickResult.class.getDeclaredConstructors();
        assertEquals(1, resultConstructors.length);
        assertTrue(Modifier.isPrivate(resultConstructors[0].getModifiers()),
            "validated tick output must only be sealed by the validated runtime");
    }
}
