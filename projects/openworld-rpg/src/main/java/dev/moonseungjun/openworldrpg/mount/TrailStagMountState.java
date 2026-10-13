package dev.moonseungjun.openworldrpg.mount;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;

/** Persistent player-owned mount unlock/cooldown authority. Live entity state is intentionally transient. */
public record TrailStagMountState(
        int schemaVersion,
        boolean trailStagUnlocked,
        Optional<String> activeMountId,
        long forcedResummonCooldownUntilTick
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static final Codec<TrailStagMountState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                    .fieldOf("mount_schema_version")
                    .forGetter(TrailStagMountState::schemaVersion),
            Codec.BOOL.optionalFieldOf("trail_stag_unlocked", false)
                    .forGetter(TrailStagMountState::trailStagUnlocked),
            Codec.STRING.optionalFieldOf("active_mount_id")
                    .forGetter(TrailStagMountState::activeMountId),
            Codec.LONG.optionalFieldOf("forced_resummon_cooldown_until_tick", 0L)
                    .forGetter(TrailStagMountState::forcedResummonCooldownUntilTick)
    ).apply(instance, TrailStagMountState::new));

    public TrailStagMountState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported mount-state schema.");
        }
        activeMountId = Objects.requireNonNull(activeMountId, "activeMountId");
        if (forcedResummonCooldownUntilTick < 0L) {
            throw new IllegalArgumentException("Forced mount cooldown tick must be non-negative.");
        }
        if (activeMountId.filter(TrailStagMountRules.TRAIL_STAG_ID::equals).isPresent()
                && !trailStagUnlocked) {
            throw new IllegalArgumentException("Locked Trail Stag cannot be the active mount.");
        }
    }

    public static TrailStagMountState initial() {
        return new TrailStagMountState(
                CURRENT_SCHEMA_VERSION,
                false,
                Optional.empty(),
                0L
        );
    }

    /** First R01 registration is permanent, free, and selects the Stag as the active mount. */
    public TrailStagMountState unlockTrailStag() {
        if (trailStagUnlocked
                && activeMountId.filter(TrailStagMountRules.TRAIL_STAG_ID::equals).isPresent()) {
            return this;
        }
        return new TrailStagMountState(
                schemaVersion,
                true,
                Optional.of(TrailStagMountRules.TRAIL_STAG_ID),
                forcedResummonCooldownUntilTick
        );
    }

    public TrailStagMountState recordResolveBreak(long nowTick) {
        requireTick(nowTick);
        long until = Math.addExact(
                nowTick,
                TrailStagMountRules.RESOLVE_BREAK_RESUMMON_COOLDOWN_TICKS
        );
        return new TrailStagMountState(
                schemaVersion,
                trailStagUnlocked,
                activeMountId,
                Math.max(forcedResummonCooldownUntilTick, until)
        );
    }

    public boolean trailStagSelected() {
        return trailStagUnlocked
                && activeMountId.filter(TrailStagMountRules.TRAIL_STAG_ID::equals).isPresent();
    }

    public long forcedCooldownTicksRemaining(long nowTick) {
        requireTick(nowTick);
        return Math.max(0L, forcedResummonCooldownUntilTick - nowTick);
    }

    private static void requireTick(long tick) {
        if (tick < 0L) {
            throw new IllegalArgumentException("Game tick must be non-negative.");
        }
    }
}
