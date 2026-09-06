package kr.moonseungjun.turnboundre.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Canonical data-driven battle action contract used by M0+ registries and the M2 network resolver.
 * The five-argument constructor remains for universal/debug compatibility; production data should
 * provide the explicit targeting block described by docs/13_DATA_SCHEMA.md.
 */
public record ActionDefinition(
        String id,
        String kind,
        int energyCost,
        int poiseDamage,
        int power,
        Targeting targeting
) {
    public record Targeting(String team, String shape, int count) {
        public static final Codec<Targeting> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("team").forGetter(Targeting::team),
                Codec.STRING.fieldOf("shape").forGetter(Targeting::shape),
                Codec.INT.fieldOf("count").forGetter(Targeting::count)
        ).apply(instance, Targeting::new));
    }

    public ActionDefinition(String id, String kind, int energyCost, int poiseDamage, int power) {
        this(id, kind, energyCost, poiseDamage, power, defaultTargeting(kind));
    }

    private static Targeting defaultTargeting(String kind) {
        if ("GUARD".equals(kind) || "PASSIVE".equals(kind)) {
            return new Targeting("SELF", "SINGLE", 1);
        }
        return new Targeting("ENEMY", "SINGLE", 1);
    }

    public static final Codec<ActionDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(ActionDefinition::id),
            Codec.STRING.fieldOf("kind").forGetter(ActionDefinition::kind),
            Codec.INT.fieldOf("energyCost").forGetter(ActionDefinition::energyCost),
            Codec.INT.fieldOf("poiseDamage").forGetter(ActionDefinition::poiseDamage),
            Codec.INT.fieldOf("power").forGetter(ActionDefinition::power),
            Targeting.CODEC.fieldOf("targeting").forGetter(ActionDefinition::targeting)
    ).apply(instance, ActionDefinition::new));
}
