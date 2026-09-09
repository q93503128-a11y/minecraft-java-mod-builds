package kr.moonseungjun.riftfrontier.client.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** Renderer snapshot containing only identity needed to resolve the server-authoritative presentation cache. */
public final class Region01BossRenderState extends EntityRenderState {
    private final Region01BossRenderIdentity identity = new Region01BossRenderIdentity();

    public int entityId() {
        return identity.entityId();
    }

    void setEntityId(int entityId) {
        identity.setEntityId(entityId);
    }
}
