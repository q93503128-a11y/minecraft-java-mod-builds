package dev.moonseungjun.openworldrpg.progression.r01;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.moonseungjun.openworldrpg.camp.R01CampRules;
import dev.moonseungjun.openworldrpg.combat.state.ProjectEquipmentSlot;
import dev.moonseungjun.openworldrpg.gathering.R01GatheringRules;
import dev.moonseungjun.openworldrpg.integration.actor.R01ExternalActorCatalog;
import dev.moonseungjun.openworldrpg.inventory.ProjectItemGrade;
import dev.moonseungjun.openworldrpg.profession.R01CraftingRecipe;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Canon-locked personal reward rolls for R01 ordinary enemies and Steelboar. */
public final class R01OrdinaryEnemyRewardRules {
    public static final List<String> STEELBOAR_EQUIPMENT_POOL = List.of(
            "openworld_rpg:quarry_maul",
            "openworld_rpg:river_pike",
            "openworld_rpg:watch_buckler",
            "openworld_rpg:ironbound_guard"
    );
    public static final List<ProjectEquipmentSlot> ARMOR_SLOTS = List.of(
            ProjectEquipmentSlot.HEAD,
            ProjectEquipmentSlot.CHEST,
            ProjectEquipmentSlot.LEGS,
            ProjectEquipmentSlot.GLOVES,
            ProjectEquipmentSlot.BOOTS
    );
    public static final int STEELBOAR_EQUIPMENT_DROP_PERCENT = 30;

    private R01OrdinaryEnemyRewardRules() {}

    public static Optional<EnemySource> sourceForEntityId(String entityId) {
        if (entityId == null) return Optional.empty();
        if (R01ExternalActorCatalog.CAVE_CENTIPEDE_BODY.equals(entityId)
                || R01ExternalActorCatalog.CAVE_CENTIPEDE_TAIL.equals(entityId)) {
            return Optional.of(EnemySource.CAVE_CENTIPEDE);
        }
        for (EnemySource source : EnemySource.values()) {
            if (source.entityId().equals(entityId)) return Optional.of(source);
        }
        return Optional.empty();
    }

    public static RewardPlan createPlan(EnemySource source, Rolls rolls) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(rolls, "rolls");
        long gold = switch (source) {
            case LOUXIA, BISON, GRIZZLY -> 0L;
            case CAVE_CENTIPEDE -> rolls.goldRollPercent() < 70 ? 3L : 0L;
            case STEELBOAR -> 18L;
        };

        Map<String, Integer> materials = new HashMap<>();
        switch (source) {
            case LOUXIA -> {
                materials.put(R01CraftingRecipe.LOUXIA_MEAT, 1 + Math.floorMod(rolls.quantityRoll(), 2));
                if (rolls.secondaryMaterialRollPercent() < 35) {
                    materials.put(R01CraftingRecipe.LOUXIA_GLOW, 1);
                }
            }
            case CAVE_CENTIPEDE -> {
                // Donor Cave Centipede Leg remains excluded from project-normalized loot.
            }
            case BISON -> {
                if (rolls.primaryMaterialRollPercent() < 70) {
                    materials.put(R01CampRules.TOUGH_HIDE, 1 + Math.floorMod(rolls.quantityRoll(), 2));
                }
            }
            case GRIZZLY -> materials.put(R01CampRules.TOUGH_HIDE, 2 + Math.floorMod(rolls.quantityRoll(), 2));
            case STEELBOAR -> {
                if (rolls.primaryMaterialRollPercent() < 60) {
                    materials.put(R01GatheringRules.IRON_ORE, 1 + Math.floorMod(rolls.quantityRoll(), 3));
                }
                if (rolls.secondaryMaterialRollPercent() < 35) {
                    materials.put(R01CampRules.TOUGH_HIDE, 1);
                }
            }
        }

