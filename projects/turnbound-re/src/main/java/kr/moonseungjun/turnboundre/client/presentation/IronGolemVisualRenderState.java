package kr.moonseungjun.turnboundre.client.presentation;

import kr.moonseungjun.turnboundre.presentation.TurnboundPresentationPose;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;

/** Render-state additions that belong only to the TURNBOUND Iron Golem visual. */
public final class IronGolemVisualRenderState extends IronGolemRenderState {
    public TurnboundPresentationPose presentationPose = TurnboundPresentationPose.NEUTRAL;
    public float presentationTime;
}
