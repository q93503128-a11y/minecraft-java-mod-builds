package dev.moonseungjun.openworldrpg.progression.r01;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules;
import dev.moonseungjun.openworldrpg.combat.state.ProjectArmorArchetype;
import dev.moonseungjun.openworldrpg.combat.state.ProjectEquipmentSlot;
import dev.moonseungjun.openworldrpg.inventory.ProjectItemGrade;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Canon-locked personal first/repeat loot plan for one eligible R01 Regalhart participant. */
public final class R01RegalhartRewardRules {
    public static final int CONTENT_LEVEL = 8;
    public static final double FIRST_COMBAT_XP_FRACTION = 0.20;
    public static final double FIRST_CLASS_XP_FRACTION = 0.15;
    public static final long FIRST_GOLD = 70L;
    public static final int FIRST_ANTLER_QUANTITY = 2;

    public static final double REPEAT_COMBAT_XP_FRACTION = 0.07;
    public static final double REPEAT_CLASS_XP_FRACTION = 0.06;
    public static final long REPEAT_GOLD = 35L;
    public static final int REPEAT_ANTLER_QUANTITY = 1;
    public static final int REPEAT_SECOND_EQUIPMENT_PERCENT = 25;

    public static final int SIGNATURE_DROP_PERCENT = 15;
    public static final String REGALHART_ANTLER_ID =
            "openworld_rpg:regalhart_antler";
    public static final String HARTCROWN_SPEAR_ID =
            "openworld_rpg:hartcrown_spear";
    public static final String HARTS_MOMENTUM_POWER_ID =
            "openworld_rpg:mythic/harts_momentum";

    public static final List<NormalBase> NORMAL_BASE_POOL = List.of(
            NormalBase.RIVER_PIKE,
            NormalBase.RIVERWOOD_BOW,
            NormalBase.WAYFARER_DAGGERS,
            NormalBase.WAYFARER_LEATHERS,
            NormalBase.GREENWATER_PENDANT,
            NormalBase.WAYFARERS_TOKEN
    );

    public static final List<ProjectEquipmentSlot> ARMOR_SLOTS = List.of(
            ProjectEquipmentSlot.HEAD,
            ProjectEquipmentSlot.CHEST,
            ProjectEquipmentSlot.LEGS,
            ProjectEquipmentSlot.GLOVES,
            ProjectEquipmentSlot.BOOTS
    );

    private R01RegalhartRewardRules() {
    }

    public static RewardPlan createPlan(
            boolean firstEligibleDefeat,
            int firstBasePoolIndex,
            int firstGradeRoll,
            int firstItemLevelRollPercent,
            int firstArmorSlotIndex,
            long firstAffixSeed,
            int secondEquipmentRollPercent,
            int secondBasePoolIndex,
            int secondGradeRollPercent,
            int secondItemLevelRollPercent,
            int secondArmorSlotIndex,
            long secondAffixSeed,
            int signatureRollPercent
    ) {
        requireIndex("firstBasePoolIndex", firstBasePoolIndex, NORMAL_BASE_POOL.size());
        requirePercent("firstItemLevelRollPercent", firstItemLevelRollPercent);
        requireIndex("firstArmorSlotIndex", firstArmorSlotIndex, ARMOR_SLOTS.size());
        requirePercent("secondEquipmentRollPercent", secondEquipmentRollPercent);
        requireIndex("secondBasePoolIndex", secondBasePoolIndex, NORMAL_BASE_POOL.size());
        requirePercent("secondGradeRollPercent", secondGradeRollPercent);
        requirePercent("secondItemLevelRollPercent", secondItemLevelRollPercent);
        requireIndex("secondArmorSlotIndex", secondArmorSlotIndex, ARMOR_SLOTS.size());
        requirePercent("signatureRollPercent", signatureRollPercent);

        ProjectItemGrade firstGrade;
        if (firstEligibleDefeat) {
            if (firstGradeRoll < 0 || firstGradeRoll >= 55) {
                throw new IllegalArgumentException(
                        "First Regalhart Superior+ grade roll must be inside 0..54."
                );
            }
            firstGrade = firstGradeRoll < 40
                    ? ProjectItemGrade.SUPERIOR
                    : ProjectItemGrade.EXALTED;
        } else {
            requirePercent("firstGradeRoll", firstGradeRoll);
            firstGrade = fieldBossGrade(firstGradeRoll);
        }

        EquipmentPlan guaranteed = equipmentPlan(
                firstBasePoolIndex,
                firstGrade,
                firstItemLevelRollPercent,
                firstArmorSlotIndex,
                firstAffixSeed
        );

        Optional<EquipmentPlan> second = Optional.empty();
        if (!firstEligibleDefeat
                && secondEquipmentRollPercent < REPEAT_SECOND_EQUIPMENT_PERCENT) {
            second = Optional.of(equipmentPlan(
                    secondBasePoolIndex,
                    fieldBossGrade(secondGradeRollPercent),
                    secondItemLevelRollPercent,
                    secondArmorSlotIndex,
                    secondAffixSeed
            ));
        }

        return new RewardPlan(
                firstEligibleDefeat,
                guaranteed,
                second,
                firstEligibleDefeat
                        ? FIRST_ANTLER_QUANTITY
                        : REPEAT_ANTLER_QUANTITY,
                signatureRollPercent,
                signatureRollPercent < SIGNATURE_DROP_PERCENT
        );
    }

