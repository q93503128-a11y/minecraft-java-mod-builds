package kr.moonseungjun.riftfrontier.combat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Structural contract for published-generation retirement in the Minecraft player weapon adapter. */
final class MinecraftPlayerWeaponGenerationContractTest {
    private static final Path ADAPTER_SOURCE = Path.of(
        "src/main/java/kr/moonseungjun/riftfrontier/combat/MinecraftPlayerWeaponCombatAdapter.java"
    );

    @Test
    void publishedCatalogCannotContinuePlayerCombatAfterAtomicReload() throws IOException {
        String source = normalizeWhitespace(Files.readString(ADAPTER_SOURCE));

        assertTrue(source.contains(
                "private final Optional<PublishedContentGenerationGuard> generationGuard;"),
            "player weapon adapter must retain the publication generation that produced its combat catalog");
        assertTrue(source.contains(
                "this.generationGuard = PublishedContentGenerationGuard.fromCatalog(catalog);"),
            "published player combat must inherit the catalog generation guard");
        assertTrue(source.contains(
                "requireCurrentGeneration(actor.getUUID()); Loadout loadout = requireLoadout(actor);"),
            "new move intents must reject stale catalogs before resolving/starting authored move state");
        assertTrue(source.contains(
                "if (!isCurrentGeneration()) { invalidate(actor.getUUID(), session); return TickResult.invalidated(); }"),
            "active sessions must be cancelled and discarded before a stale timeline can advance");
        assertTrue(source.contains(
                "if (!isCurrentGeneration()) { invalidate(actor.getUUID(), session); return false; }"),
            "recovery-only module authority must disappear with the stale publication generation");
        assertTrue(source.contains(
                "PlayerWeaponCombatController controller = catalog.playerWeaponController(loadout.familyId(), loadout.moduleId()); requireCurrentGeneration(actor.getUUID());"),
            "session creation must re-check publication after assembling the immutable profile so reload cannot publish a stale replacement session");
        assertTrue(source.contains(
                "clearActor(actorId); throw stale;"),
            "a stale begin path must fail closed and clear any retained execution for that actor");
    }

    private static String normalizeWhitespace(String source) {
        return source.replaceAll("\\s+", " ").trim();
    }
}
