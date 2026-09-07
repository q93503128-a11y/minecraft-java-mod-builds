package kr.moonseungjun.turnboundre.battle;

import kr.moonseungjun.turnboundre.data.ActionDefinition;
import kr.moonseungjun.turnboundre.data.CharacterDefinition;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.DefinitionRepository;
import kr.moonseungjun.turnboundre.data.EncounterDefinition;
import kr.moonseungjun.turnboundre.data.RewardTableDefinition;
import kr.moonseungjun.turnboundre.progression.CharacterProgress;
import kr.moonseungjun.turnboundre.progression.ProgressionRules;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Opens one authored EncounterDefinition from a single immutable definition snapshot. */
public final class AuthoredEncounterLauncher {
    public record PlayerSlot(String participantId, CharacterProgress progress, UUID entityId) {
        public PlayerSlot {
            if (participantId == null || participantId.isBlank()) {
                throw new IllegalArgumentException("participantId must not be blank");
            }
            if (progress == null || entityId == null) throw new IllegalArgumentException("progress/entityId required");
        }
    }

    public record Launch(
            EncounterDefinition encounter,
            BattleInstance battle,
            BattleDefinitionContext definitionContext,
            BattleRewardContext rewardContext
    ) {}

    private final BattleManager battles;
    private final DefinitionRepository definitions;

    public AuthoredEncounterLauncher(BattleManager battles, DefinitionRepository definitions) {
        if (battles == null || definitions == null) throw new IllegalArgumentException("battles/definitions required");
        this.battles = battles;
        this.definitions = definitions;
    }

    public Launch open(
            String encounterId,
            UUID ownerPlayerId,
            List<PlayerSlot> playerSlots,
            List<UUID> enemyEntityIds,
            UUID battleId,
            long battleSeed
    ) {
        if (encounterId == null || encounterId.isBlank()) throw new IllegalArgumentException("encounterId must not be blank");
        if (ownerPlayerId == null || battleId == null) throw new IllegalArgumentException("ownerPlayerId/battleId required");
        if (playerSlots == null || playerSlots.isEmpty()) throw new IllegalArgumentException("playerSlots must not be empty");
        if (enemyEntityIds == null) throw new IllegalArgumentException("enemyEntityIds must not be null");

        DefinitionRepository.Snapshot snapshot = definitions.snapshot();
        DefinitionRegistry registry = snapshot.registry();
        EncounterDefinition encounter = registry.encounters().get(encounterId);
        if (encounter == null) throw new IllegalArgumentException("unknown authored encounter " + encounterId);
        if (enemyEntityIds.size() != encounter.enemies().size()) {
            throw new IllegalArgumentException("enemy entity count " + enemyEntityIds.size()
                    + " does not match authored encounter slots " + encounter.enemies().size());
        }
        RewardTableDefinition rewardTable = registry.rewards().get(encounter.rewardTable());
        if (rewardTable == null) {
            throw new IllegalStateException("validated encounter reward table disappeared: " + encounter.rewardTable());
        }

        List<BattleParticipant> participants = new ArrayList<>();
        List<EntityParticipantBinding> bindings = new ArrayList<>();
        Map<String, String> characterIds = new LinkedHashMap<>();
        int ordinal = 0;

        for (PlayerSlot slot : playerSlots) {
            if (characterIds.putIfAbsent(slot.participantId(), slot.progress().characterId()) != null) {
                throw new IllegalArgumentException("duplicate player participantId " + slot.participantId());
            }
            CharacterDefinition definition = requireCharacter(registry, slot.progress().characterId());
            CharacterDefinition.Stats stats = ProgressionRules.stats(definition, slot.progress());
            participants.add(participant(slot.participantId(), BattleTeam.PLAYER, ordinal++, stats));
            bindings.add(new EntityParticipantBinding(slot.participantId(), slot.entityId()));
        }

        for (int index = 0; index < encounter.enemies().size(); index++) {
            EncounterDefinition.EnemySlot authored = encounter.enemies().get(index);
            String participantId = "enemy_" + index;
            if (characterIds.containsKey(participantId)) {
                throw new IllegalArgumentException("player participantId collides with authored enemy id " + participantId);
            }
            CharacterDefinition definition = requireCharacter(registry, authored.character());
            CharacterProgress progress = new CharacterProgress(
                    definition.id(), definition.originStar(), authored.currentStar(), authored.level());
            CharacterDefinition.Stats stats = ProgressionRules.stats(definition, progress);
            characterIds.put(participantId, definition.id());
            participants.add(participant(participantId, BattleTeam.ENEMY, ordinal++, stats));
            UUID entityId = enemyEntityIds.get(index);
            if (entityId == null) throw new IllegalArgumentException("enemy entityId must not be null at slot " + index);
            bindings.add(new EntityParticipantBinding(participantId, entityId));
        }

        BattleInstance battle = new BattleInstance(battleId, battleSeed, participants);
        BattleDefinitionContext definitionContext = new BattleDefinitionContext(registry, snapshot.hash(), characterIds);
        BattleRewardContext rewardContext = new BattleRewardContext(
                ownerPlayerId, rewardTable, rewardSeed(battleSeed, ownerPlayerId, encounter.id()));
        battles.register(battle, bindings, participants, definitionContext, rewardContext);
        battle.start();
        publishAuthoredEnemyIntents(battle, definitionContext);
        return new Launch(encounter, battle, definitionContext, rewardContext);
    }

    private static CharacterDefinition requireCharacter(DefinitionRegistry registry, String characterId) {
        CharacterDefinition definition = registry.characters().get(characterId);
        if (definition == null) throw new IllegalArgumentException("missing CharacterDefinition " + characterId);
        return definition;
    }

    private static BattleParticipant participant(
            String participantId,
            BattleTeam team,
            int ordinal,
            CharacterDefinition.Stats stats
    ) {
        return new BattleParticipant(
                participantId, team, ordinal, stats.spd(), stats.hp(), stats.atk(), stats.def(), stats.poise());
    }

    private static void publishAuthoredEnemyIntents(BattleInstance battle, BattleDefinitionContext context) {
        for (String participantId : battle.actorOrder()) {
            if (battle.participant(participantId).team() != BattleTeam.ENEMY || !battle.combatState(participantId).alive()) continue;
            CharacterDefinition enemy = requireCharacter(context.definitions(), context.characterId(participantId));
            ActionDefinition basic = context.definitions().actions().get(enemy.basicAction());
            if (basic == null) throw new IllegalStateException("enemy basic action missing from battle snapshot: " + enemy.basicAction());
            EnemyIntent.Targeting targeting = "MULTI".equals(basic.targeting().shape())
                    ? EnemyIntent.Targeting.ALL : EnemyIntent.Targeting.SINGLE;
            battle.setEnemyIntent(participantId, new EnemyIntent(
                    basic.id(), EnemyIntent.Type.ATTACK, targeting, EnemyIntent.Risk.NORMAL, true, null));
        }
    }

    private static long rewardSeed(long battleSeed, UUID ownerPlayerId, String encounterId) {
        long mixed = battleSeed ^ ownerPlayerId.getMostSignificantBits() ^ Long.rotateLeft(ownerPlayerId.getLeastSignificantBits(), 23);
        mixed ^= ((long) encounterId.hashCode() << 32) ^ Integer.toUnsignedLong(encounterId.hashCode());
        return Long.rotateLeft(mixed ^ 0x9E3779B97F4A7C15L, 17);
    }
}
