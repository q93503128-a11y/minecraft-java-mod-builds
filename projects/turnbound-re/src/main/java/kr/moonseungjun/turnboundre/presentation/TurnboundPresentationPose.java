package kr.moonseungjun.turnboundre.presentation;

/**
 * Presentation-only pose families that a custom virtual-stage entity may choose to interpret.
 * They never carry combat legality, damage, target or progression state.
 */
public enum TurnboundPresentationPose {
    NEUTRAL,
    OFFENSIVE,
    DEFENSIVE,
    SLAM,
    EXECUTE,
    CHARGE,
    BLAST,
    CATASTROPHE,
    VENOM,
    WEB,
    POUNCE
}
