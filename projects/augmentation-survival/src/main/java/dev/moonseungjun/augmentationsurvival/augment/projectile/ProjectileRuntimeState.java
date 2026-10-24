package dev.moonseungjun.augmentationsurvival.augment.projectile;

import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Mutable per-projectile state. Stored on the arrow entity through a mixin carrier. */
public final class ProjectileRuntimeState {
    private boolean initialized;
    private UUID originalOwnerId;
    private CombatProvenance provenance = CombatProvenance.ROOT_PROJECTILE;
    private double baseDamageScale = 1.0D;
    private byte vanillaPierceLevel;
    private int piercingLevel;
    private int trackingLevel;
    private int ricochetLevel;
    private int chainLevel;
    private int bounceCount;
    private int segmentHitCount;
    private int trackingTargetId = -1;
    private boolean chainUsed;
    private boolean reflected;
    private final Set<Integer> segmentHitEntityIds = new HashSet<>();

    public boolean initialized() {
        return initialized;
    }

    public void initialize(
            UUID ownerId,
            byte vanillaPierceLevel,
            int piercingLevel,
            int trackingLevel,
            int ricochetLevel,
            int chainLevel
    ) {
        this.originalOwnerId = ownerId;
        this.vanillaPierceLevel = vanillaPierceLevel;
        this.piercingLevel = piercingLevel;
        this.trackingLevel = trackingLevel;
        this.ricochetLevel = ricochetLevel;
        this.chainLevel = chainLevel;
        this.initialized = true;
    }

    public UUID originalOwnerId() {
        return originalOwnerId;
    }

    public CombatProvenance provenance() {
        return reflected ? CombatProvenance.REFLECTED : provenance;
    }

    public void setProvenance(CombatProvenance provenance) {
        this.provenance = provenance;
    }

    public double baseDamageScale() {
        return baseDamageScale;
    }

    public void setBaseDamageScale(double baseDamageScale) {
        this.baseDamageScale = baseDamageScale;
    }

    public byte vanillaPierceLevel() {
        return vanillaPierceLevel;
    }

    public int piercingLevel() {
        return piercingLevel;
    }

    public int trackingLevel() {
        return trackingLevel;
    }

    public int ricochetLevel() {
        return ricochetLevel;
    }

    public int chainLevel() {
        return chainLevel;
    }

    public int bounceCount() {
        return bounceCount;
    }

    public void recordBounce() {
        bounceCount++;
        startNewSegment();
    }

    public int segmentHitCount() {
        return segmentHitCount;
    }

    public void recordSegmentHit(int entityId) {
        if (segmentHitEntityIds.add(entityId)) {
            segmentHitCount++;
        }
    }

    public boolean hasHitInCurrentSegment(int entityId) {
        return segmentHitEntityIds.contains(entityId);
    }

    public void startNewSegment() {
        segmentHitEntityIds.clear();
        segmentHitCount = 0;
        trackingTargetId = -1;
    }

    public int trackingTargetId() {
        return trackingTargetId;
    }

    public void setTrackingTargetId(int trackingTargetId) {
        this.trackingTargetId = trackingTargetId;
    }

    public boolean chainUsed() {
        return chainUsed;
    }

    public void markChainUsed() {
        chainUsed = true;
    }

    public boolean reflected() {
        return reflected;
    }

    public void markReflected() {
        reflected = true;
        trackingTargetId = -1;
    }
}
