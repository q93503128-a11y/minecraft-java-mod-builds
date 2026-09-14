package kr.moonseungjun.turnboundre.presentation;

/** Optional presentation hook for virtual entities that need more than the vanilla aggressive flag. */
public interface PresentationPoseAware {
    TurnboundPresentationPose presentationPose();

    void setPresentationPose(TurnboundPresentationPose pose);
}
