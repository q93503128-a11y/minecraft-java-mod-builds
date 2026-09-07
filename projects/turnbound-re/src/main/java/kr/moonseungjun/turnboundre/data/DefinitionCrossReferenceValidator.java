package kr.moonseungjun.turnboundre.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cross-reference validation that requires multiple already-decoded definition families.
 * Keeping this separate from scalar/schema validation prevents permissive fallback behavior at runtime.
 */
public final class DefinitionCrossReferenceValidator {
    private DefinitionCrossReferenceValidator() {}

    public static List<String> validate(
            List<ActionDefinition> actions,
            List<CharacterDefinition> characters,
            List<StatusDefinition> statuses
    ) {
        Map<String, ActionDefinition> byAction = new LinkedHashMap<>();
        for (ActionDefinition action : actions) {
            if (action != null && action.id() != null) byAction.put(action.id(), action);
        }
        Set<String> statusIds = statuses.stream()
                .filter(status -> status != null && status.id() != null)
                .map(StatusDefinition::id)
                .collect(Collectors.toUnmodifiableSet());

        List<String> errors = new ArrayList<>();
        for (CharacterDefinition character : characters) {
            if (character == null) continue;
            requireKind(errors, character.id(), "basicAction", character.basicAction(), "BASIC", byAction);
            for (String skill : character.skills()) {
                requireKind(errors, character.id(), "skills", skill, "SKILL", byAction);
            }
            requireKind(errors, character.id(), "burst", character.burst(), "BURST", byAction);
            for (String passive : character.passives()) {
                requireKind(errors, character.id(), "passives", passive, "PASSIVE", byAction);
            }
        }

        for (ActionDefinition action : actions) {
            if (action == null) continue;
            for (ActionDefinition.Effect effect : action.effects()) {
                if (effect == null) continue;
                if (("APPLY_STATUS".equals(effect.type()) || "REMOVE_STATUS".equals(effect.type()))
                        && !statusIds.contains(effect.status())) {
                    errors.add(action.id() + ": effect " + effect.type() + " references missing status " + effect.status());
                }
            }
        }
        return List.copyOf(errors);
    }

    private static void requireKind(
            List<String> errors,
            String characterId,
            String slot,
            String actionId,
            String expectedKind,
            Map<String, ActionDefinition> actions
    ) {
        ActionDefinition action = actions.get(actionId);
        if (action == null) return; // Missing reference is reported by DefinitionValidator.
        if (!expectedKind.equals(action.kind())) {
            errors.add(characterId + ": " + slot + " requires " + expectedKind
                    + " but " + actionId + " is " + action.kind());
        }
    }
}
