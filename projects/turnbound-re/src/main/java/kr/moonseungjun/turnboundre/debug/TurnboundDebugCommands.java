package kr.moonseungjun.turnboundre.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import kr.moonseungjun.turnboundre.battle.BattleActionExecutor;
import kr.moonseungjun.turnboundre.battle.BattleCommand;
import kr.moonseungjun.turnboundre.battle.BattleDefinitionContext;
import kr.moonseungjun.turnboundre.battle.BattleEvent;
import kr.moonseungjun.turnboundre.battle.BattleInstance;
import kr.moonseungjun.turnboundre.battle.BattleManager;
import kr.moonseungjun.turnboundre.battle.BattleParticipant;
import kr.moonseungjun.turnboundre.battle.BattleState;
import kr.moonseungjun.turnboundre.battle.BattleTeam;
import kr.moonseungjun.turnboundre.battle.EnemyIntent;
import kr.moonseungjun.turnboundre.battle.EntityParticipantBinding;
import kr.moonseungjun.turnboundre.data.ActionDefinition;
import kr.moonseungjun.turnboundre.data.CharacterDefinition;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.DefinitionRepository;
import kr.moonseungjun.turnboundre.network.BattleNetworkGateway;
import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DEBUG_ONLY operator harness. It deliberately uses production definitions and production command/effect execution,
 * while keeping presentation and world setup non-production.
 */
public final class TurnboundDebugCommands {
    private static final String PLAYER_ID = "debug_player";
    private static final String ENEMY_ID = "debug_enemy";
    private static final String DEFAULT_PLAYER_CHARACTER = "turnbound_re:zombie";

