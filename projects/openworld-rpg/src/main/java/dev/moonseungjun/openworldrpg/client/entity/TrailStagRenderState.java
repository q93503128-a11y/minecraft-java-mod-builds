package dev.moonseungjun.openworldrpg.client.entity;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

public final class TrailStagRenderState extends EntityRenderState {
    public float ageSeconds;
    public float bodyYawDegrees;
    public float horizontalSpeed;
    public float verticalVelocity;
    public boolean onGround;
    public boolean authoredAirborne;
}
