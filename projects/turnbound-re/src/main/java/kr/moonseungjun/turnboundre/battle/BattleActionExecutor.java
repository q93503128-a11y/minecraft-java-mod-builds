package kr.moonseungjun.turnboundre.battle;

import kr.moonseungjun.turnboundre.data.ActionDefinition;
import kr.moonseungjun.turnboundre.data.CharacterDefinition;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.StatusDefinition;

import java.util.List;

/**
 * Executes already-authorized data actions against the deterministic battle core.
 * Command ownership/target legality belongs to BattleCommandService; this class only interprets validated effects.
 */
public final class BattleActionExecutor {
    private final DefinitionRegistry definitions;
    private final BattleCharacterSource characters;

    public BattleActionExecutor(DefinitionRegistry definitions, BattleCharacterSource characters) {
        if (definitions == null) throw new IllegalArgumentException("definitions must not be null");
        if (characters == null) throw new IllegalArgumentException("characters must not be null");
        this.definitions = definitions;
        this.characters = characters;
    }

    public void execute(BattleInstance battle, String actorId, ActionDefinition action, List<String> targetIds) {
        if (battle == null) throw new IllegalArgumentException("battle must not be null");
        if (actorId == null || actorId.isBlank()) throw new IllegalArgumentException("actorId must not be blank");
        if (action == null) throw new IllegalArgumentException("action must not be null");
        if (targetIds == null || targetIds.isEmpty()) throw new IllegalArgumentException("targetIds must not be empty");
        if (battle.state() != BattleState.RESOLVING) throw new IllegalStateException("action execution requires RESOLVING");

        BattleParticipant actor = battle.participant(actorId);
        ParticipantCombatState actorState = battle.combatState(actorId);
        for (ActionDefinition.Effect effect : action.effects()) {
            if (effect == null || "NONE".equals(effect.type())) continue;
            for (String targetId : targetIds) {
                if (!battle.rollEffectChance(effect.chance())) {
                    battle.recordEffectEvent("EFFECT_MISSED", actorId,
                            "action=" + action.id() + " effect=" + effect.type() + " target=" + targetId);
                    continue;
                }
                applyEffect(battle, actor, actorState, action, effect, targetId);
            }
        }
    }

    private void applyEffect(
            BattleInstance battle,
            BattleParticipant actor,
            ParticipantCombatState actorState,
            ActionDefinition action,
            ActionDefinition.Effect effect,
            String targetId
    ) {
        BattleParticipant target = battle.participant(targetId);
        ParticipantCombatState targetState = battle.combatState(targetId);
        switch (effect.type()) {
            case "DAMAGE" -> {
                AffinityGrade affinity = affinityFor(battle, targetId, action.damageTag());
                int attack = scaledStat(actor.attack(), actorState.statuses().multiplier("ATK_MULTIPLIER"));
                int defense = scaledStat(target.defense(), targetState.statuses().multiplier("DEF_MULTIPLIER"));
                int hpPower = scaledPower(action.hpPower(), effect.value());
                int poisePower = scaledPower(action.poisePower(), effect.value());
                poisePower = scaledStat(poisePower, targetState.statuses().multiplier("POISE_TAKEN_MULTIPLIER"));
                double damageTaken = targetState.statuses().multiplier("DAMAGE_TAKEN_MULTIPLIER");
                battle.resolveDamage(actor.id(), targetId, new DamageService.DamageRequest(
                        DamageTag.valueOf(action.damageTag()), affinity, hpPower, poisePower,
                        attack, defense, targetState.exposed(), DamageService.DEFAULT_CRIT_CHANCE,
                        DamageService.DEFAULT_CRIT_MULTIPLIER, damageTaken));
            }
            case "HEAL" -> {
                int attack = scaledStat(actor.attack(), actorState.statuses().multiplier("ATK_MULTIPLIER"));
                int healPower = scaledPower(action.hpPower(), effect.value());
                int amount = Math.max(0, (int) Math.floor(healPower * attack / 100.0D));
                battle.resolveHeal(actor.id(), targetId, amount, action.id());
            }
            case "APPLY_STATUS" -> {
                StatusDefinition status = definitions.statuses().get(effect.status());
                if (status == null) throw new IllegalStateException("validated action lost status definition " + effect.status());
                battle.applyDataStatus(actor.id(), targetId, status, effect.duration());
            }
            case "REMOVE_STATUS" -> battle.removeDataStatus(actor.id(), targetId, effect.status());
            case "ENERGY" -> battle.adjustEnergy(actor.id(), targetId, (int) Math.round(effect.value()), action.id());
            case "POISE_DAMAGE" -> {
                AffinityGrade affinity = affinityFor(battle, targetId, action.damageTag());
                int poisePower = scaledPower(action.poisePower(), effect.value());
                poisePower = scaledStat(poisePower, targetState.statuses().multiplier("POISE_TAKEN_MULTIPLIER"));
                battle.resolvePoiseOnly(actor.id(), targetId, affinity, poisePower, action.id());
            }
            case "INTENT_DELAY" -> battle.delayEnemyIntent(actor.id(), targetId, action.id());
            default -> throw new IllegalStateException("validated action has unsupported effect " + effect.type());
        }
    }

    private AffinityGrade affinityFor(BattleInstance battle, String participantId, String damageTag) {
        String characterId = characters.characterId(battle.battleId(), participantId);
        if (characterId == null || characterId.isBlank()) {
            throw new IllegalStateException("battle participant has no CharacterDefinition mapping: " + participantId);
        }
        CharacterDefinition character = definitions.characters().get(characterId);
        if (character == null) throw new IllegalStateException("battle participant references missing character " + characterId);
        String grade = character.affinities().get(damageTag);
        if (grade == null) throw new IllegalStateException(characterId + " missing affinity " + damageTag);
        return AffinityGrade.valueOf(grade);
    }

    private static int scaledPower(int base, double effectValue) {
        double scale = effectValue > 0.0D ? effectValue : 1.0D;
        return Math.max(0, (int) Math.floor(base * scale));
    }

    private static int scaledStat(int base, double multiplier) {
        if (!Double.isFinite(multiplier) || multiplier < 0.0D) {
            throw new IllegalStateException("invalid runtime stat multiplier " + multiplier);
        }
        return Math.max(0, (int) Math.floor(base * multiplier));
    }
}
