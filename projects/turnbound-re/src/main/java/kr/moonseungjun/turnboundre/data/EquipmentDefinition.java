package kr.moonseungjun.turnboundre.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/** One minimal character equipment piece. Costs, visual source and stat choices are data-driven; rules stay in code. */
public record EquipmentDefinition(
        String id,
        String ingredientItem,
        String visualItem,
        List<Tier> tiers
) {
    public record Bonus(int hpPercent, int atkPercent, int defPercent, int poisePercent) {
        public static final Codec<Bonus> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("hpPercent", 0).forGetter(Bonus::hpPercent),
                Codec.INT.optionalFieldOf("atkPercent", 0).forGetter(Bonus::atkPercent),
                Codec.INT.optionalFieldOf("defPercent", 0).forGetter(Bonus::defPercent),
                Codec.INT.optionalFieldOf("poisePercent", 0).forGetter(Bonus::poisePercent)
        ).apply(instance, Bonus::new));
    }

    /** Tier 1 is the forge cost; later tiers are upgrade costs. */
    public record Tier(int level, long coinCost, int materialCount, Bonus bonus) {
        public static final Codec<Tier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("level").forGetter(Tier::level),
                Codec.LONG.fieldOf("coinCost").forGetter(Tier::coinCost),
                Codec.INT.fieldOf("materialCount").forGetter(Tier::materialCount),
                Bonus.CODEC.fieldOf("bonus").forGetter(Tier::bonus)
        ).apply(instance, Tier::new));
    }

    public static final Codec<EquipmentDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(EquipmentDefinition::id),
            Codec.STRING.fieldOf("ingredientItem").forGetter(EquipmentDefinition::ingredientItem),
            Codec.STRING.optionalFieldOf("visualItem", "").forGetter(EquipmentDefinition::visualItem),
            Tier.CODEC.listOf().fieldOf("tiers").forGetter(EquipmentDefinition::tiers)
    ).apply(instance, EquipmentDefinition::new));

    public EquipmentDefinition {
        visualItem = visualItem == null || visualItem.isBlank() ? ingredientItem : visualItem;
        tiers = tiers == null ? List.of() : List.copyOf(tiers);
    }

    /** Legacy/source-fixture compatibility: old definitions without visualItem use the material item as a safe runtime fallback. */
    public EquipmentDefinition(String id, String ingredientItem, List<Tier> tiers) {
        this(id, ingredientItem, ingredientItem, tiers);
    }

    public Tier tier(int level) {
        return tiers.stream().filter(tier -> tier.level() == level).findFirst().orElse(null);
    }

    public int maxLevel() {
        return tiers.isEmpty() ? 0 : tiers.getLast().level();
    }
}
