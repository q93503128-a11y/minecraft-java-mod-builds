package kr.moonseungjun.turnboundre.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DefinitionValidator {
    private static final Set<String> ACTION_KINDS = Set.of("BASIC", "SKILL", "GUARD", "BURST", "PASSIVE");
    private static final Set<String> TARGET_TEAMS = Set.of("SELF", "ALLY", "ENEMY", "ANY");
    private static final Set<String> TARGET_SHAPES = Set.of("SINGLE", "MULTI");
    private static final Set<String> ROLES = Set.of("VANGUARD", "BREAKER", "STRIKER", "SUPPORT", "CONTROL");
    private static final Set<String> AFFINITIES = Set.of("FLAME", "TIDE", "GALE", "STONE", "LIGHT", "DARK");
    private static final Set<String> STATUS_POLARITIES = Set.of("POSITIVE", "NEGATIVE", "NEUTRAL");
    private static final Set<String> STATUS_DURATION_UNITS = Set.of("TURN", "CYCLE");

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

            ActionDefinition.Targeting targeting = action.targeting();
            if (targeting == null) {
                errors.add(id + ": targeting must not be null");
                continue;
            }
            if (!TARGET_TEAMS.contains(targeting.team())) {
                errors.add(id + ": unknown targeting team " + targeting.team());
            }
            if (!TARGET_SHAPES.contains(targeting.shape())) {
                errors.add(id + ": unknown targeting shape " + targeting.shape());
            }
            if (targeting.count() < 1) errors.add(id + ": targeting count must be >= 1");
            if ("SINGLE".equals(targeting.shape()) && targeting.count() != 1) {
                errors.add(id + ": SINGLE targeting count must be 1");
            }
        }
        return List.copyOf(errors);
    }

    public static List<String> validateStatuses(List<StatusDefinition> statuses) {
        List<String> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (StatusDefinition status : statuses) {
            String id = status.id();
            if (id == null || id.isBlank()) errors.add("status id is blank");
            else if (!seen.add(id)) errors.add("duplicate status id: " + id);
            if (!STATUS_POLARITIES.contains(status.polarity())) errors.add(id + ": unknown polarity " + status.polarity());
            if (!STATUS_DURATION_UNITS.contains(status.durationUnit())) errors.add(id + ": unknown durationUnit " + status.durationUnit());
            if (status.maxStacks() < 1) errors.add(id + ": maxStacks must be >= 1");
            if (status.refreshRule() == null || status.refreshRule().isBlank()) errors.add(id + ": refreshRule must not be blank");
            for (String tag : status.dispelTags()) if (tag == null || tag.isBlank()) errors.add(id + ": blank dispelTag");
            for (String hook : status.hooks()) if (hook == null || hook.isBlank()) errors.add(id + ": blank hook");
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
