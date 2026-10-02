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

/** Canon-locked personal loot plan for one eligible R01 Nature Spirit participant. */
public final class R01NatureSpiritRewardRules {
    public static final int CONTENT_LEVEL = 7;
    public static final int EQUIPMENT_DROP_PERCENT = 30;
    public static final int HEALING_HERB_DROP_PERCENT = 60;
    public static final int VERDANT_CRYSTAL_DROP_PERCENT = 35;

    public static final String HEALING_HERB_ID =
            "openworld_rpg:healing_herb";
    public static final String VERDANT_CRYSTAL_ID =
            "openworld_rpg:verdant_crystal";

    public static final List<NormalBase> EQUIPMENT_POOL = List.of(
            NormalBase.INITIATE_STAFF,
            NormalBase.INITIATE_WAND,
            NormalBase.APPRENTICE_FOCUS,
            NormalBase.RIVER_SCHOLAR_GARB,
            NormalBase.GREENWATER_PENDANT
    );

    public static final List<ProjectEquipmentSlot> ARMOR_SLOTS = List.of(
            ProjectEquipmentSlot.HEAD,
            ProjectEquipmentSlot.CHEST,
            ProjectEquipmentSlot.LEGS,
            ProjectEquipmentSlot.GLOVES,
            ProjectEquipmentSlot.BOOTS
    );

    private R01NatureSpiritRewardRules() {
    }

    public static RewardPlan createPlan(
            int equipmentRollPercent,
            int basePoolIndex,
            int gradeRollPercent,
            int itemLevelRollPercent,
            int armorSlotIndex,
            long affixSeed,
            int healingHerbRollPercent,
            int healingHerbQuantityRoll,
            int verdantCrystalRollPercent
    ) {
        requirePercent("equipmentRollPercent", equipmentRollPercent);
        requireIndex("basePoolIndex", basePoolIndex, EQUIPMENT_POOL.size());
        requirePercent("gradeRollPercent", gradeRollPercent);
        requirePercent("itemLevelRollPercent", itemLevelRollPercent);
        requireIndex("armorSlotIndex", armorSlotIndex, ARMOR_SLOTS.size());
        requirePercent("healingHerbRollPercent", healingHerbRollPercent);
        if (healingHerbQuantityRoll < 0 || healingHerbQuantityRoll >= 2) {
            throw new IllegalArgumentException(
                    "healingHerbQuantityRoll must be 0 or 1."
            );
        }
        requirePercent("verdantCrystalRollPercent", verdantCrystalRollPercent);

        Optional<EquipmentPlan> equipment = Optional.empty();
        if (equipmentRollPercent < EQUIPMENT_DROP_PERCENT) {
            NormalBase base = EQUIPMENT_POOL.get(basePoolIndex);
            Optional<ProjectEquipmentSlot> armorSlot =
                    base == NormalBase.RIVER_SCHOLAR_GARB
                            ? Optional.of(ARMOR_SLOTS.get(armorSlotIndex))
                            : Optional.empty();
            equipment = Optional.of(new EquipmentPlan(
                    base,
                    grade(gradeRollPercent),
                    itemLevel(itemLevelRollPercent),
                    armorSlot,
                    affixSeed
            ));
        }

        int herbQuantity =
                healingHerbRollPercent < HEALING_HERB_DROP_PERCENT
                        ? 1 + healingHerbQuantityRoll
                        : 0;
        int crystalQuantity =
                verdantCrystalRollPercent < VERDANT_CRYSTAL_DROP_PERCENT
                        ? 1
                        : 0;
        return new RewardPlan(
                equipment,
                herbQuantity,
                crystalQuantity
        );
    }

    public static ProjectItemGrade grade(int rollPercent) {
        requirePercent("gradeRollPercent", rollPercent);
        if (rollPercent < 50) {
            return ProjectItemGrade.STANDARD;
        }
        if (rollPercent < 85) {
            return ProjectItemGrade.REFINED;
        }
        if (rollPercent < 98) {
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
        INITIATE_STAFF("openworld_rpg:initiate_staff"),
        INITIATE_WAND("openworld_rpg:initiate_wand"),
        APPRENTICE_FOCUS("openworld_rpg:apprentice_focus"),
        RIVER_SCHOLAR_GARB("openworld_rpg:river_scholar_garb"),
        GREENWATER_PENDANT("openworld_rpg:greenwater_pendant");

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
                                () -> "Unknown Nature Spirit normal base: " + value
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
                        ProjectEquipmentSlot.CODEC
                                .optionalFieldOf("armor_slot")
                                .forGetter(EquipmentPlan::armorSlot),
                        Codec.LONG.fieldOf("affix_seed")
                                .forGetter(EquipmentPlan::affixSeed)
                ).apply(instance, EquipmentPlan::new));

        public EquipmentPlan {
            Objects.requireNonNull(base, "base");
            Objects.requireNonNull(grade, "grade");
            armorSlot = Objects.requireNonNull(armorSlot, "armorSlot");
            if (itemLevel < ProjectCombatRules.MIN_CONTENT_LEVEL
                    || itemLevel > ProjectCombatRules.MAX_CONTENT_LEVEL) {
                throw new IllegalArgumentException(
                        "Nature Spirit item level outside project bounds."
                );
            }
            boolean armorFamily = base == NormalBase.RIVER_SCHOLAR_GARB;
            if (armorFamily != armorSlot.isPresent()) {
                throw new IllegalArgumentException(
                        "Only River Scholar Garb requires an armor-slot roll."
                );
            }
            armorSlot.ifPresent(slot -> {
                if (!ProjectArmorArchetype.isArmorSlot(slot)) {
                    throw new IllegalArgumentException(
                            "Nature Spirit armor roll requires a real armor slot."
                    );
                }
            });
        }
    }

    public record RewardPlan(
            Optional<EquipmentPlan> equipment,
            int healingHerbQuantity,
            int verdantCrystalQuantity
    ) {
        public static final Codec<RewardPlan> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        EquipmentPlan.CODEC.optionalFieldOf("equipment")
                                .forGetter(RewardPlan::equipment),
                        Codec.intRange(0, 2).fieldOf("healing_herb_quantity")
                                .forGetter(RewardPlan::healingHerbQuantity),
                        Codec.intRange(0, 1).fieldOf("verdant_crystal_quantity")
                                .forGetter(RewardPlan::verdantCrystalQuantity)
                ).apply(instance, RewardPlan::new));

        public RewardPlan {
            equipment = Objects.requireNonNull(equipment, "equipment");
            if (healingHerbQuantity < 0 || healingHerbQuantity > 2) {
                throw new IllegalArgumentException(
                        "Nature Spirit Healing Herb quantity must be 0..2."
                );
            }
            if (verdantCrystalQuantity < 0 || verdantCrystalQuantity > 1) {
                throw new IllegalArgumentException(
                        "Nature Spirit Verdant Crystal quantity must be 0..1."
                );
            }
        }
    }
}
