package kr.moonseungjun.turnboundre.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/** Server-authoritative encounter reward table. */
public record RewardTableDefinition(String id, List<Roll> rolls) {
    public record Roll(
            String type,
            String character,
            int min,
            int max,
            int weight,
            double chance
    ) {
        public static final Codec<Roll> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("type").forGetter(Roll::type),
                Codec.STRING.optionalFieldOf("character", "").forGetter(Roll::character),
                Codec.INT.fieldOf("min").forGetter(Roll::min),
                Codec.INT.fieldOf("max").forGetter(Roll::max),
                Codec.INT.optionalFieldOf("weight", 1).forGetter(Roll::weight),
                Codec.DOUBLE.optionalFieldOf("chance", 1.0D).forGetter(Roll::chance)
        ).apply(instance, Roll::new));
    }

    public static final Codec<RewardTableDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(RewardTableDefinition::id),
            Roll.CODEC.listOf().fieldOf("rolls").forGetter(RewardTableDefinition::rolls)
    ).apply(instance, RewardTableDefinition::new));

    public RewardTableDefinition {
        rolls = rolls == null ? List.of() : List.copyOf(rolls);
    }
}
