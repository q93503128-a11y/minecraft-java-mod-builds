package kr.moonseungjun.turnboundre.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DefinitionValidator {
    private static final Set<String> ACTION_KINDS = Set.of("BASIC", "SKILL", "GUARD", "BURST", "PASSIVE");
    private static final Set<String> ROLES = Set.of("VANGUARD", "BREAKER", "STRIKER", "SUPPORT", "CONTROL");
    private static final Set<String> AFFINITIES = Set.of("FLAME", "TIDE", "GALE", "STONE", "LIGHT", "DARK");

    private DefinitionValidator() {}

    public static List<String> validateActions(List<ActionDefinition> actions) {
        List<String> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ActionDefinition action : actions) {
            String id = action.id();
            if (id == null || id.isBlank()) errors.add("action id is blank");
            else if (!seen.add(id)) errors.add("duplicate action id: " + id);
            if (!ACTION_KINDS.contains(action.kind())) errors.add(id + ": unknown action kind " + action.kind());
            if (action.energyCost() < 0) errors.add(id + ": energyCost must be >= 0");
            if (action.poiseDamage() < 0) errors.add(id + ": poiseDamage must be >= 0");
            if (action.power() < 0) errors.add(id + ": power must be >= 0");
        }
        return List.copyOf(errors);
    }

    public static List<String> validateCharacters(List<CharacterDefinition> characters, Set<String> actionIds) {
        List<String> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (CharacterDefinition character : characters) {
            if (character.id() == null || character.id().isBlank()) errors.add("character id is blank");
            else if (!seen.add(character.id())) errors.add("duplicate character id: " + character.id());
            if (character.originStar() < 1 || character.originStar() > 5) errors.add(character.id() + ": originStar must be 1..5");
            if (!ROLES.contains(character.role())) errors.add(character.id() + ": unknown role " + character.role());
            if (!AFFINITIES.contains(character.affinity())) errors.add(character.id() + ": unknown affinity " + character.affinity());
            for (String action : character.actions()) if (!actionIds.contains(action)) errors.add(character.id() + ": missing action " + action);
        }
        return List.copyOf(errors);
    }
}
