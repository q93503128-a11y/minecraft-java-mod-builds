package dev.moonseungjun.openworldrpg.progression.r01;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.moonseungjun.openworldrpg.inventory.ProjectItemGrade;
import java.util.Objects;
import java.util.Optional;

/**
 * Persistent first-clear Earthloong boss-loot RNG plan.
 *
 * <p>The plan intentionally stores canonical source identity before final item materialization.
 * Current canon fixes the six equal-weight normal base families and the 15% equal-weight Mythic
 * pool, but does not yet close a Superior-vs-Exalted floor distribution or the Ironbound Guard
 * armor slot. Those missing rules are not invented here.</p>
 */
public record R01EarthloongBossLootPlanState(
        int schemaVersion,
        Optional<FirstClearPlan> firstClearPlan
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static final Codec<R01EarthloongBossLootPlanState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("earthloong_loot_plan_schema_version")
                            .forGetter(R01EarthloongBossLootPlanState::schemaVersion),
                    FirstClearPlan.CODEC.optionalFieldOf("first_clear_plan")
                            .forGetter(R01EarthloongBossLootPlanState::firstClearPlan)
            ).apply(instance, R01EarthloongBossLootPlanState::new));

    public R01EarthloongBossLootPlanState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported Earthloong loot-plan schema version: "
                            + schemaVersion
            );
        }
        firstClearPlan = Objects.requireNonNull(
                firstClearPlan,
                "firstClearPlan"
        );
    }

    public static R01EarthloongBossLootPlanState initial() {
        return new R01EarthloongBossLootPlanState(
                CURRENT_SCHEMA_VERSION,
                Optional.empty()
        );
    }

    public R01EarthloongBossLootPlanState withFirstClearPlan(
            FirstClearPlan plan
    ) {
        Objects.requireNonNull(plan, "plan");
        if (firstClearPlan.isPresent()) {
            if (!firstClearPlan.orElseThrow().equals(plan)) {
                throw new IllegalStateException(
                        "Earthloong first-clear loot plan cannot be rerolled."
                );
            }
            return this;
        }
        return new R01EarthloongBossLootPlanState(
                schemaVersion,
                Optional.of(plan)
        );
    }

    public record FirstClearPlan(
            R01EarthloongBossLootRules.NormalBase normalBase,
            ProjectItemGrade minimumNormalGrade,
            int signatureRollPercent,
            Optional<R01EarthloongBossLootRules.MythicBase> mythicDrop
    ) {
        public static final Codec<FirstClearPlan> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        R01EarthloongBossLootRules.NormalBase.CODEC
                                .fieldOf("normal_base")
                                .forGetter(FirstClearPlan::normalBase),
                        ProjectItemGrade.CODEC
                                .fieldOf("minimum_normal_grade")
                                .forGetter(FirstClearPlan::minimumNormalGrade),
                        Codec.intRange(0, 99)
                                .fieldOf("signature_roll_percent")
                                .forGetter(FirstClearPlan::signatureRollPercent),
                        R01EarthloongBossLootRules.MythicBase.CODEC
                                .optionalFieldOf("mythic_drop")
                                .forGetter(FirstClearPlan::mythicDrop)
                ).apply(instance, FirstClearPlan::new));

        public FirstClearPlan {
            Objects.requireNonNull(normalBase, "normalBase");
            Objects.requireNonNull(minimumNormalGrade, "minimumNormalGrade");
            mythicDrop = Objects.requireNonNull(mythicDrop, "mythicDrop");

            if (minimumNormalGrade != ProjectItemGrade.SUPERIOR) {
                throw new IllegalArgumentException(
                        "R01 first-clear boss gear currently owns a Superior minimum floor only."
                );
            }
            boolean signatureHit =
                    signatureRollPercent
                            < R01EarthloongBossLootRules.SIGNATURE_DROP_PERCENT;
            if (signatureHit != mythicDrop.isPresent()) {
                throw new IllegalArgumentException(
                        "Persisted Earthloong signature roll/result mismatch."
                );
            }
        }
    }
}
