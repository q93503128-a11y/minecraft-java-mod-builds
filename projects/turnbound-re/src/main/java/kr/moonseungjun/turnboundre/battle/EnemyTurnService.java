package kr.moonseungjun.turnboundre.battle;

import kr.moonseungjun.turnboundre.data.ActionDefinition;
import kr.moonseungjun.turnboundre.data.CharacterDefinition;

import java.util.ArrayList;
import java.util.List;

/** Server-owned deterministic enemy turn runner. Stops as soon as player input or a terminal state is reached. */
public final class EnemyTurnService {
    private EnemyTurnService() {}

    public static void resolveUntilPlayerOrTerminal(BattleManager battles, BattleInstance battle) {
        if (battles == null || battle == null) throw new IllegalArgumentException("battles/battle required");
        int guard = 0;
        while (battle.state() == BattleState.RESOLVING
                && battle.outcome() == BattleInstance.Outcome.ONGOING
                && battle.participant(battle.currentActorId()).team() == BattleTeam.ENEMY) {
            if (++guard > 64) throw new IllegalStateException("enemy auto-turn guard exceeded for " + battle.battleId());
            resolveOne(battles, battle);
        }
    }

    private static void resolveOne(BattleManager battles, BattleInstance battle) {
        BattleDefinitionContext context = battles.definitionContext(battle.battleId()).orElseThrow(() ->
                new IllegalStateException("authored enemy turn missing definition context: " + battle.battleId()));
        String actorId = battle.currentActorId();
        if (battle.participant(actorId).team() != BattleTeam.ENEMY) {
            throw new IllegalStateException("enemy auto-turn called for player actor " + actorId);
        }

        String characterId = context.characterId(actorId);
        CharacterDefinition character = context.definitions().characters().get(characterId);
        if (character == null) throw new IllegalStateException("enemy character disappeared: " + characterId);
        ActionDefinition action = context.definitions().actions().get(character.basicAction());
        if (action == null) throw new IllegalStateException("enemy basic action disappeared: " + character.basicAction());

        AuthoredEncounterLauncher.publishAuthoredEnemyIntent(battle, context, actorId);
        List<String> targets = targets(battle, actorId, action);
        battle.resolveEnemyStub();
        new BattleActionExecutor(context.definitions(), (battleId, participantId) -> context.characterId(participantId))
                .execute(battle, actorId, action, targets);
        battle.finishResolution();
    }

    private static List<String> targets(BattleInstance battle, String actorId, ActionDefinition action) {
        BattleParticipant actor = battle.participant(actorId);
        List<String> candidates = new ArrayList<>();
        for (String targetId : battle.actorOrder()) {
            if (!battle.combatState(targetId).alive()) continue;
            BattleParticipant target = battle.participant(targetId);
            boolean allowed = switch (action.targeting().team()) {
                case "SELF" -> targetId.equals(actorId);
                case "ALLY" -> target.team() == actor.team();
                case "ENEMY" -> target.team() != actor.team();
                case "ANY" -> true;
                default -> false;
            };
            if (allowed) candidates.add(targetId);
        }
        int count = action.targeting().count();
        if (count < 1 || candidates.size() < count) {
            throw new IllegalStateException("enemy action has insufficient legal targets: " + action.id());
        }
        return List.copyOf(candidates.subList(0, count));
    }
}
