package kr.moonseungjun.turnboundre.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import kr.moonseungjun.turnboundre.battle.AffinityGrade;
import kr.moonseungjun.turnboundre.battle.BattleCommand;
import kr.moonseungjun.turnboundre.battle.BattleEvent;
import kr.moonseungjun.turnboundre.battle.BattleInstance;
import kr.moonseungjun.turnboundre.battle.BattleManager;
import kr.moonseungjun.turnboundre.battle.BattleParticipant;
import kr.moonseungjun.turnboundre.battle.BattleState;
import kr.moonseungjun.turnboundre.battle.BattleTeam;
import kr.moonseungjun.turnboundre.battle.DamageService;
import kr.moonseungjun.turnboundre.battle.DamageTag;
import kr.moonseungjun.turnboundre.battle.EntityParticipantBinding;
import kr.moonseungjun.turnboundre.network.BattleNetworkGateway;
import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

/**
 * DEBUG_ONLY operator harness. Numbers and presentation here are test fixtures, not production balance or UI.
 */
public final class TurnboundDebugCommands {
    private static final String PLAYER_ID = "debug_player";
    private static final String ENEMY_ID = "debug_enemy";

    private TurnboundDebugCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, BattleManager battles) {
        if (battles == null) throw new IllegalArgumentException("battles must not be null");
        dispatcher.register(Commands.literal("turnbound_re")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("contracts")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("TURNBOUND: RE M2 contracts active"), false);
                            return 1;
                        }))
                .then(Commands.literal("debug_status")
                        .executes(ctx -> debugStatus(ctx.getSource(), battles)))
                .then(Commands.literal("debug_cleanup")
                        .executes(ctx -> debugCleanup(ctx.getSource(), battles)))
                .then(Commands.literal("debug_encounter")
                        .then(Commands.argument("enemy", EntityArgument.entity())
                                .executes(ctx -> startDebugEncounter(
                                        ctx.getSource(), battles, EntityArgument.getEntity(ctx, "enemy")))))
                .then(Commands.literal("debug_basic")
                        .executes(ctx -> debugBasic(ctx.getSource(), battles)))
                .then(Commands.literal("debug_enemy")
                        .executes(ctx -> debugEnemy(ctx.getSource(), battles))));
    }

    private static int startDebugEncounter(CommandSourceStack source, BattleManager battles, Entity enemy) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!(enemy instanceof Mob)) {
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

        List<BattleParticipant> participants = List.of(
                new BattleParticipant(PLAYER_ID, BattleTeam.PLAYER, 0, 20, 120, 24, 10, 50),
                new BattleParticipant(ENEMY_ID, BattleTeam.ENEMY, 1, 10, 100, 18, 8, 40));
        UUID battleId = UUID.randomUUID();
        BattleInstance battle = new BattleInstance(battleId, battleId.getMostSignificantBits() ^ battleId.getLeastSignificantBits(), participants);
        battles.register(battle, List.of(
                new EntityParticipantBinding(PLAYER_ID, player.getUUID()),
                new EntityParticipantBinding(ENEMY_ID, enemy.getUUID())), participants);
        battle.start();
        syncDebugState(player, battle, 0);

        source.sendSuccess(() -> Component.literal(
                "DEBUG_ONLY encounter started battle=" + battleId
                        + " enemy=" + enemy.getName().getString()
                        + " actor=" + battle.currentActorId()
                        + " rev=" + battle.revision()), false);
        return 1;
    }

    private static int debugBasic(CommandSourceStack source, BattleManager battles) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BattleInstance battle = requirePlayerBattle(source, battles, player);
        if (battle == null) return 0;

        BattleCommand command = new BattleCommand(
                battle.revision(), PLAYER_ID, "basic", "debug-" + UUID.randomUUID(), List.of(ENEMY_ID));
        BattleNetworkPayloads.DecodedCommand decoded = BattleNetworkPayloads.BattleCommandC2S.of(battle.battleId(), command).decode();
        BattleNetworkGateway.Result result = new BattleNetworkGateway(battles).submit(player.getUUID(), decoded);
        if (!result.accepted()) {
            PacketDistributor.sendToPlayer(player, BattleNetworkPayloads.BattleEventsS2C.rejection(
                    battle.battleId(), battle.revision(), result.code().name() + ":" + result.detail()));
            PacketDistributor.sendToPlayer(player, BattleNetworkPayloads.BattleSnapshotS2C.from(battle));
            source.sendFailure(Component.literal("DEBUG_ONLY basic rejected: " + result.code() + " / " + result.detail()));
            return 0;
        }

        battle.resolveDamage(PLAYER_ID, ENEMY_ID, DamageService.DamageRequest.standard(
                DamageTag.MELEE, AffinityGrade.NORMAL, 90, 18, 24, 8, false));
        battle.finishResolution();
        syncDebugState(player, battle, result.eventStartIndex());
        source.sendSuccess(() -> Component.literal(debugBattleLine(battle)), false);
        return 1;
    }

    private static int debugEnemy(CommandSourceStack source, BattleManager battles) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BattleInstance battle = requirePlayerBattle(source, battles, player);
        if (battle == null) return 0;
        if (battle.state() != BattleState.RESOLVING || !ENEMY_ID.equals(battle.currentActorId())) {
            source.sendFailure(Component.literal("DEBUG_ONLY enemy step requires the enemy RESOLVING turn; current="
                    + battle.state() + "/" + battle.currentActorId()));
            return 0;
        }

        int fromIndex = battle.eventLog().size();
        String intentAction = battle.enemyIntent(ENEMY_ID).actionId();
        battle.resolveEnemyStub();
        if (!"recover".equals(intentAction)) {
            battle.resolveDamage(ENEMY_ID, PLAYER_ID, DamageService.DamageRequest.standard(
                    DamageTag.MELEE, AffinityGrade.NORMAL, 75, 12, 18, 10, false));
        }
        battle.finishResolution();
        syncDebugState(player, battle, fromIndex);
        source.sendSuccess(() -> Component.literal(debugBattleLine(battle)), false);
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
        String own = battle == null ? "none" : debugBattleLine(battle);
        source.sendSuccess(() -> Component.literal("DEBUG_ONLY active=" + battles.activeBattleCount()
                + " bound=" + battles.boundEntityCount() + " own=" + own), false);
        return 1;
    }

    private static BattleInstance requirePlayerBattle(CommandSourceStack source, BattleManager battles, ServerPlayer player) {
        BattleInstance battle = battles.battleForEntity(player.getUUID()).orElse(null);
        if (battle == null) source.sendFailure(Component.literal("DEBUG_ONLY no live battle; start debug_encounter first"));
        return battle;
    }

    private static void syncDebugState(ServerPlayer player, BattleInstance battle, int fromIndex) {
        List<BattleEvent> all = battle.eventLog();
        int start = Math.max(0, Math.min(fromIndex, all.size()));
        List<BattleEvent> events = List.copyOf(all.subList(start, all.size()));
        PacketDistributor.sendToPlayer(player, BattleNetworkPayloads.BattleEventsS2C.from(
                battle.battleId(), battle.revision(), start, events));
        PacketDistributor.sendToPlayer(player, BattleNetworkPayloads.BattleSnapshotS2C.from(battle));
    }

    private static String debugBattleLine(BattleInstance battle) {
        return "battle=" + battle.battleId() + " state=" + battle.state() + " actor=" + battle.currentActorId()
                + " rev=" + battle.revision() + " pHP=" + battle.combatState(PLAYER_ID).hp()
                + " eHP=" + battle.combatState(ENEMY_ID).hp() + " ePoise=" + battle.combatState(ENEMY_ID).poise();
    }
}
