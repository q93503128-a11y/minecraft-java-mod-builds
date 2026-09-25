package dev.moonseungjun.openworldrpg.progression.r01;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.Objects;
import java.util.Optional;

/**
 * Bounded persistent coordinator for repeatable R01 rewards.
 *
 * <p>Unlike one-time reward history, this stores only the last committed Roadside cycle and at
 * most one pending plan, preventing an infinite set of transaction IDs in long-lived saves.</p>
 */
public record R01RepeatRewardState(
        int schemaVersion,
        long lastRoadsideRewardedCycle,
        Optional<RoadsideRewardPlan> pendingRoadsideReward
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static final Codec<R01RepeatRewardState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("repeat_reward_schema_version")
                            .forGetter(R01RepeatRewardState::schemaVersion),
                    Codec.LONG.fieldOf("last_roadside_rewarded_cycle")
                            .forGetter(R01RepeatRewardState::lastRoadsideRewardedCycle),
                    RoadsideRewardPlan.CODEC.optionalFieldOf("pending_roadside_reward")
                            .forGetter(R01RepeatRewardState::pendingRoadsideReward)
            ).apply(instance, R01RepeatRewardState::new));

    public R01RepeatRewardState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported repeat-reward schema version: " + schemaVersion
            );
        }
        if (lastRoadsideRewardedCycle < 0L) {
            throw new IllegalArgumentException(
                    "lastRoadsideRewardedCycle must be non-negative."
            );
        }
        pendingRoadsideReward = Objects.requireNonNull(
                pendingRoadsideReward,
                "pendingRoadsideReward"
        );
        pendingRoadsideReward.ifPresent(plan -> {
            if (plan.cycle() <= lastRoadsideRewardedCycle) {
                throw new IllegalArgumentException(
                        "Pending Roadside reward must be newer than committed cycle."
                );
            }
        });
    }

    public static R01RepeatRewardState initial() {
        return new R01RepeatRewardState(
                CURRENT_SCHEMA_VERSION,
                0L,
                Optional.empty()
        );
    }

    public R01RepeatRewardState beginRoadside(RoadsideRewardPlan plan) {
        Objects.requireNonNull(plan, "plan");
        if (plan.cycle() <= lastRoadsideRewardedCycle) {
            return this;
        }
        if (pendingRoadsideReward.isPresent()) {
            RoadsideRewardPlan current = pendingRoadsideReward.orElseThrow();
            if (!current.equals(plan)) {
                throw new IllegalStateException(
                        "Cannot replace an unresolved Roadside reward plan."
                );
            }
            return this;
        }
        return new R01RepeatRewardState(
                schemaVersion,
                lastRoadsideRewardedCycle,
                Optional.of(plan)
        );
    }

    public R01RepeatRewardState completeRoadside(long cycle) {
        if (cycle <= lastRoadsideRewardedCycle) {
            return this;
        }
        RoadsideRewardPlan plan = pendingRoadsideReward.orElseThrow(
                () -> new IllegalStateException(
                        "Cannot complete Roadside reward without a pending plan."
                )
        );
        if (plan.cycle() != cycle) {
            throw new IllegalStateException(
                    "Roadside reward cycle changed before completion."
            );
        }
        return new R01RepeatRewardState(
                schemaVersion,
                cycle,
                Optional.empty()
        );
    }

    public record RoadsideRewardPlan(
            long cycle,
            long combatXp,
            Optional<RootClass> rewardClass,
            long classXp,
            long gold
    ) {
        public static final Codec<RoadsideRewardPlan> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.LONG.fieldOf("cycle")
                                .forGetter(RoadsideRewardPlan::cycle),
                        Codec.LONG.fieldOf("combat_xp")
                                .forGetter(RoadsideRewardPlan::combatXp),
                        RootClass.CODEC.optionalFieldOf("reward_class")
                                .forGetter(RoadsideRewardPlan::rewardClass),
                        Codec.LONG.fieldOf("class_xp")
                                .forGetter(RoadsideRewardPlan::classXp),
                        Codec.LONG.fieldOf("gold")
                                .forGetter(RoadsideRewardPlan::gold)
                ).apply(instance, RoadsideRewardPlan::new));

        public RoadsideRewardPlan {
            if (cycle <= 0L) {
                throw new IllegalArgumentException("Roadside reward cycle must be positive.");
            }
            if (combatXp < 0L || classXp < 0L || gold < 0L) {
                throw new IllegalArgumentException("Roadside reward amounts cannot be negative.");
            }
            rewardClass = Objects.requireNonNull(rewardClass, "rewardClass");
            if (rewardClass.isEmpty() && classXp != 0L) {
                throw new IllegalArgumentException(
                        "Class XP requires the class that owned the eligible contribution."
                );
            }
        }
    }
}