    private TurnboundDebugCommands() {}

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            BattleManager battles,
            DefinitionRepository definitions
    ) {
        if (battles == null) throw new IllegalArgumentException("battles must not be null");
        if (definitions == null) throw new IllegalArgumentException("definitions must not be null");
        dispatcher.register(Commands.literal("turnbound_re")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("contracts")
                        .executes(ctx -> {
                            var snapshot = definitions.snapshot();
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "TURNBOUND: RE M3 data contracts active generation=" + snapshot.generation()
                                            + " hash=" + shortHash(snapshot.hash())), false);
                            return 1;
                        }))
                .then(Commands.literal("debug_status")
                        .executes(ctx -> debugStatus(ctx.getSource(), battles)))
                .then(Commands.literal("debug_cleanup")
                        .executes(ctx -> debugCleanup(ctx.getSource(), battles)))
                .then(Commands.literal("debug_encounter")
                        .then(Commands.argument("enemy", EntityArgument.entity())
                                .executes(ctx -> startDebugEncounter(
                                        ctx.getSource(), battles, definitions,
                                        EntityArgument.getEntity(ctx, "enemy"), DEFAULT_PLAYER_CHARACTER))
                                .then(Commands.argument("player_character", StringArgumentType.word())
                                        .executes(ctx -> startDebugEncounter(
                                                ctx.getSource(), battles, definitions,
                                                EntityArgument.getEntity(ctx, "enemy"),
                                                StringArgumentType.getString(ctx, "player_character"))))))
                .then(Commands.literal("debug_basic")
                        .executes(ctx -> debugPlayerAction(ctx.getSource(), battles, ActionSlot.BASIC)))
                .then(Commands.literal("debug_skill1")
                        .executes(ctx -> debugPlayerAction(ctx.getSource(), battles, ActionSlot.SKILL_1)))
                .then(Commands.literal("debug_skill2")
                        .executes(ctx -> debugPlayerAction(ctx.getSource(), battles, ActionSlot.SKILL_2)))
                .then(Commands.literal("debug_burst")
                        .executes(ctx -> debugPlayerAction(ctx.getSource(), battles, ActionSlot.BURST)))
                .then(Commands.literal("debug_enemy")
                        .executes(ctx -> debugEnemy(ctx.getSource(), battles))));
    }

    private static int startDebugEncounter(
            CommandSourceStack source,
            BattleManager battles,
            DefinitionRepository definitions,
            Entity enemyEntity,
            String playerCharacterId
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!(enemyEntity instanceof Mob enemy)) {
            source.sendFailure(Component.literal("DEBUG_ONLY encounter target must be a Mob"));
            return 0;
        }
        if (battles.battleForEntity(player.getUUID()).isPresent()) {
            source.sendFailure(Component.literal("DEBUG_ONLY player already belongs to a live battle; use debug_cleanup first"));
            return 0;
        }
        if (battles.battleForEntity(enemy.getUUID()).isPresent()) {
            source.sendFailure(Component.literal("DEBUG_ONLY target mob already belongs to a live battle"));
            return 0;
        }

        DefinitionRepository.Snapshot snapshot = definitions.snapshot();
        DefinitionRegistry registry = snapshot.registry();
        CharacterDefinition playerDefinition = registry.characters().get(playerCharacterId);
        if (playerDefinition == null) {
            source.sendFailure(Component.literal("DEBUG_ONLY unknown player CharacterDefinition: " + playerCharacterId));
            return 0;
        }

        var entityKey = BuiltInRegistries.ENTITY_TYPE.getKey(enemy.getType());
        if (entityKey == null) {
            source.sendFailure(Component.literal("DEBUG_ONLY target Mob has no registered EntityType id"));
            return 0;
        }
        String sourceEntityId = entityKey.toString();
        List<CharacterDefinition> enemyMatches = registry.characters().values().stream()
                .filter(character -> sourceEntityId.equals(character.sourceEntity()))
                .toList();
        if (enemyMatches.isEmpty()) {
            source.sendFailure(Component.literal(
                    "DEBUG_ONLY no loaded CharacterDefinition maps sourceEntity=" + sourceEntityId));
            return 0;
        }
        if (enemyMatches.size() != 1) {
            source.sendFailure(Component.literal(
                    "DEBUG_ONLY ambiguous CharacterDefinitions for sourceEntity=" + sourceEntityId
                            + " count=" + enemyMatches.size()));
            return 0;
        }
        CharacterDefinition enemyDefinition = enemyMatches.getFirst();

        List<BattleParticipant> participants = List.of(
                participant(PLAYER_ID, BattleTeam.PLAYER, 0, playerDefinition),
                participant(ENEMY_ID, BattleTeam.ENEMY, 1, enemyDefinition));
        UUID battleId = UUID.randomUUID();
        BattleInstance battle = new BattleInstance(
                battleId,
                battleId.getMostSignificantBits() ^ battleId.getLeastSignificantBits(),
                participants);
        BattleDefinitionContext definitionContext = new BattleDefinitionContext(
                registry,
                snapshot.hash(),
                Map.of(PLAYER_ID, playerDefinition.id(), ENEMY_ID, enemyDefinition.id()));

        battles.register(battle, List.of(
                new EntityParticipantBinding(PLAYER_ID, player.getUUID()),
                new EntityParticipantBinding(ENEMY_ID, enemy.getUUID())), participants, definitionContext);
        battle.start();
        publishDataBasicIntent(battle, definitionContext);
        syncDebugState(player, battle, battles, 0);

        source.sendSuccess(() -> Component.literal(
                "DEBUG_ONLY data encounter started battle=" + battleId
                        + " player=" + playerDefinition.id()
                        + " enemy=" + enemyDefinition.id()
                        + " sourceEntity=" + sourceEntityId
                        + " hash=" + shortHash(snapshot.hash())
                        + " actor=" + battle.currentActorId()
                        + " rev=" + battle.revision()), false);
        return 1;
    }

    private static int debugPlayerAction(
            CommandSourceStack source,
            BattleManager battles,
            ActionSlot slot
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BattleInstance battle = requirePlayerBattle(source, battles, player);
        if (battle == null) return 0;
        BattleDefinitionContext context = battles.definitionContext(battle.battleId()).orElse(null);
        if (context == null) {
            source.sendFailure(Component.literal("DEBUG_ONLY battle has no data definition context"));
            return 0;
        }
        CharacterDefinition character = context.definitions().characters().get(context.characterId(PLAYER_ID));
        if (character == null) {
            source.sendFailure(Component.literal("DEBUG_ONLY player CharacterDefinition is missing from battle snapshot"));
            return 0;
        }

        String actionId = actionId(character, slot);
        if (actionId == null) {
            source.sendFailure(Component.literal("DEBUG_ONLY " + character.id() + " has no " + slot.label));
            return 0;
        }
        ActionDefinition action = context.definitions().actions().get(actionId);
        if (action == null) {
            source.sendFailure(Component.literal("DEBUG_ONLY action disappeared from battle snapshot: " + actionId));
            return 0;
        }
        if (action.targeting().count() != 1) {
            source.sendFailure(Component.literal(
                    "DEBUG_ONLY 1v1 harness cannot execute " + actionId + " because it requires "
                            + action.targeting().count() + " targets"));
            return 0;
        }
        String targetId = switch (action.targeting().team()) {
            case "SELF", "ALLY" -> PLAYER_ID;
            case "ENEMY", "ANY" -> ENEMY_ID;
            default -> null;
        };
        if (targetId == null) {
            source.sendFailure(Component.literal("DEBUG_ONLY unsupported target rule: " + action.targeting().team()));
            return 0;
        }

        BattleCommand command = new BattleCommand(
                battle.revision(), PLAYER_ID, actionId, "debug-" + UUID.randomUUID(), List.of(targetId));
        BattleNetworkPayloads.DecodedCommand decoded = BattleNetworkPayloads.BattleCommandC2S
                .of(battle.battleId(), command).decode();
        BattleNetworkGateway.Result result = new BattleNetworkGateway(battles).submit(player.getUUID(), decoded);
        if (!result.accepted()) {
            sendRejection(player, battle, result, battles);
            source.sendFailure(Component.literal(
                    "DEBUG_ONLY " + slot.label + " rejected: " + result.code() + " / " + result.detail()));
            return 0;
        }

        syncDebugState(player, battle, battles, result.eventStartIndex());
        source.sendSuccess(() -> Component.literal(debugBattleLine(battle, battles)), false);
        return 1;
    }

    private static int debugEnemy(CommandSourceStack source, BattleManager battles) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BattleInstance battle = requirePlayerBattle(source, battles, player);
        if (battle == null) return 0;
        if (battle.state() != BattleState.RESOLVING || !ENEMY_ID.equals(battle.currentActorId())) {
            source.sendFailure(Component.literal("DEBUG_ONLY enemy step requires enemy RESOLVING; current="
                    + battle.state() + "/" + battle.currentActorId()));
            return 0;
        }
        BattleDefinitionContext context = battles.definitionContext(battle.battleId()).orElse(null);
        if (context == null) {
            source.sendFailure(Component.literal("DEBUG_ONLY battle has no data definition context"));
            return 0;
        }

        int fromIndex = battle.eventLog().size();
        String intentAction = battle.enemyIntent(ENEMY_ID).actionId();
        battle.resolveEnemyStub();
        if (!"recover".equals(intentAction)) {
            ActionDefinition action = context.definitions().actions().get(intentAction);
            if (action == null) {
                source.sendFailure(Component.literal("DEBUG_ONLY enemy Intent action missing from battle snapshot: " + intentAction));
                return 0;
            }
            if (action.targeting().count() != 1) {
                source.sendFailure(Component.literal("DEBUG_ONLY 1v1 enemy harness cannot resolve multi-target action " + intentAction));
                return 0;
            }
            BattleActionExecutor executor = new BattleActionExecutor(
                    context.definitions(), (battleId, participantId) -> context.characterId(participantId));
            executor.execute(battle, ENEMY_ID, action, List.of(PLAYER_ID));
        }
        battle.finishResolution();
        if (battle.outcome() == BattleInstance.Outcome.ONGOING && battle.combatState(ENEMY_ID).alive()) {
            publishDataBasicIntent(battle, context);
        }
        syncDebugState(player, battle, battles, fromIndex);
        source.sendSuccess(() -> Component.literal(debugBattleLine(battle, battles)), false);
        return 1;
    }

    private static int debugCleanup(CommandSourceStack source, BattleManager battles) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BattleInstance battle = battles.battleForEntity(player.getUUID()).orElse(null);
        if (battle == null) {
            source.sendSuccess(() -> Component.literal("DEBUG_ONLY no live battle; active=" + battles.activeBattleCount()
                    + " bound=" + battles.boundEntityCount()), false);
            return 1;
        }

        UUID battleId = battle.battleId();
        PacketDistributor.sendToPlayer(player, BattleNetworkPayloads.BattleEventsS2C.from(
                battleId,
                battle.revision(),
                -1,
                List.of(new BattleEvent(battle.revision(), DebugBattleClientState.CLEAR_EVENT_TYPE, "", "debug_cleanup"))));
        if (battle.state() == BattleState.REWARD) battle.cleanup();
        battles.cleanup(battleId);
        source.sendSuccess(() -> Component.literal("DEBUG_ONLY cleanup battle=" + battleId
                + " active=" + battles.activeBattleCount() + " bound=" + battles.boundEntityCount()), false);
        return 1;
    }

    private static int debugStatus(CommandSourceStack source, BattleManager battles) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BattleInstance battle = battles.battleForEntity(player.getUUID()).orElse(null);
        String own = battle == null ? "none" : debugBattleLine(battle, battles);
        source.sendSuccess(() -> Component.literal("DEBUG_ONLY active=" + battles.activeBattleCount()
                + " bound=" + battles.boundEntityCount() + " own=" + own), false);
        return 1;
    }

    private static BattleParticipant participant(
            String participantId,
            BattleTeam team,
            int ordinal,
            CharacterDefinition definition
    ) {
        CharacterDefinition.Stats stats = definition.baseStats();
        return new BattleParticipant(
                participantId, team, ordinal, stats.spd(), stats.hp(), stats.atk(), stats.def(), stats.poise());
    }

    private static void publishDataBasicIntent(BattleInstance battle, BattleDefinitionContext context) {
        CharacterDefinition enemy = context.definitions().characters().get(context.characterId(ENEMY_ID));
        if (enemy == null) throw new IllegalStateException("debug enemy CharacterDefinition missing from snapshot");
        ActionDefinition basic = context.definitions().actions().get(enemy.basicAction());
        if (basic == null) throw new IllegalStateException("debug enemy basic action missing from battle snapshot: " + enemy.basicAction());
        EnemyIntent.Targeting targeting = "MULTI".equals(basic.targeting().shape())
                ? EnemyIntent.Targeting.ALL : EnemyIntent.Targeting.SINGLE;
        battle.setEnemyIntent(ENEMY_ID, new EnemyIntent(
                basic.id(), EnemyIntent.Type.ATTACK, targeting, EnemyIntent.Risk.NORMAL, true, null));
    }

    private static String actionId(CharacterDefinition character, ActionSlot slot) {
        return switch (slot) {
            case BASIC -> character.basicAction();
            case SKILL_1 -> character.skills().isEmpty() ? null : character.skills().get(0);
            case SKILL_2 -> character.skills().size() < 2 ? null : character.skills().get(1);
            case BURST -> character.burst();
        };
    }

    private static void sendRejection(
            ServerPlayer player,
            BattleInstance battle,
            BattleNetworkGateway.Result result,
            BattleManager battles
    ) {
        PacketDistributor.sendToPlayer(player, BattleNetworkPayloads.BattleEventsS2C.rejection(
                battle.battleId(), battle.revision(), result.code().name() + ":" + result.detail()));
        BattleDefinitionContext definitions = battles.definitionContext(battle.battleId()).orElse(null);
        PacketDistributor.sendToPlayer(player, BattleNetworkPayloads.BattleSnapshotS2C.from(
                battle, definitions, battles));
    }

    private static BattleInstance requirePlayerBattle(CommandSourceStack source, BattleManager battles, ServerPlayer player) {
        BattleInstance battle = battles.battleForEntity(player.getUUID()).orElse(null);
        if (battle == null) source.sendFailure(Component.literal("DEBUG_ONLY no live battle; start debug_encounter first"));
        return battle;
    }

    private static void syncDebugState(
            ServerPlayer player,
            BattleInstance battle,
            BattleManager battles,
            int fromIndex
    ) {
        List<BattleEvent> all = battle.eventLog();
        int start = Math.max(0, Math.min(fromIndex, all.size()));
        List<BattleEvent> events = List.copyOf(all.subList(start, all.size()));
        PacketDistributor.sendToPlayer(player, BattleNetworkPayloads.BattleEventsS2C.from(
                battle.battleId(), battle.revision(), start, events));
        BattleDefinitionContext definitions = battles.definitionContext(battle.battleId()).orElse(null);
        PacketDistributor.sendToPlayer(player, BattleNetworkPayloads.BattleSnapshotS2C.from(
                battle, definitions, battles));
    }

    private static String debugBattleLine(BattleInstance battle, BattleManager battles) {
        BattleDefinitionContext context = battles.definitionContext(battle.battleId()).orElse(null);
        String definitions = context == null ? "no-data-context" : "hash=" + shortHash(context.definitionHash())
                + " pChar=" + context.characterId(PLAYER_ID) + " eChar=" + context.characterId(ENEMY_ID);
        return "battle=" + battle.battleId() + " state=" + battle.state() + " actor=" + battle.currentActorId()
                + " rev=" + battle.revision() + " pHP=" + battle.combatState(PLAYER_ID).hp()
                + " pEnergy=" + battle.combatState(PLAYER_ID).energy()
                + " eHP=" + battle.combatState(ENEMY_ID).hp() + " ePoise=" + battle.combatState(ENEMY_ID).poise()
                + " " + definitions;
    }

    private static String shortHash(String hash) {
        return hash == null || hash.length() < 12 ? String.valueOf(hash) : hash.substring(0, 12);
    }

    private enum ActionSlot {
        BASIC("basic"), SKILL_1("skill1"), SKILL_2("skill2"), BURST("burst");
        private final String label;
        ActionSlot(String label) { this.label = label; }
    }
}
