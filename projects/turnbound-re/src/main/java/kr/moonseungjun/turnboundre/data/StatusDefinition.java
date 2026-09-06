package kr.moonseungjun.turnboundre.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record StatusDefinition(
        String id,
        String polarity,
        String durationUnit,
        int maxStacks,
        String refreshRule,
        List<String> dispelTags,
        List<String> hooks
) {
    public static final Codec<StatusDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(StatusDefinition::id),
            Codec.STRING.fieldOf("polarity").forGetter(StatusDefinition::polarity),
            Codec.STRING.fieldOf("durationUnit").forGetter(StatusDefinition::durationUnit),
            Codec.INT.fieldOf("maxStacks").forGetter(StatusDefinition::maxStacks),
            Codec.STRING.fieldOf("refreshRule").forGetter(StatusDefinition::refreshRule),
            Codec.STRING.listOf().fieldOf("dispelTags").forGetter(StatusDefinition::dispelTags),
            Codec.STRING.listOf().fieldOf("hooks").forGetter(StatusDefinition::hooks)
    ).apply(instance, StatusDefinition::new));

    public StatusDefinition {
        dispelTags = dispelTags == null ? List.of() : List.copyOf(dispelTags);
        hooks = hooks == null ? List.of() : List.copyOf(hooks);
    }
}
