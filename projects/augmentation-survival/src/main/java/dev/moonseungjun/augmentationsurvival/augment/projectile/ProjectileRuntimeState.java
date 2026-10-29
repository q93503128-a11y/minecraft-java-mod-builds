package dev.moonseungjun.augmentationsurvival.augment.projectile;

import com.mojang.serialization.Codec;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Mutable per-projectile state. Stored on the arrow entity through a mixin carrier. */
public final class ProjectileRuntimeState {
    private static final String SAVE_PREFIX = "augmentation_survival_projectile_";
    private static final int MAX_PERSISTED_SEGMENT_HITS = 128;
    private static final int MAX_PERSISTED_LEVEL = 3;

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
    private UUID trackingTargetId;
    private boolean chainUsed;
    private boolean reflected;
    private final Set<UUID> segmentHitEntityIds = new HashSet<>();

    public void write(ValueOutput output) {
        output.putBoolean(SAVE_PREFIX + "present", true);
        output.putBoolean(SAVE_PREFIX + "initialized", initialized);
        output.putString(SAVE_PREFIX + "owner", originalOwnerId == null ? "" : originalOwnerId.toString());
        output.putString(SAVE_PREFIX + "provenance", provenance.name());
        output.putDouble(SAVE_PREFIX + "base_damage_scale", baseDamageScale);
        output.putByte(SAVE_PREFIX + "vanilla_pierce", vanillaPierceLevel);
        output.putInt(SAVE_PREFIX + "piercing_level", piercingLevel);
        output.putInt(SAVE_PREFIX + "tracking_level", trackingLevel);
        output.putInt(SAVE_PREFIX + "ricochet_level", ricochetLevel);
        output.putInt(SAVE_PREFIX + "chain_level", chainLevel);
        output.putInt(SAVE_PREFIX + "bounce_count", bounceCount);
        output.putInt(SAVE_PREFIX + "segment_hit_count", segmentHitCount);
        output.putString(SAVE_PREFIX + "tracking_target", trackingTargetId == null ? "" : trackingTargetId.toString());
        output.putBoolean(SAVE_PREFIX + "chain_used", chainUsed);
        output.putBoolean(SAVE_PREFIX + "reflected", reflected);
        ValueOutput.TypedOutputList<String> hitIds = output.list(
                SAVE_PREFIX + "segment_hit_entity_uuids", Codec.STRING
        );
        int persistedHits = 0;
        for (UUID entityId : segmentHitEntityIds) {
            if (persistedHits++ >= MAX_PERSISTED_SEGMENT_HITS) break;
            hitIds.add(entityId.toString());
        }
    }

    public static ProjectileRuntimeState read(ValueInput input) {
        if (!input.getBooleanOr(SAVE_PREFIX + "present", false)) {
            return null;
        }

        ProjectileRuntimeState state = new ProjectileRuntimeState();
        state.initialized = input.getBooleanOr(SAVE_PREFIX + "initialized", false);
        state.originalOwnerId = parseUuid(input.getStringOr(SAVE_PREFIX + "owner", ""));
        state.provenance = parseProvenance(input.getStringOr(SAVE_PREFIX + "provenance", "ROOT_PROJECTILE"));
        double savedScale = input.getDoubleOr(SAVE_PREFIX + "base_damage_scale", 1.0D);
        state.baseDamageScale = Double.isFinite(savedScale) && savedScale >= 0.0D
                ? Math.min(savedScale, 64.0D)
                : 1.0D;
        state.vanillaPierceLevel = (byte) Math.max(
                0,
                Math.min(127, Byte.toUnsignedInt(input.getByteOr(SAVE_PREFIX + "vanilla_pierce", (byte) 0)))
        );
        state.piercingLevel = boundedSavedLevel(input, "piercing_level");
        state.trackingLevel = boundedSavedLevel(input, "tracking_level");
        state.ricochetLevel = boundedSavedLevel(input, "ricochet_level");
        state.chainLevel = boundedSavedLevel(input, "chain_level");
        state.bounceCount = Math.max(0, Math.min(3, input.getIntOr(SAVE_PREFIX + "bounce_count", 0)));
        state.segmentHitCount = Math.max(
                0,
                Math.min(MAX_PERSISTED_SEGMENT_HITS, input.getIntOr(SAVE_PREFIX + "segment_hit_count", 0))
        );
        state.trackingTargetId = parseUuid(input.getStringOr(SAVE_PREFIX + "tracking_target", ""));
        state.chainUsed = input.getBooleanOr(SAVE_PREFIX + "chain_used", false);
        state.reflected = input.getBooleanOr(SAVE_PREFIX + "reflected", false);
        for (String encoded : input.listOrEmpty(SAVE_PREFIX + "segment_hit_entity_uuids", Codec.STRING)) {
            if (state.segmentHitEntityIds.size() >= MAX_PERSISTED_SEGMENT_HITS) {
                break;
            }
            UUID entityId = parseUuid(encoded);
            if (entityId != null) {
                state.segmentHitEntityIds.add(entityId);
            }
        }
        if (!state.initialized || state.originalOwnerId == null) {
            state.initialized = false;
            state.originalOwnerId = null;
            state.segmentHitEntityIds.clear();
            state.segmentHitCount = 0;
            state.trackingTargetId = null;
        }
        return state;
    }

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

    public void recordSegmentHit(UUID entityId) {
        if (segmentHitEntityIds.add(entityId)) {
            segmentHitCount++;
        }
    }

    public boolean hasHitInCurrentSegment(UUID entityId) {
        return segmentHitEntityIds.contains(entityId);
    }

    public void startNewSegment() {
        segmentHitEntityIds.clear();
        segmentHitCount = 0;
        trackingTargetId = null;
    }

    public UUID trackingTargetId() {
        return trackingTargetId;
    }

    public void setTrackingTargetId(UUID trackingTargetId) {
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
        trackingTargetId = null;
    }

    private static int boundedSavedLevel(ValueInput input, String field) {
        return Math.max(0, Math.min(MAX_PERSISTED_LEVEL, input.getIntOr(SAVE_PREFIX + field, 0)));
    }

    private static UUID parseUuid(String encoded) {
        if (encoded.isEmpty() || encoded.length() > 36) {
            return null;
        }
        try {
            return UUID.fromString(encoded);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static CombatProvenance parseProvenance(String encoded) {
        try {
            return CombatProvenance.valueOf(encoded);
        } catch (IllegalArgumentException ignored) {
            return CombatProvenance.ROOT_PROJECTILE;
        }
    }
}
