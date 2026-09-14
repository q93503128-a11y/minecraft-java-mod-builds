package kr.moonseungjun.turnboundre.client.presentation;

import kr.moonseungjun.turnboundre.presentation.TurnboundPresentationPose;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Render-state additions that belong only to the TURNBOUND Creeper visual. */
public final class CreeperVisualRenderState extends LivingEntityRenderState {
    public TurnboundPresentationPose presentationPose = TurnboundPresentationPose.NEUTRAL;
    public boolean volatileCharged;
    public float presentationTime;
}
