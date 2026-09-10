package kr.moonseungjun.riftfrontier.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure-Java structural contract for client entity lifecycle pruning without loading NeoForge client classes. */
final class RiftfrontierClientPresentationLifecycleTest {
    private static final Path NETWORKING_SOURCE = Path.of(
        "src/main/java/kr/moonseungjun/riftfrontier/client/RiftfrontierClientNetworking.java"
    );

    @Test
    void clientEntityLeavePrunesExactUuidWhileDisconnectStillClearsAll() throws IOException {
        String source = normalizeWhitespace(Files.readString(NETWORKING_SOURCE));

        assertTrue(source.contains("private static void entityLeavingLevel(EntityLeaveLevelEvent event)"),
            "client lifecycle must subscribe to entity leave events");
        assertTrue(source.contains("if (!event.getLevel().isClientSide()) return;"),
            "integrated-server logical-side entity leaves must not mutate the client cache");
        assertTrue(source.contains(
                "BossPresentationClientState.forgetActor(event.getEntity().getId(), event.getEntity().getUUID());"),
            "entity leave must retire the exact numeric-id + UUID actor epoch");
        assertTrue(source.contains("BossPresentationClientState.clearAll();"),
            "disconnect must continue clearing every presentation watermark");
    }

    private static String normalizeWhitespace(String source) {
        return source.replaceAll("\\s+", " ").trim();
    }
}
