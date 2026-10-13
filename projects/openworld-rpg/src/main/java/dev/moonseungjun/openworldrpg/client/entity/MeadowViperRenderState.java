package dev.moonseungjun.openworldrpg.client.entity;

import dev.moonseungjun.openworldrpg.entity.MeadowViperEntity;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public final class MeadowViperRenderState extends EntityRenderState {
    public MeadowViperEntity.Phase phase = MeadowViperEntity.Phase.NEUTRAL;
    public float phaseAgeSeconds;
    public float ageSeconds;
    public float bodyYawDegrees;
    public float horizontalSpeed;
    public boolean onGround;
}
