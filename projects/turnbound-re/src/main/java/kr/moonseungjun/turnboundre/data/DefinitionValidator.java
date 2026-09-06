package kr.moonseungjun.turnboundre.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DefinitionValidator {
    private static final Set<String> ROLES = Set.of("VANGUARD", "BREAKER", "STRIKER", "SUPPORT", "CONTROL");
    private static final Set<String> AFFINITIES = Set.of("FLAME", "TIDE", "GALE", "STONE", "LIGHT", "DARK");

    private DefinitionValidator() {}

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
