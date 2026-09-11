package kr.moonseungjun.riftfrontier.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure-Java structural contract for client presentation lifecycle retirement without loading NeoForge client classes. */
final class RiftfrontierClientPresentationLifecycleTest {
    private static final Path NETWORKING_SOURCE = Path.of(
        "src/main/java/kr/moonseungjun/riftfrontier/client/RiftfrontierClientNetworking.java"
    );
    private static final Path RESOURCE_SOURCE = Path.of(
        "src/main/java/kr/moonseungjun/riftfrontier/client/RiftfrontierClientResources.java"
    );

    @Test
    void clientEntityLeavePrunesExactUuidWhileDisconnectRetiresWholeConnectionEpoch() throws IOException {
        String networking = normalizeWhitespace(Files.readString(NETWORKING_SOURCE));
        String resources = normalizeWhitespace(Files.readString(RESOURCE_SOURCE));

        assertTrue(networking.contains("private static void entityLeavingLevel(EntityLeaveLevelEvent event)"),
            "client lifecycle must subscribe to entity leave events");
        assertTrue(networking.contains("if (!event.getLevel().isClientSide()) return;"),
            "integrated-server logical-side entity leaves must not mutate the client cache");
        assertTrue(networking.contains(
                "BossPresentationClientState.forgetActor(event.getEntity().getId(), event.getEntity().getUUID());"),
            "entity leave must retire the exact numeric-id + UUID actor epoch");
        assertTrue(networking.contains("BossPresentationClientState.clearAll();"),
            "disconnect must clear every semantic ordering watermark before a new server epoch");
        assertTrue(networking.contains("RiftfrontierClientResources.retireBossPresentationConnectionEpoch();"),
            "disconnect must also retire reviewed render capabilities from the previous server connection");
        assertTrue(resources.contains("static void retireBossPresentationConnectionEpoch() { clearPreparedPresentation(); }"),
            "connection retirement must reuse the full prepared/published presentation invalidation boundary");
        assertTrue(resources.contains("Region01BossClientRenderRuntime.clear();"),
            "connection retirement must invalidate the renderer-visible publication slot, not only cached semantics");
    }

    private static String normalizeWhitespace(String source) {
        return source.replaceAll("\\s+", " ").trim();
    }
}