        Optional<EquipmentPlan> equipment = Optional.empty();
        if (source == EnemySource.STEELBOAR
                && rolls.equipmentRollPercent() < STEELBOAR_EQUIPMENT_DROP_PERCENT) {
            String baseId = STEELBOAR_EQUIPMENT_POOL.get(rolls.equipmentBasePoolIndex());
            Optional<ProjectEquipmentSlot> armorSlot =
                    "openworld_rpg:ironbound_guard".equals(baseId)
                            ? Optional.of(ARMOR_SLOTS.get(rolls.armorSlotIndex()))
                            : Optional.empty();
            equipment = Optional.of(new EquipmentPlan(
                    baseId,
                    eliteGrade(rolls.gradeRollPercent()),
                    itemLevel(source.contentLevel(), rolls.itemLevelRollPercent()),
                    armorSlot,
                    rolls.affixSeed()
            ));
        }
        return new RewardPlan(gold, Map.copyOf(materials), equipment);
    }

    public static ProjectItemGrade eliteGrade(int rollPercent) {
        requirePercent("rollPercent", rollPercent);
        if (rollPercent < 50) return ProjectItemGrade.STANDARD;
        if (rollPercent < 85) return ProjectItemGrade.REFINED;
        if (rollPercent < 98) return ProjectItemGrade.SUPERIOR;
        return ProjectItemGrade.EXALTED;
    }

    public static int itemLevel(int sourceLevel, int rollPercent) {
        if (sourceLevel < 1 || sourceLevel > 80) {
            throw new IllegalArgumentException("sourceLevel must be inside 1..80.");
        }
        requirePercent("rollPercent", rollPercent);
        int delta;
        if (rollPercent < 10) delta = -2;
        else if (rollPercent < 30) delta = -1;
        else if (rollPercent < 70) delta = 0;
        else if (rollPercent < 90) delta = 1;
        else delta = 2;
        return Math.max(1, Math.min(80, sourceLevel + delta));
    }

    private static void requirePercent(String name, int value) {
        if (value < 0 || value >= 100) throw new IllegalArgumentException(name + " must be inside 0..99.");
    }

    public enum EnemySource {
        LOUXIA(R01ExternalActorCatalog.LOUXIA, 1, 0.01, 0.008),
        CAVE_CENTIPEDE(R01ExternalActorCatalog.CAVE_CENTIPEDE_HEAD, 4, 0.01, 0.008),
        BISON(R01ExternalActorCatalog.BISON, 3, 0.01, 0.008),
        GRIZZLY(R01ExternalActorCatalog.GRIZZLY, 6, 0.01, 0.008),
        STEELBOAR(R01ExternalActorCatalog.STEELBOAR, 6, 0.06, 0.05);

        public static final Codec<EnemySource> CODEC = Codec.STRING.comapFlatMap(
                value -> {
                    try {
                        return DataResult.success(EnemySource.valueOf(value.trim().toUpperCase(Locale.ROOT)));
                    } catch (IllegalArgumentException exception) {
                        return DataResult.error(() -> "Unknown R01 ordinary reward source: " + value);
                    }
                },
                value -> value.name().toLowerCase(Locale.ROOT)
        );

        private final String entityId;
        private final int contentLevel;
        private final double combatXpFraction;
        private final double classXpFraction;

        EnemySource(String entityId, int contentLevel, double combatXpFraction, double classXpFraction) {
            this.entityId = entityId;
            this.contentLevel = contentLevel;
            this.combatXpFraction = combatXpFraction;
            this.classXpFraction = classXpFraction;
        }

        public String entityId() { return entityId; }
        public int contentLevel() { return contentLevel; }
        public double combatXpFraction() { return combatXpFraction; }
        public double classXpFraction() { return classXpFraction; }
    }

    public record Rolls(
            int goldRollPercent,
            int primaryMaterialRollPercent,
            int secondaryMaterialRollPercent,
            int quantityRoll,
            int equipmentRollPercent,
            int equipmentBasePoolIndex,
            int gradeRollPercent,
            int itemLevelRollPercent,
            int armorSlotIndex,
            long affixSeed
    ) {
        public Rolls {
            requirePercent("goldRollPercent", goldRollPercent);
            requirePercent("primaryMaterialRollPercent", primaryMaterialRollPercent);
            requirePercent("secondaryMaterialRollPercent", secondaryMaterialRollPercent);
            if (quantityRoll < 0 || quantityRoll >= 6) throw new IllegalArgumentException("quantityRoll must be inside 0..5.");
            requirePercent("equipmentRollPercent", equipmentRollPercent);
            if (equipmentBasePoolIndex < 0 || equipmentBasePoolIndex >= STEELBOAR_EQUIPMENT_POOL.size()) {
                throw new IllegalArgumentException("Invalid Steelboar equipment-pool index.");
            }
            requirePercent("gradeRollPercent", gradeRollPercent);
            requirePercent("itemLevelRollPercent", itemLevelRollPercent);
            if (armorSlotIndex < 0 || armorSlotIndex >= ARMOR_SLOTS.size()) {
                throw new IllegalArgumentException("Invalid armor-slot index.");
            }
        }
    }

    public record EquipmentPlan(
            String baseId,
            ProjectItemGrade grade,
            int itemLevel,
            Optional<ProjectEquipmentSlot> armorSlot,
            long affixSeed
    ) {
        public static final Codec<EquipmentPlan> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("base_id").forGetter(EquipmentPlan::baseId),
                ProjectItemGrade.CODEC.fieldOf("grade").forGetter(EquipmentPlan::grade),
                Codec.intRange(1, 80).fieldOf("item_level").forGetter(EquipmentPlan::itemLevel),
                ProjectEquipmentSlot.CODEC.optionalFieldOf("armor_slot").forGetter(EquipmentPlan::armorSlot),
                Codec.LONG.fieldOf("affix_seed").forGetter(EquipmentPlan::affixSeed)
        ).apply(instance, EquipmentPlan::new));

        public EquipmentPlan {
            Objects.requireNonNull(baseId, "baseId");
            Objects.requireNonNull(grade, "grade");
            armorSlot = Objects.requireNonNull(armorSlot, "armorSlot");
            if (!STEELBOAR_EQUIPMENT_POOL.contains(baseId)) {
                throw new IllegalArgumentException("Steelboar equipment base is outside its canonical R01 pool.");
            }
            boolean armorFamily = "openworld_rpg:ironbound_guard".equals(baseId);
            if (armorFamily != armorSlot.isPresent()) {
                throw new IllegalArgumentException("Only Ironbound Guard requires a resolved armor slot.");
            }
        }
    }

    public record RewardPlan(long gold, Map<String, Integer> materials, Optional<EquipmentPlan> equipment) {
        private static final Codec<Map<String, Integer>> MATERIALS_CODEC =
                Codec.unboundedMap(Codec.STRING, Codec.intRange(1, 64));

        public static final Codec<RewardPlan> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("gold").forGetter(RewardPlan::gold),
                MATERIALS_CODEC.fieldOf("materials").forGetter(RewardPlan::materials),
                EquipmentPlan.CODEC.optionalFieldOf("equipment").forGetter(RewardPlan::equipment)
        ).apply(instance, RewardPlan::new));

        public RewardPlan {
            if (gold < 0L) throw new IllegalArgumentException("Gold cannot be negative.");
            materials = Map.copyOf(Objects.requireNonNull(materials, "materials"));
            equipment = Objects.requireNonNull(equipment, "equipment");
            materials.forEach((id, amount) -> {
                if (id == null || id.isBlank() || id.indexOf(':') <= 0 || amount <= 0) {
                    throw new IllegalArgumentException("Invalid material reward entry.");
                }
            });
        }
    }
}