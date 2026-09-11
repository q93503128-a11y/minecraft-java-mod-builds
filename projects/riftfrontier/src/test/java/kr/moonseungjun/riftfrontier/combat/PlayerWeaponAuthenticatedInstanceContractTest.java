package kr.moonseungjun.riftfrontier.combat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Source contract for the authenticated ServerPlayer exact-instance authority fence. */
final class PlayerWeaponAuthenticatedInstanceContractTest {
    private static final Path RUNTIME_SOURCE = Path.of(
        "src/main/java/kr/moonseungjun/riftfrontier/combat/PlayerWeaponServerRuntime.java"
    );

    @Test
    void staleServerPlayerCallbacksCannotReplaceSuccessorCombatAuthority() throws IOException {
        String runtime = normalizeWhitespace(Files.readString(RUNTIME_SOURCE));

        assertTrue(runtime.contains("private static boolean isCurrentServerPlayerInstance(ServerPlayer player)"),
            "production player combat must expose one exact-instance admission fence");
        assertTrue(runtime.contains("if (!(player.level() instanceof ServerLevel level)) return false;"),
            "player authority must remain confined to a server level");
        assertTrue(runtime.contains("return level.getEntity(player.getUUID()) == player;"),
            "the admitted actor must be the exact entity currently registered for its UUID");
        assertTrue(occurrences(runtime, "if (!isCurrentServerPlayerInstance(player))") == 2,
            "both move-intent creation and per-player ticking must revalidate the exact authenticated instance");
        assertTrue(runtime.contains("if (!isCurrentServerPlayerInstance(player)) { clearPlayer(player); return IntentResult.REJECTED; }"),
            "a delayed stale move intent must fail closed and may clear only its own exact-instance session");
        assertTrue(runtime.contains("return clearPlayer(player) ? MinecraftPlayerWeaponCombatAdapter.TickResult.invalidated() : MinecraftPlayerWeaponCombatAdapter.TickResult.idle();"),
            "a stale tick callback must retire only an exact stale session and never mutate a successor session");
        assertTrue(!runtime.contains("if (!isCurrentServerPlayerInstance(player)) { clearPlayer(player.getUUID());"),
            "stale callbacks must never use UUID-wide cleanup because that could erase a successor instance");
    }

    private static String normalizeWhitespace(String source) {
        return source.replaceAll("\\s+", " ").trim();
    }

    private static int occurrences(String haystack, String needle) {
        int count = 0;
        int from = 0;
        while ((from = haystack.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
    }
}
