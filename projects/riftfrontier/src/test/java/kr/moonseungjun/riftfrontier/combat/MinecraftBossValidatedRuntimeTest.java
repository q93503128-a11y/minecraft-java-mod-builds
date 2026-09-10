package kr.moonseungjun.riftfrontier.combat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-Java structural contract for the production boss runtime boundary.
 *
 * <p>The Gradle JUnit runtime intentionally does not include Minecraft runtime classes. Loading
 * {@code MinecraftBossCombatAdapter} reflectively therefore defeats the purpose of this boundary test because the JVM
 * resolves its Minecraft-typed method descriptors while loading the class. Native GameTest owns behavior that needs
 * Minecraft classes; this test reads the production source without loading the adapter and verifies the sealing
 * declarations that must remain present.</p>
 */
final class MinecraftBossValidatedRuntimeTest {
    private static final Path ADAPTER_SOURCE = Path.of(
        "src/main/java/kr/moonseungjun/riftfrontier/combat/MinecraftBossCombatAdapter.java"
    );

    @Test
    void productionFactoryRequiresValidatedSemanticsAndReturnsSealedRuntime() throws IOException {
        String source = normalizeWhitespace(Files.readString(ADAPTER_SOURCE));

        assertTrue(source.contains(
                "public static ValidatedRuntime validated( CombatRuntimeCatalog catalog, ValidatedBossCombatSemantics validatedSemantics,"),
            "production factory must accept validated boss semantics and return the sealed runtime");
        assertTrue(source.contains("private ValidatedRuntime("),
            "production validated runtime must not be forgeable outside MinecraftBossCombatAdapter");
        assertTrue(source.contains("private ValidatedTickResult("),
            "validated tick output must only be sealed by the validated runtime");
    }

    private static String normalizeWhitespace(String source) {
        return source.replaceAll("\\s+", " ").trim();
    }
}
