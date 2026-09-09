package kr.moonseungjun.riftfrontier.client.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

import java.util.UUID;

/** Renderer snapshot containing only identity needed to resolve the server-authoritative presentation cache. */
public final class Region01BossRenderState extends EntityRenderState {
    private final Region01BossRenderIdentity identity = new Region01BossRenderIdentity();

    public int entityId() {
        return identity.entityId();
    }

    public UUID entityUuid() {
        return identity.entityUuid();
    }

    boolean identityExtracted() {
        return identity.extracted();
    }

    void setIdentity(int entityId, UUID entityUuid) {
        identity.set(entityId, entityUuid);
    }
}
