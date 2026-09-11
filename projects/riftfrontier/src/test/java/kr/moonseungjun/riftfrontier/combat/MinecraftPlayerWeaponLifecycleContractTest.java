package kr.moonseungjun.riftfrontier.combat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Structural contract for exact-instance player combat lifecycle retirement. */
final class MinecraftPlayerWeaponLifecycleContractTest {
    private static final Path ADAPTER_SOURCE = Path.of(
        "src/main/java/kr/moonseungjun/riftfrontier/combat/MinecraftPlayerWeaponCombatAdapter.java"
    );
    private static final Path RUNTIME_SOURCE = Path.of(
        "src/main/java/kr/moonseungjun/riftfrontier/combat/PlayerWeaponServerRuntime.java"
    );
    private static final Path MOD_SOURCE = Path.of(
        "src/main/java/kr/moonseungjun/riftfrontier/Riftfrontier.java"
    );

    @Test
    void staleLifecycleEventsCannotCancelSuccessorPlayerInstance() throws IOException {
        String adapter = normalizeWhitespace(Files.readString(ADAPTER_SOURCE));
        String runtime = normalizeWhitespace(Files.readString(RUNTIME_SOURCE));
        String mod = normalizeWhitespace(Files.readString(MOD_SOURCE));

        assertTrue(adapter.contains(
                "public boolean clearActor(LivingEntity actor)"),
            "player combat adapter must expose exact-instance lifecycle cleanup");
        assertTrue(adapter.contains(
                "if (session == null || session.actor != actor) return false; invalidate(actor.getUUID(), session);"),
            "a delayed event from an old entity instance must not clear the current same-UUID successor session");
        assertTrue(runtime.contains(
                "return current != null && current.adapter.clearActor(player);"),
            "normal lifecycle cleanup must use exact player-instance ownership");
        assertTrue(runtime.contains(
                "return current != null && current.adapter.clearActor(playerId);"),
            "an explicit UUID-wide fence must remain available for reconnect/server identity boundaries");
        assertTrue(runtime.contains(
                "public static void entityLeaveLevel(EntityLeaveLevelEvent event) { if (event.getEntity() instanceof ServerPlayer player) clearPlayer(player); }"),
            "entity removal must retire process-local combat state through the exact-instance path");
        assertTrue(runtime.contains(
                "public static void serverStopped(ServerStoppedEvent event) { clearAllForContentBoundary(); }"),
            "process-local player combat capability must not survive the authoritative server lifetime");
        assertTrue(mod.contains(
                "PlayerWeaponServerRuntime.clearPlayer(player.getUUID());"),
            "login must remain a deliberate UUID-wide reconnect fence before new authority is established");
        assertTrue(mod.contains(
                "PlayerWeaponServerRuntime.clearPlayer(player);"),
            "logout and dimension lifecycle handlers must use exact-instance cleanup");
        assertTrue(mod.contains(
                "PlayerWeaponServerRuntime.clearPlayer(original);"),
            "clone retirement must target the exact predecessor instance instead of the shared UUID");
        assertTrue(mod.contains(
                "NeoForge.EVENT_BUS.addListener(PlayerWeaponServerRuntime::entityLeaveLevel);"),
            "entity-leave cleanup must be wired to the NeoForge event bus");
        assertTrue(mod.contains(
                "NeoForge.EVENT_BUS.addListener(PlayerWeaponServerRuntime::serverStopped);"),
            "server-stop cleanup must be wired to the NeoForge event bus");
    }

    private static String normalizeWhitespace(String source) {
        return source.replaceAll("\\s+", " ").trim();
    }
}
