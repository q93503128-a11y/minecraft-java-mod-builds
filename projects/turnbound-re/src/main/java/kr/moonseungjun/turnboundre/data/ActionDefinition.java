package kr.moonseungjun.turnboundre.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * Canonical data-driven battle action contract.
 * JSON follows docs/13_DATA_SCHEMA.md: positive energyDelta generates Energy, negative consumes it.
 */
public record ActionDefinition(
        String id,
        String kind,
        int energyDelta,
        int hpPower,
        int poisePower,
        String damageTag,
        Targeting targeting,
        int priority,
        List<Effect> effects
) {
    public record Targeting(String team, String shape, int count) {
        public static final Codec<Targeting> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("team").forGetter(Targeting::team),
                Codec.STRING.fieldOf("shape").forGetter(Targeting::shape),
                Codec.INT.fieldOf("count").forGetter(Targeting::count)
        ).apply(instance, Targeting::new));
    }

    public record Effect(String type, String status, double value, int duration, double chance) {
        public static final Codec<Effect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("type").forGetter(Effect::type),
                Codec.STRING.optionalFieldOf("status", "").forGetter(Effect::status),
                Codec.DOUBLE.optionalFieldOf("value", 0.0D).forGetter(Effect::value),
                Codec.INT.optionalFieldOf("duration", 0).forGetter(Effect::duration),
                Codec.DOUBLE.optionalFieldOf("chance", 1.0D).forGetter(Effect::chance)
        ).apply(instance, Effect::new));
    }

    public static final Codec<ActionDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(ActionDefinition::id),
            Codec.STRING.fieldOf("kind").forGetter(ActionDefinition::kind),
            Codec.INT.fieldOf("energyDelta").forGetter(ActionDefinition::energyDelta),
            Codec.INT.fieldOf("hpPower").forGetter(ActionDefinition::hpPower),
            Codec.INT.fieldOf("poisePower").forGetter(ActionDefinition::poisePower),
            Codec.STRING.fieldOf("damageTag").forGetter(ActionDefinition::damageTag),
            Targeting.CODEC.fieldOf("targeting").forGetter(ActionDefinition::targeting),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(ActionDefinition::priority),
            Effect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(ActionDefinition::effects)
    ).apply(instance, ActionDefinition::new));

    public ActionDefinition {
        effects = effects == null ? List.of() : List.copyOf(effects);
    }

    /** Convenience constructor for the two universal core actions and compact deterministic tests. */
    public ActionDefinition(String id, String kind, int energyCost, int poiseDamage, int power) {
        this(id, kind, legacyEnergyDelta(kind, energyCost), power, poiseDamage, "MELEE",
                defaultTargeting(kind), 0, List.of(new Effect("DAMAGE", "", 1.0D, 0, 1.0D)));
    }

    /** Core compatibility accessor while BattleInstance migrates to energyDelta semantics. */
    public int energyCost() {
        return Math.max(0, -energyDelta);
    }

    public int power() { return hpPower; }
    public int poiseDamage() { return poisePower; }

    private static int legacyEnergyDelta(String kind, int energyCost) {
        if ("SKILL".equals(kind) || "BURST".equals(kind)) return -energyCost;
        return 0;
    }

    private static Targeting defaultTargeting(String kind) {
        if ("GUARD".equals(kind) || "PASSIVE".equals(kind)) return new Targeting("SELF", "SINGLE", 1);
        return new Targeting("ENEMY", "SINGLE", 1);
    }
}
