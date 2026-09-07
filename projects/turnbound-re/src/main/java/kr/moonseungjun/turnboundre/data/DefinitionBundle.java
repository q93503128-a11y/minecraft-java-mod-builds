package kr.moonseungjun.turnboundre.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/** One atomic content pack. Multiple bundles can be merged by the server reload layer after validation. */
public record DefinitionBundle(
        List<ActionDefinition> actions,
        List<CharacterDefinition> characters,
        List<StatusDefinition> statuses,
        List<EncounterDefinition> encounters,
        List<RewardTableDefinition> rewards,
        List<ProgressionDefinition> progressions
) {
    public static final Codec<DefinitionBundle> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ActionDefinition.CODEC.listOf().optionalFieldOf("actions", List.of()).forGetter(DefinitionBundle::actions),
            CharacterDefinition.CODEC.listOf().optionalFieldOf("characters", List.of()).forGetter(DefinitionBundle::characters),
            StatusDefinition.CODEC.listOf().optionalFieldOf("statuses", List.of()).forGetter(DefinitionBundle::statuses),
            EncounterDefinition.CODEC.listOf().optionalFieldOf("encounters", List.of()).forGetter(DefinitionBundle::encounters),
            RewardTableDefinition.CODEC.listOf().optionalFieldOf("rewards", List.of()).forGetter(DefinitionBundle::rewards),
            ProgressionDefinition.CODEC.listOf().optionalFieldOf("progressions", List.of()).forGetter(DefinitionBundle::progressions)
    ).apply(instance, DefinitionBundle::new));

    public DefinitionBundle {
        actions = actions == null ? List.of() : List.copyOf(actions);
        characters = characters == null ? List.of() : List.copyOf(characters);
        statuses = statuses == null ? List.of() : List.copyOf(statuses);
        encounters = encounters == null ? List.of() : List.copyOf(encounters);
        rewards = rewards == null ? List.of() : List.copyOf(rewards);
        progressions = progressions == null ? List.of() : List.copyOf(progressions);
    }
}
