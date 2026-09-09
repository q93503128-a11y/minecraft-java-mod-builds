package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Minecraft-API-free semantic state shared by the server presentation frame, network transport and client resolver.
 * This deliberately contains no animation, VFX, sound or damage timing constants.
 */
public record BossPresentationSemanticState(
    int entityId,
    UUID entityUuid,
    long serverGameTick,
    boolean active,
    int bossPhase,
    String patternId,
    String attackPhase,
    double phaseProgress,
    String presentationCue,
    String delivery,
    List<String> counterplay,
    boolean hitWindowOpen
) {
    public static final int MAX_COUNTERPLAY = 16;

    public BossPresentationSemanticState {
        if (entityId < 0) throw new IllegalArgumentException("entityId must be >= 0");
        entityUuid = Objects.requireNonNull(entityUuid, "entityUuid");
        if (serverGameTick < 0) throw new IllegalArgumentException("serverGameTick must be >= 0");
        patternId = Objects.requireNonNull(patternId, "patternId");
        attackPhase = Objects.requireNonNull(attackPhase, "attackPhase");
        presentationCue = Objects.requireNonNull(presentationCue, "presentationCue");
        delivery = Objects.requireNonNull(delivery, "delivery");
        counterplay = List.copyOf(Objects.requireNonNull(counterplay, "counterplay"));
        if (counterplay.size() > MAX_COUNTERPLAY) throw new IllegalArgumentException("too many counterplay semantics");
        if (!Double.isFinite(phaseProgress) || phaseProgress < 0.0D || phaseProgress > 1.0D) {
            throw new IllegalArgumentException("phaseProgress must be finite and between 0 and 1");
        }
        if (active) {
            if (bossPhase <= 0) throw new IllegalArgumentException("active presentation requires a positive boss phase");
            ContentId.parse(patternId);
            AttackTimeline.Phase phase = AttackTimeline.Phase.valueOf(attackPhase);
            if (phase == AttackTimeline.Phase.COMPLETE) throw new IllegalArgumentException("COMPLETE is not an active presentation phase");
            if (hitWindowOpen != (phase == AttackTimeline.Phase.ACTIVE)) {
                throw new IllegalArgumentException("hitWindowOpen must exactly match ACTIVE phase");
            }
        } else if (bossPhase != 0 || !patternId.isEmpty() || !attackPhase.isEmpty() || hitWindowOpen) {
            throw new IllegalArgumentException("inactive presentation must use the canonical clear state");
        }
    }

    /** Compatibility constructor for pure-Java fixtures that do not model a Minecraft UUID. */
    public BossPresentationSemanticState(
        int entityId,
        long serverGameTick,
        boolean active,
        int bossPhase,
        String patternId,
        String attackPhase,
        double phaseProgress,
        String presentationCue,
        String delivery,
        List<String> counterplay,
        boolean hitWindowOpen
    ) {
        this(entityId, fixtureUuid(entityId), serverGameTick, active, bossPhase, patternId, attackPhase, phaseProgress,
            presentationCue, delivery, counterplay, hitWindowOpen);
    }

    public static BossPresentationSemanticState fromFrame(
        int entityId,
        UUID entityUuid,
        long serverGameTick,
        MinecraftBossCombatAdapter.PresentationFrame frame
    ) {
        Objects.requireNonNull(frame, "frame");
        return new BossPresentationSemanticState(
            entityId,
            entityUuid,
            serverGameTick,
            true,
            frame.bossPhase(),
            frame.patternId().toString(),
            frame.attackPhase().name(),
            frame.phaseProgress(),
            frame.presentationCue(),
            frame.delivery(),
            frame.counterplay().stream().sorted().toList(),
            frame.hitWindowOpen()
        );
    }

    /** Compatibility factory for API-free contract tests. Production networking always supplies the Minecraft UUID. */
    public static BossPresentationSemanticState fromFrame(
        int entityId,
        long serverGameTick,
        MinecraftBossCombatAdapter.PresentationFrame frame
    ) {
        return fromFrame(entityId, fixtureUuid(entityId), serverGameTick, frame);
    }

    public static BossPresentationSemanticState clear(int entityId, UUID entityUuid, long serverGameTick) {
        return new BossPresentationSemanticState(
            entityId, entityUuid, serverGameTick, false, 0, "", "", 0.0D, "", "", List.of(), false
        );
    }

    /** Compatibility clear for pure-Java fixtures. Production networking always supplies the Minecraft UUID. */
    public static BossPresentationSemanticState clear(int entityId, long serverGameTick) {
        return clear(entityId, fixtureUuid(entityId), serverGameTick);
    }

    private static UUID fixtureUuid(int entityId) {
        if (entityId < 0) throw new IllegalArgumentException("entityId must be >= 0");
        return new UUID(0L, Integer.toUnsignedLong(entityId) + 1L);
    }
}
