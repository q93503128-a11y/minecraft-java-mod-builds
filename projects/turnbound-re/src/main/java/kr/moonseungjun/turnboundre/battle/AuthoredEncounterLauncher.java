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

    /** Legacy/entity-backed adapter retained for tests and later presentation entities. */
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
        EncounterDefinition encounter = requireEncounter(registry, encounterId);
        if (enemyEntityIds.size() != encounter.enemies().size()) {
            throw new IllegalArgumentException("enemy entity count " + enemyEntityIds.size()
                    + " does not match authored encounter slots " + encounter.enemies().size());
        }
        RewardTableDefinition rewardTable = requireReward(registry, encounter);

        List<BattleParticipant> participants = new ArrayList<>();
        List<EntityParticipantBinding> bindings = new ArrayList<>();
        Map<String, String> characterIds = new LinkedHashMap<>();
        Map<String, UUID> controllers = new LinkedHashMap<>();
        int ordinal = 0;

        for (PlayerSlot slot : playerSlots) {
            addPlayer(registry, participants, characterIds, controllers, slot.participantId(), slot.progress(), ownerPlayerId, ordinal++);
            bindings.add(new EntityParticipantBinding(slot.participantId(), slot.entityId()));
        }

        for (int index = 0; index < encounter.enemies().size(); index++) {
            String participantId = "enemy_" + index;
            addEnemy(registry, participants, characterIds, encounter.enemies().get(index), participantId, ordinal++);
            UUID entityId = enemyEntityIds.get(index);
            if (entityId == null) throw new IllegalArgumentException("enemy entityId must not be null at slot " + index);
            bindings.add(new EntityParticipantBinding(participantId, entityId));
        }

        return register(encounter, rewardTable, snapshot, ownerPlayerId, battleId, battleSeed,
                participants, bindings, characterIds, controllers);
    }

    /**
     * Production gameplay path while final character presentation entities are not yet approved.
     * Participants are logical TURNBOUND actors only: no Minecraft Mob or phantom entity UUID is required.
     */
    public Launch openVirtual(
            String encounterId,
            UUID ownerPlayerId,
            List<CharacterProgress> party,
            UUID battleId,
            long battleSeed
    ) {
        if (encounterId == null || encounterId.isBlank()) throw new IllegalArgumentException("encounterId must not be blank");
        if (ownerPlayerId == null || battleId == null) throw new IllegalArgumentException("ownerPlayerId/battleId required");
        if (party == null || party.isEmpty() || party.size() > 4) throw new IllegalArgumentException("party must contain 1..4 characters");

        DefinitionRepository.Snapshot snapshot = definitions.snapshot();
        DefinitionRegistry registry = snapshot.registry();
        EncounterDefinition encounter = requireEncounter(registry, encounterId);
        RewardTableDefinition rewardTable = requireReward(registry, encounter);

        List<BattleParticipant> participants = new ArrayList<>();
        Map<String, String> characterIds = new LinkedHashMap<>();
        Map<String, UUID> controllers = new LinkedHashMap<>();
        int ordinal = 0;
        for (int index = 0; index < party.size(); index++) {
            CharacterProgress progress = party.get(index);
            if (progress == null) throw new IllegalArgumentException("party progress must not be null");
            addPlayer(registry, participants, characterIds, controllers,
                    "party_" + index, progress, ownerPlayerId, ordinal++);
        }
        for (int index = 0; index < encounter.enemies().size(); index++) {
            addEnemy(registry, participants, characterIds, encounter.enemies().get(index), "enemy_" + index, ordinal++);
        }

        return register(encounter, rewardTable, snapshot, ownerPlayerId, battleId, battleSeed,
                participants, List.of(), characterIds, controllers);
    }

    private Launch register(
            EncounterDefinition encounter,
            RewardTableDefinition rewardTable,
            DefinitionRepository.Snapshot snapshot,
            UUID ownerPlayerId,
            UUID battleId,
            long battleSeed,
            List<BattleParticipant> participants,
            List<EntityParticipantBinding> bindings,
            Map<String, String> characterIds,
            Map<String, UUID> controllers
    ) {
        BattleInstance battle = new BattleInstance(battleId, battleSeed, participants);
        BattleDefinitionContext definitionContext = new BattleDefinitionContext(snapshot.registry(), snapshot.hash(), characterIds);
        BattleRewardContext rewardContext = new BattleRewardContext(
                ownerPlayerId, rewardTable, rewardSeed(battleSeed, ownerPlayerId, encounter.id()));
        battles.register(battle, bindings, participants, definitionContext, rewardContext, controllers);
        battle.start();
        publishAuthoredEnemyIntents(battle, definitionContext);
        return new Launch(encounter, battle, definitionContext, rewardContext);
    }

    private static void addPlayer(
            DefinitionRegistry registry,
            List<BattleParticipant> participants,
            Map<String, String> characterIds,
            Map<String, UUID> controllers,
            String participantId,
            CharacterProgress progress,
            UUID ownerPlayerId,
            int ordinal
    ) {
        if (characterIds.putIfAbsent(participantId, progress.characterId()) != null) {
            throw new IllegalArgumentException("duplicate player participantId " + participantId);
        }
        CharacterDefinition definition = requireCharacter(registry, progress.characterId());
        ProgressionRules.requireMatches(definition, progress);
        CharacterDefinition.Stats stats = ProgressionRules.stats(definition, progress);
        participants.add(participant(participantId, BattleTeam.PLAYER, ordinal, stats));
        controllers.put(participantId, ownerPlayerId);
    }

    private static void addEnemy(
            DefinitionRegistry registry,
            List<BattleParticipant> participants,
            Map<String, String> characterIds,
            EncounterDefinition.EnemySlot authored,
            String participantId,
            int ordinal
    ) {
        if (characterIds.containsKey(participantId)) {
            throw new IllegalArgumentException("participant id collision " + participantId);
        }
        CharacterDefinition definition = requireCharacter(registry, authored.character());
        CharacterProgress progress = new CharacterProgress(
                definition.id(), definition.originStar(), authored.currentStar(), authored.level());
        CharacterDefinition.Stats stats = ProgressionRules.stats(definition, progress);
        characterIds.put(participantId, definition.id());
        participants.add(participant(participantId, BattleTeam.ENEMY, ordinal, stats));
    }

    private static EncounterDefinition requireEncounter(DefinitionRegistry registry, String encounterId) {
        EncounterDefinition encounter = registry.encounters().get(encounterId);
        if (encounter == null) throw new IllegalArgumentException("unknown authored encounter " + encounterId);
        return encounter;
    }

    private static RewardTableDefinition requireReward(DefinitionRegistry registry, EncounterDefinition encounter) {
        RewardTableDefinition rewardTable = registry.rewards().get(encounter.rewardTable());
        if (rewardTable == null) {
            throw new IllegalStateException("validated encounter reward table disappeared: " + encounter.rewardTable());
        }
        return rewardTable;
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

    public static void publishAuthoredEnemyIntents(BattleInstance battle, BattleDefinitionContext context) {
        for (String participantId : battle.actorOrder()) {
            if (battle.participant(participantId).team() != BattleTeam.ENEMY || !battle.combatState(participantId).alive()) continue;
            publishAuthoredEnemyIntent(battle, context, participantId);
        }
    }

    public static void publishAuthoredEnemyIntent(
            BattleInstance battle,
            BattleDefinitionContext context,
            String participantId
    ) {
        if (battle.participant(participantId).team() != BattleTeam.ENEMY || !battle.combatState(participantId).alive()) return;
        CharacterDefinition enemy = requireCharacter(context.definitions(), context.characterId(participantId));
        ActionDefinition basic = context.definitions().actions().get(enemy.basicAction());
        if (basic == null) throw new IllegalStateException("enemy basic action missing from battle snapshot: " + enemy.basicAction());
        EnemyIntent.Targeting targeting = "MULTI".equals(basic.targeting().shape())
                ? EnemyIntent.Targeting.ALL : EnemyIntent.Targeting.SINGLE;
        battle.setEnemyIntent(participantId, new EnemyIntent(
                basic.id(), EnemyIntent.Type.ATTACK, targeting, EnemyIntent.Risk.NORMAL, true, null));
    }

    private static long rewardSeed(long battleSeed, UUID ownerPlayerId, String encounterId) {
        long mixed = battleSeed ^ ownerPlayerId.getMostSignificantBits() ^ Long.rotateLeft(ownerPlayerId.getLeastSignificantBits(), 23);
        mixed ^= ((long) encounterId.hashCode() << 32) ^ Integer.toUnsignedLong(encounterId.hashCode());
        return Long.rotateLeft(mixed ^ 0x9E3779B97F4A7C15L, 17);
    }
}
