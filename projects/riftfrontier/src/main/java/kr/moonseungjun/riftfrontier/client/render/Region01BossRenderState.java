package kr.moonseungjun.riftfrontier.client.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

import java.util.UUID;

/** Renderer snapshot for authoritative identity plus a local-only field-review preview clock. */
public final class Region01BossRenderState extends EntityRenderState {
    private final Region01BossRenderIdentity identity = new Region01BossRenderIdentity();
    private float fieldReviewPreviewTimeSeconds;

    public int entityId() {
        return identity.entityId();
    }

    public UUID entityUuid() {
        return identity.entityUuid();
    }

    public float fieldReviewPreviewTimeSeconds() {
        return fieldReviewPreviewTimeSeconds;
    }

    boolean identityExtracted() {
        return identity.extracted();
    }

    void setIdentity(int entityId, UUID entityUuid) {
        identity.set(entityId, entityUuid);
    }

    void setFieldReviewPreviewTimeSeconds(float value) {
        if (!Float.isFinite(value) || value < 0.0F) {
            throw new IllegalArgumentException("field review preview time must be finite and >= 0");
        }
        fieldReviewPreviewTimeSeconds = value;
    }
}