    public static ProjectItemGrade fieldBossGrade(int rollPercent) {
        requirePercent("gradeRollPercent", rollPercent);
        if (rollPercent < 10) {
            return ProjectItemGrade.STANDARD;
        }
        if (rollPercent < 45) {
            return ProjectItemGrade.REFINED;
        }
        if (rollPercent < 85) {
            return ProjectItemGrade.SUPERIOR;
        }
        return ProjectItemGrade.EXALTED;
    }

    public static int itemLevel(int rollPercent) {
        requirePercent("itemLevelRollPercent", rollPercent);
        int delta;
        if (rollPercent < 10) {
            delta = -2;
        } else if (rollPercent < 30) {
            delta = -1;
        } else if (rollPercent < 70) {
            delta = 0;
        } else if (rollPercent < 90) {
            delta = 1;
        } else {
            delta = 2;
        }
        return Math.max(
                ProjectCombatRules.MIN_CONTENT_LEVEL,
                Math.min(
                        ProjectCombatRules.MAX_CONTENT_LEVEL,
                        CONTENT_LEVEL + delta
                )
        );
    }

    private static EquipmentPlan equipmentPlan(
            int basePoolIndex,
            ProjectItemGrade grade,
            int itemLevelRollPercent,
            int armorSlotIndex,
            long affixSeed
    ) {
        NormalBase base = NORMAL_BASE_POOL.get(basePoolIndex);
        Optional<ProjectEquipmentSlot> armorSlot =
                base == NormalBase.WAYFARER_LEATHERS
                        ? Optional.of(ARMOR_SLOTS.get(armorSlotIndex))
                        : Optional.empty();
        return new EquipmentPlan(
                base,
                grade,
                itemLevel(itemLevelRollPercent),
                armorSlot,
                affixSeed
        );
    }

    private static void requirePercent(String name, int value) {
        if (value < 0 || value >= 100) {
            throw new IllegalArgumentException(name + " must be inside 0..99.");
        }
    }

    private static void requireIndex(String name, int value, int size) {
        if (value < 0 || value >= size) {
            throw new IllegalArgumentException(
                    name + " must be inside 0.." + (size - 1) + "."
            );
        }
    }

    public enum NormalBase {
        RIVER_PIKE("openworld_rpg:river_pike"),
        RIVERWOOD_BOW("openworld_rpg:riverwood_bow"),
        WAYFARER_DAGGERS("openworld_rpg:wayfarer_daggers"),
        WAYFARER_LEATHERS("openworld_rpg:wayfarer_leathers"),
        GREENWATER_PENDANT("openworld_rpg:greenwater_pendant"),
        WAYFARERS_TOKEN("openworld_rpg:wayfarers_token");

        public static final Codec<NormalBase> CODEC = Codec.STRING.comapFlatMap(
                value -> {
                    try {
                        return DataResult.success(
                                NormalBase.valueOf(
                                        value.trim().toUpperCase(Locale.ROOT)
                                )
                        );
                    } catch (IllegalArgumentException exception) {
                        return DataResult.error(
                                () -> "Unknown Regalhart normal base: " + value
                        );
                    }
                },
                value -> value.name().toLowerCase(Locale.ROOT)
        );

        private final String baseId;

        NormalBase(String baseId) {
            this.baseId = baseId;
        }

        public String baseId() {
            return baseId;
        }
    }

