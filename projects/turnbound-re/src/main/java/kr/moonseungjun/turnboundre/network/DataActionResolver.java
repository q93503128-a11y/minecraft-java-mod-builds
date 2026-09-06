package kr.moonseungjun.turnboundre.network;

import kr.moonseungjun.turnboundre.battle.ActionUsePolicy;
import kr.moonseungjun.turnboundre.data.ActionDefinition;
import kr.moonseungjun.turnboundre.data.CharacterDefinition;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;

import java.util.Optional;
import java.util.UUID;

/**
 * Converts validated, loaded data definitions plus runtime eligibility into the strict M1 action policy.
 * It never fabricates missing actions, character ownership, cooldown state, status eligibility or targeting.
 */
public final class DataActionResolver {
    @FunctionalInterface
    public interface ParticipantCharacterSource {
        String characterId(UUID battleId, String participantId);
    }

    public interface RuntimeEligibility {
        boolean cooldownReady(UUID battleId, String participantId, String actionId);
        boolean statusEligible(UUID battleId, String participantId, String actionId);
    }

    public static final RuntimeEligibility ALLOW_ALL_RUNTIME = new RuntimeEligibility() {
        @Override
        public boolean cooldownReady(UUID battleId, String participantId, String actionId) {
            return true;
        }

        @Override
        public boolean statusEligible(UUID battleId, String participantId, String actionId) {
            return true;
        }
    };

    public record ResolvedAction(ActionDefinition definition, ActionUsePolicy policy) {}

    private final DefinitionRegistry definitions;
    private final ParticipantCharacterSource characters;
    private final RuntimeEligibility runtimeEligibility;

    public DataActionResolver(
            DefinitionRegistry definitions,
            ParticipantCharacterSource characters,
            RuntimeEligibility runtimeEligibility
    ) {
        if (definitions == null) throw new IllegalArgumentException("definitions must not be null");
        if (characters == null) throw new IllegalArgumentException("characters must not be null");
        if (runtimeEligibility == null) throw new IllegalArgumentException("runtimeEligibility must not be null");
        this.definitions = definitions;
        this.characters = characters;
        this.runtimeEligibility = runtimeEligibility;
    }

    public Optional<ResolvedAction> resolve(UUID battleId, String participantId, String actionId) {
        if (battleId == null || participantId == null || participantId.isBlank() || actionId == null || actionId.isBlank()) {
            return Optional.empty();
        }

        ActionDefinition action = definitions.actions().get(actionId);
        if (action == null || !("SKILL".equals(action.kind()) || "BURST".equals(action.kind()))) {
            return Optional.empty();
        }

        String characterId = characters.characterId(battleId, participantId);
        if (characterId == null || characterId.isBlank()) return Optional.empty();
        CharacterDefinition character = definitions.characters().get(characterId);
        if (character == null) return Optional.empty();

        ActionUsePolicy.TargetRule targetRule = parseTargetRule(action.targeting().team());
        if (targetRule == null) return Optional.empty();
        int targetCount = targetCount(action.targeting());
        if (targetCount < 1) return Optional.empty();

        boolean owned = character.actions().contains(actionId);
        boolean cooldownReady = runtimeEligibility.cooldownReady(battleId, participantId, actionId);
        boolean statusEligible = runtimeEligibility.statusEligible(battleId, participantId, actionId);
        ActionUsePolicy policy = new ActionUsePolicy(
                owned,
                cooldownReady,
                statusEligible,
                targetCount,
                targetCount,
                targetRule
        );
        return Optional.of(new ResolvedAction(action, policy));
    }

    private static int targetCount(ActionDefinition.Targeting targeting) {
        if (targeting == null) return -1;
        return switch (targeting.shape()) {
            case "SINGLE" -> targeting.count() == 1 ? 1 : -1;
            case "MULTI" -> targeting.count() > 1 ? targeting.count() : -1;
            default -> -1;
        };
    }

    private static ActionUsePolicy.TargetRule parseTargetRule(String team) {
        if (team == null) return null;
        return switch (team) {
            case "SELF" -> ActionUsePolicy.TargetRule.SELF;
            case "ALLY" -> ActionUsePolicy.TargetRule.ALLY;
            case "ENEMY" -> ActionUsePolicy.TargetRule.ENEMY;
            case "ANY" -> ActionUsePolicy.TargetRule.ANY;
            default -> null;
        };
    }
}
