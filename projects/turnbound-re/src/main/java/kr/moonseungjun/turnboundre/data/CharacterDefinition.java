package kr.moonseungjun.turnboundre.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record CharacterDefinition(String id, int originStar, String role, String affinity, List<String> actions) {
    public static final Codec<CharacterDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(CharacterDefinition::id),
            Codec.INT.fieldOf("originStar").forGetter(CharacterDefinition::originStar),
            Codec.STRING.fieldOf("role").forGetter(CharacterDefinition::role),
            Codec.STRING.fieldOf("affinity").forGetter(CharacterDefinition::affinity),
            Codec.STRING.listOf().fieldOf("actions").forGetter(CharacterDefinition::actions)
    ).apply(instance, CharacterDefinition::new));
}
