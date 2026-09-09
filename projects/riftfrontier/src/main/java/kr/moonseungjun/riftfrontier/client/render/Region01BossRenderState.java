package kr.moonseungjun.riftfrontier.client.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** Renderer snapshot containing only identity needed to resolve the server-authoritative presentation cache. */
public final class Region01BossRenderState extends EntityRenderState {
    private int entityId = -1;

    public int entityId() {
        return entityId;
    }

    void setEntityId(int entityId) {
        if (entityId < 0) throw new IllegalArgumentException("entityId must be >= 0");
        this.entityId = entityId;
    }
}