    public record EquipmentPlan(
            NormalBase base,
            ProjectItemGrade grade,
            int itemLevel,
            Optional<ProjectEquipmentSlot> armorSlot,
            long affixSeed
    ) {
        public static final Codec<EquipmentPlan> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        NormalBase.CODEC.fieldOf("base")
                                .forGetter(EquipmentPlan::base),
                        ProjectItemGrade.CODEC.fieldOf("grade")
                                .forGetter(EquipmentPlan::grade),
                        Codec.intRange(
                                ProjectCombatRules.MIN_CONTENT_LEVEL,
                                ProjectCombatRules.MAX_CONTENT_LEVEL
                        ).fieldOf("item_level")
                                .forGetter(EquipmentPlan::itemLevel),
                        ProjectEquipmentSlot.CODEC.optionalFieldOf("armor_slot")
                                .forGetter(EquipmentPlan::armorSlot),
                        Codec.LONG.fieldOf("affix_seed")
                                .forGetter(EquipmentPlan::affixSeed)
                ).apply(instance, EquipmentPlan::new));

        public EquipmentPlan {
            Objects.requireNonNull(base, "base");
            Objects.requireNonNull(grade, "grade");
            armorSlot = Objects.requireNonNull(armorSlot, "armorSlot");
            boolean armorFamily = base == NormalBase.WAYFARER_LEATHERS;
            if (armorFamily != armorSlot.isPresent()) {
                throw new IllegalArgumentException(
                        "Only Wayfarer Leathers requires an armor-slot roll."
                );
            }
            armorSlot.ifPresent(slot -> {
                if (!ProjectArmorArchetype.isArmorSlot(slot)) {
                    throw new IllegalArgumentException(
                            "Regalhart armor roll requires a real armor slot."
                    );
                }
            });
        }
    }

    public record RewardPlan(
            boolean firstEligibleDefeat,
            EquipmentPlan guaranteedEquipment,
            Optional<EquipmentPlan> secondEquipment,
            int antlerQuantity,
            int signatureRollPercent,
            boolean hartcrownSpearDrop
    ) {
        public static final Codec<RewardPlan> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.BOOL.fieldOf("first_eligible_defeat")
                                .forGetter(RewardPlan::firstEligibleDefeat),
                        EquipmentPlan.CODEC.fieldOf("guaranteed_equipment")
                                .forGetter(RewardPlan::guaranteedEquipment),
                        EquipmentPlan.CODEC.optionalFieldOf("second_equipment")
                                .forGetter(RewardPlan::secondEquipment),
                        Codec.intRange(1, 2).fieldOf("antler_quantity")
                                .forGetter(RewardPlan::antlerQuantity),
                        Codec.intRange(0, 99).fieldOf("signature_roll_percent")
                                .forGetter(RewardPlan::signatureRollPercent),
                        Codec.BOOL.fieldOf("hartcrown_spear_drop")
                                .forGetter(RewardPlan::hartcrownSpearDrop)
                ).apply(instance, RewardPlan::new));

        public RewardPlan {
            Objects.requireNonNull(guaranteedEquipment, "guaranteedEquipment");
            secondEquipment = Objects.requireNonNull(
                    secondEquipment,
                    "secondEquipment"
            );
            if (firstEligibleDefeat) {
                if (antlerQuantity != FIRST_ANTLER_QUANTITY
                        || secondEquipment.isPresent()
                        || (guaranteedEquipment.grade() != ProjectItemGrade.SUPERIOR
                                && guaranteedEquipment.grade() != ProjectItemGrade.EXALTED)) {
                    throw new IllegalArgumentException(
                            "First Regalhart reward plan does not match field-boss first-clear canon."
                    );
                }
            } else if (antlerQuantity != REPEAT_ANTLER_QUANTITY) {
                throw new IllegalArgumentException(
                        "Repeat Regalhart reward must grant exactly one Antler."
                );
            }
            if ((signatureRollPercent < SIGNATURE_DROP_PERCENT)
                    != hartcrownSpearDrop) {
                throw new IllegalArgumentException(
                        "Regalhart signature roll/result mismatch."
                );
            }
        }

        public double combatXpFraction() {
            return firstEligibleDefeat
                    ? FIRST_COMBAT_XP_FRACTION
                    : REPEAT_COMBAT_XP_FRACTION;
        }

        public double classXpFraction() {
            return firstEligibleDefeat
                    ? FIRST_CLASS_XP_FRACTION
                    : REPEAT_CLASS_XP_FRACTION;
        }

        public long gold() {
            return firstEligibleDefeat ? FIRST_GOLD : REPEAT_GOLD;
        }
    }
}
