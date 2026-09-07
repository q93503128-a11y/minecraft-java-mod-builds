package kr.moonseungjun.turnboundre.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable, atomically validated registry for all server-authoritative content definitions.
 */
public final class DefinitionRegistry {
    private final Map<String, ActionDefinition> actions;
    private final Map<String, CharacterDefinition> characters;
    private final Map<String, StatusDefinition> statuses;
    private final Map<String, EncounterDefinition> encounters;
    private final Map<String, RewardTableDefinition> rewards;

    private DefinitionRegistry(
            Map<String, ActionDefinition> actions,
            Map<String, CharacterDefinition> characters,
            Map<String, StatusDefinition> statuses,
            Map<String, EncounterDefinition> encounters,
            Map<String, RewardTableDefinition> rewards
    ) {
        this.actions = Collections.unmodifiableMap(actions);
        this.characters = Collections.unmodifiableMap(characters);
        this.statuses = Collections.unmodifiableMap(statuses);
        this.encounters = Collections.unmodifiableMap(encounters);
        this.rewards = Collections.unmodifiableMap(rewards);
    }

    public static DefinitionRegistry create(List<ActionDefinition> actions, List<CharacterDefinition> characters) {
        return create(actions, characters, List.of(), List.of(), List.of());
    }

    public static DefinitionRegistry create(
            List<ActionDefinition> actions,
            List<CharacterDefinition> characters,
            List<StatusDefinition> statuses
    ) {
        return create(actions, characters, statuses, List.of(), List.of());
    }

    public static DefinitionRegistry create(DefinitionBundle bundle) {
        if (bundle == null) throw new IllegalArgumentException("bundle must not be null");
        return create(bundle.actions(), bundle.characters(), bundle.statuses(), bundle.encounters(), bundle.rewards());
    }

    public static DefinitionRegistry create(
            List<ActionDefinition> actions,
            List<CharacterDefinition> characters,
            List<StatusDefinition> statuses,
            List<EncounterDefinition> encounters,
            List<RewardTableDefinition> rewards
    ) {
        if (actions == null || characters == null || statuses == null || encounters == null || rewards == null) {
            throw new IllegalArgumentException("definition lists must not be null");
        }

        List<String> errors = new ArrayList<>();
        errors.addAll(DefinitionValidator.validateActions(actions));
        errors.addAll(DefinitionValidator.validateStatuses(statuses));

        Set<String> actionIds = ids(actions.stream().map(ActionDefinition::id).toList());
        errors.addAll(DefinitionValidator.validateCharacters(characters, actionIds));
        errors.addAll(DefinitionCrossReferenceValidator.validate(actions, characters, statuses));

        Map<String, CharacterDefinition> characterMap = mapCharacters(characters);
        Set<String> characterIds = Set.copyOf(characterMap.keySet());
        errors.addAll(DefinitionValidator.validateRewards(rewards, characterIds));

        Set<String> rewardIds = ids(rewards.stream().map(RewardTableDefinition::id).toList());
        errors.addAll(DefinitionValidator.validateEncounters(encounters, characterMap, rewardIds));

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Invalid TURNBOUND definitions: " + String.join("; ", errors));
        }

        return new DefinitionRegistry(
                mapActions(actions), characterMap, mapStatuses(statuses), mapEncounters(encounters), mapRewards(rewards));
    }

    private static Set<String> ids(List<String> values) {
        return values.stream().filter(id -> id != null && !id.isBlank()).collect(Collectors.toUnmodifiableSet());
    }

    private static Map<String, ActionDefinition> mapActions(List<ActionDefinition> values) {
        Map<String, ActionDefinition> out = new LinkedHashMap<>();
        for (ActionDefinition value : values) out.put(value.id(), value);
        return out;
    }

    private static Map<String, CharacterDefinition> mapCharacters(List<CharacterDefinition> values) {
        Map<String, CharacterDefinition> out = new LinkedHashMap<>();
        for (CharacterDefinition value : values) out.put(value.id(), value);
        return out;
    }

    private static Map<String, StatusDefinition> mapStatuses(List<StatusDefinition> values) {
        Map<String, StatusDefinition> out = new LinkedHashMap<>();
        for (StatusDefinition value : values) out.put(value.id(), value);
        return out;
    }

    private static Map<String, EncounterDefinition> mapEncounters(List<EncounterDefinition> values) {
        Map<String, EncounterDefinition> out = new LinkedHashMap<>();
        for (EncounterDefinition value : values) out.put(value.id(), value);
        return out;
    }

    private static Map<String, RewardTableDefinition> mapRewards(List<RewardTableDefinition> values) {
        Map<String, RewardTableDefinition> out = new LinkedHashMap<>();
        for (RewardTableDefinition value : values) out.put(value.id(), value);
        return out;
    }

    public Map<String, ActionDefinition> actions() { return actions; }
    public Map<String, CharacterDefinition> characters() { return characters; }
    public Map<String, StatusDefinition> statuses() { return statuses; }
    public Map<String, EncounterDefinition> encounters() { return encounters; }
    public Map<String, RewardTableDefinition> rewards() { return rewards; }
}
