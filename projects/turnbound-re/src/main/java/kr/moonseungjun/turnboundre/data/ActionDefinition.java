package kr.moonseungjun.turnboundre.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ActionDefinition(String id, String kind, int energyCost, int poiseDamage, int power) {
    public static final Codec<ActionDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(ActionDefinition::id),
            Codec.STRING.fieldOf("kind").forGetter(ActionDefinition::kind),
            Codec.INT.fieldOf("energyCost").forGetter(ActionDefinition::energyCost),
            Codec.INT.fieldOf("poiseDamage").forGetter(ActionDefinition::poiseDamage),
            Codec.INT.fieldOf("power").forGetter(ActionDefinition::power)
    ).apply(instance, ActionDefinition::new));
}
