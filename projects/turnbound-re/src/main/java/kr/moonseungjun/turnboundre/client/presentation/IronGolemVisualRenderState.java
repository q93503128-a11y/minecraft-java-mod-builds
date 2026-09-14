package kr.moonseungjun.turnboundre.client.presentation;

import kr.moonseungjun.turnboundre.presentation.TurnboundPresentationPose;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Render-state additions that belong only to the TURNBOUND Iron Golem visual. */
public final class IronGolemVisualRenderState extends LivingEntityRenderState {
    public TurnboundPresentationPose presentationPose = TurnboundPresentationPose.NEUTRAL;
    public float presentationTime;
}
