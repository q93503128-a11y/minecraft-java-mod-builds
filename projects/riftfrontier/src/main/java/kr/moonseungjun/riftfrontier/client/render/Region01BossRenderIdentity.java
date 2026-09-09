package kr.moonseungjun.riftfrontier.client.render;

/**
 * Pure identity invariant shared by the Minecraft client render state and ordinary JVM regression tests.
 * Keeping this value object free of client-only Minecraft supertypes lets the default test source set verify
 * the fail-closed entity-id contract without weakening source-set isolation.
 */
final class Region01BossRenderIdentity {
    private int entityId = -1;

    int entityId() {
        return entityId;
    }

    void setEntityId(int entityId) {
        if (entityId < 0) throw new IllegalArgumentException("entityId must be >= 0");
        this.entityId = entityId;
    }
}
