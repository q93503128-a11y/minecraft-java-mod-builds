package kr.moonseungjun.turnboundre.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable, validated registry for data-driven battle definitions.
 * Construction is fail-fast so invalid datapack content cannot silently enter battle state.
 */
public final class DefinitionRegistry {
    private final Map<String, ActionDefinition> actions;
    private final Map<String, CharacterDefinition> characters;
    private final Map<String, StatusDefinition> statuses;

    private DefinitionRegistry(
            Map<String, ActionDefinition> actions,
            Map<String, CharacterDefinition> characters,
            Map<String, StatusDefinition> statuses
    ) {
        this.actions = Collections.unmodifiableMap(actions);
        this.characters = Collections.unmodifiableMap(characters);
        this.statuses = Collections.unmodifiableMap(statuses);
    }

    public static DefinitionRegistry create(List<ActionDefinition> actions, List<CharacterDefinition> characters) {
        return create(actions, characters, List.of());
    }

    public static DefinitionRegistry create(
            List<ActionDefinition> actions,
            List<CharacterDefinition> characters,
            List<StatusDefinition> statuses
    ) {
        List<String> actionErrors = DefinitionValidator.validateActions(actions);
        List<String> statusErrors = DefinitionValidator.validateStatuses(statuses);
        Set<String> actionIds = actions.stream()
                .map(ActionDefinition::id)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());
        List<String> characterErrors = DefinitionValidator.validateCharacters(characters, actionIds);

        if (!actionErrors.isEmpty() || !characterErrors.isEmpty() || !statusErrors.isEmpty()) {
            List<String> allErrors = new java.util.ArrayList<>();
            allErrors.addAll(actionErrors);
            allErrors.addAll(characterErrors);
            allErrors.addAll(statusErrors);
            throw new IllegalArgumentException("Invalid TURNBOUND definitions: " + String.join("; ", allErrors));
        }

        Map<String, ActionDefinition> actionMap = new LinkedHashMap<>();
        for (ActionDefinition action : actions) actionMap.put(action.id(), action);
        Map<String, CharacterDefinition> characterMap = new LinkedHashMap<>();
        for (CharacterDefinition character : characters) characterMap.put(character.id(), character);
        Map<String, StatusDefinition> statusMap = new LinkedHashMap<>();
        for (StatusDefinition status : statuses) statusMap.put(status.id(), status);
        return new DefinitionRegistry(actionMap, characterMap, statusMap);
    }

    public Map<String, ActionDefinition> actions() {
        return actions;
    }

    public Map<String, CharacterDefinition> characters() {
        return characters;
    }

    public Map<String, StatusDefinition> statuses() {
        return statuses;
    }
}
