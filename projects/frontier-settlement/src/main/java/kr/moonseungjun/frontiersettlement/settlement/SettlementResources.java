package kr.moonseungjun.frontiersettlement.settlement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SettlementResources(long wood, long stone, long metal, long food) {
    public static final SettlementResources ZERO = new SettlementResources(0L, 0L, 0L, 0L);

    public static final Codec<SettlementResources> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("wood", 0L).forGetter(SettlementResources::wood),
            Codec.LONG.optionalFieldOf("stone", 0L).forGetter(SettlementResources::stone),
            Codec.LONG.optionalFieldOf("metal", 0L).forGetter(SettlementResources::metal),
            Codec.LONG.optionalFieldOf("food", 0L).forGetter(SettlementResources::food)
    ).apply(instance, SettlementResources::new));
}
