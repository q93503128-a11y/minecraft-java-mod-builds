package kr.moonseungjun.riftfrontier.combat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure-Java source contract for event-driven validated boss owner lifetime sealing. */
final class BossRuntimeLifecycleContractTest {
    private static final Path ADAPTER_SOURCE = Path.of(
        "src/main/java/kr/moonseungjun/riftfrontier/combat/MinecraftBossCombatAdapter.java"
    );
    private static final Path MOD_SOURCE = Path.of(
        "src/main/java/kr/moonseungjun/riftfrontier/Riftfrontier.java"
    );

    @Test
    void validatedRuntimeIsEntityLifetimeBoundAndLeaveEventIsRegistered() throws IOException {
        String adapter = normalizeWhitespace(Files.readString(ADAPTER_SOURCE));
        String mod = normalizeWhitespace(Files.readString(MOD_SOURCE));

        assertTrue(adapter.contains("private LivingEntity minecraftOwner;"),
            "validated runtime must retain the exact Minecraft owner instance");
        assertTrue(adapter.contains("private ResourceKey<Level> minecraftOwnerDimension;"),
            "validated runtime owner must remain dimension-scoped");
        assertTrue(adapter.contains("private boolean minecraftOwnerInvalidated;"),
            "validated runtime must retain irreversible owner invalidation state");
        assertTrue(adapter.contains("private boolean minecraftOwnerGenerationRetired;"),
            "stale publication retirement must be irreversible for the retained capability");
        assertTrue(adapter.contains("requireMinecraftOwner(level, boss);"),
            "Minecraft-facing begin/tick/phase mutation paths must cross the owner lifetime gate");
        assertTrue(adapter.contains("requireDetachedMutation(\"begin attack\");"),
            "ownerless attack mutation must be sealed after Minecraft ownership is established");
        assertTrue(adapter.contains("requireDetachedMutation(\"transition phase\");"),
            "ownerless phase mutation must be sealed after Minecraft ownership is established");
        assertTrue(adapter.contains(
                "public BossCombatController.PhaseTransition transitionToPhase( ServerLevel level, LivingEntity boss, int newPhase )"),
            "Minecraft-bound phase transitions must require explicit authoritative level and actor context");
        assertTrue(adapter.contains("if (existing != runtime && existing.retireIfGenerationStale()) { iterator.remove(); }"),
            "a fresh claim must prune stale-generation owner slots before enforcing singleton ownership");
        assertTrue(adapter.contains("if (!runtimes.isEmpty() && !runtimes.contains(runtime))"),
            "one current-generation live boss entity must still reject a second independently mutable validated runtime");
        assertTrue(adapter.contains("bindValidatedRuntimeOwner(boss, this); minecraftOwner = boss;"),
            "owner claim must succeed before the candidate runtime records itself as bound");
        assertTrue(adapter.contains("retireStaleGenerationOwnerClaim(); throw stale;"),
            "a stale runtime discovered through its own API must release its process-local singleton claim");
        assertTrue(adapter.contains("releaseValidatedRuntimeOwner(owner, this);"),
            "generation retirement must remove exactly the stale capability rather than clearing a newer owner claim");
        assertTrue(adapter.contains("public static void entityLeaveLevel(EntityLeaveLevelEvent event)"),
            "validated runtime must expose the event-driven owner retirement boundary");
        assertTrue(adapter.contains("runtimes = VALIDATED_RUNTIMES_BY_OWNER.remove(owner);"),
            "entity leave must detach the process-local capability for the exact owner instance");
        assertTrue(adapter.contains("runtime.invalidateMinecraftOwner(owner);"),
            "entity leave must invalidate retained capabilities, not merely cancel one attack");
        assertTrue(mod.contains("NeoForge.EVENT_BUS.addListener(MinecraftBossCombatAdapter::entityLeaveLevel);"),
            "production mod bootstrap must register the leave boundary on the main NeoForge event bus");
    }

    private static String normalizeWhitespace(String source) {
        return source.replaceAll("\\s+", " ").trim();
    }
}
