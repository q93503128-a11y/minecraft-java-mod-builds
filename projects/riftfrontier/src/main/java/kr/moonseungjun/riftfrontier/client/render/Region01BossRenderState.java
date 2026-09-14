package kr.moonseungjun.riftfrontier.client.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

import java.util.UUID;

/** Renderer snapshot for authoritative identity plus local-only field-review presentation state. */
public final class Region01BossRenderState extends EntityRenderState {
    private final Region01BossRenderIdentity identity = new Region01BossRenderIdentity();
    private float fieldReviewPreviewTimeSeconds;
    private int hurtTime;
    private int hurtDuration;
    private int deathTime;

    public int entityId() {
        return identity.entityId();
    }

    public UUID entityUuid() {
        return identity.entityUuid();
    }

    public float fieldReviewPreviewTimeSeconds() {
        return fieldReviewPreviewTimeSeconds;
    }

    public int hurtTime() {
        return hurtTime;
    }

    public int hurtDuration() {
        return hurtDuration;
    }

    public int deathTime() {
        return deathTime;
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

    void setFieldReviewReactionState(int hurtTime, int hurtDuration, int deathTime) {
        if (hurtTime < 0 || hurtDuration < 0 || deathTime < 0) {
            throw new IllegalArgumentException("field review reaction timers must be >= 0");
        }
        this.hurtTime = hurtTime;
        this.hurtDuration = hurtDuration;
        this.deathTime = deathTime;
    }
}
