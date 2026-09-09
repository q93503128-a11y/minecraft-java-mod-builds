package kr.moonseungjun.riftfrontier.client.render;

import java.util.Objects;
import java.util.UUID;

/**
 * Pure identity invariant shared by the Minecraft client render state and ordinary JVM regression tests.
 * Numeric entity ids are not globally stable, so the renderer must carry the matching Minecraft UUID as well.
 */
final class Region01BossRenderIdentity {
    private int entityId = -1;
    private UUID entityUuid;

    int entityId() {
        return entityId;
    }

    UUID entityUuid() {
        if (entityUuid == null) throw new IllegalStateException("entityUuid has not been extracted");
        return entityUuid;
    }

    boolean extracted() {
        return entityId >= 0 && entityUuid != null;
    }

    void set(int entityId, UUID entityUuid) {
        if (entityId < 0) throw new IllegalArgumentException("entityId must be >= 0");
        this.entityId = entityId;
        this.entityUuid = Objects.requireNonNull(entityUuid, "entityUuid");
    }
}
