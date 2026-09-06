package kr.moonseungjun.turnboundre.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable, validated M0 registry for data-driven battle definitions.
 * Construction is fail-fast so invalid datapack content cannot silently enter battle state.
 */
public final class DefinitionRegistry {
    private final Map<String, ActionDefinition> actions;
    private final Map<String, CharacterDefinition> characters;

    private DefinitionRegistry(Map<String, ActionDefinition> actions, Map<String, CharacterDefinition> characters) {
        this.actions = Collections.unmodifiableMap(actions);
        this.characters = Collections.unmodifiableMap(characters);
    }

    public static DefinitionRegistry create(List<ActionDefinition> actions, List<CharacterDefinition> characters) {
        List<String> actionErrors = DefinitionValidator.validateActions(actions);
        Set<String> actionIds = actions.stream()
                .map(ActionDefinition::id)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());
        List<String> characterErrors = DefinitionValidator.validateCharacters(characters, actionIds);

        if (!actionErrors.isEmpty() || !characterErrors.isEmpty()) {
            throw new IllegalArgumentException("Invalid TURNBOUND definitions: "
                    + String.join("; ", actionErrors) + (actionErrors.isEmpty() || characterErrors.isEmpty() ? "" : "; ")
                    + String.join("; ", characterErrors));
        }

        Map<String, ActionDefinition> actionMap = new LinkedHashMap<>();
        for (ActionDefinition action : actions) actionMap.put(action.id(), action);
        Map<String, CharacterDefinition> characterMap = new LinkedHashMap<>();
        for (CharacterDefinition character : characters) characterMap.put(character.id(), character);
        return new DefinitionRegistry(actionMap, characterMap);
    }

    public Map<String, ActionDefinition> actions() {
        return actions;
    }

    public Map<String, CharacterDefinition> characters() {
        return characters;
    }
}
