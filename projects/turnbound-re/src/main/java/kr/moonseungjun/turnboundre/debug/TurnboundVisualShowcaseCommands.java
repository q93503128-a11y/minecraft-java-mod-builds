package kr.moonseungjun.turnboundre.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import kr.moonseungjun.turnboundre.battle.BattleCommand;
import kr.moonseungjun.turnboundre.battle.BattleDefinitionContext;
import kr.moonseungjun.turnboundre.battle.BattleEvent;
import kr.moonseungjun.turnboundre.battle.BattleInstance;
import kr.moonseungjun.turnboundre.battle.BattleManager;
import kr.moonseungjun.turnboundre.data.DefinitionRepository;
import kr.moonseungjun.turnboundre.network.BattleNetworkGateway;
import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

/** Operator-only one-command visual audit for the representative Skeleton/Enderman presentation gate. */
public final class TurnboundVisualShowcaseCommands {
    private TurnboundVisualShowcaseCommands() {}

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            BattleManager battles,
            DefinitionRepository definitions
    ) {
        if (battles == null) throw new IllegalArgumentException("battles must not be null");
        if (definitions == null) throw new IllegalArgumentException("definitions must not be null");

        dispatcher.register(Commands.literal("turnbound_re_showcase")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> run(ctx.getSource(), battles, definitions))
                .then(Commands.literal("run")
                        .executes(ctx -> run(ctx.getSource(), battles, definitions)))
                .then(Commands.literal("cleanup")
                        .executes(ctx -> cleanup(ctx.getSource(), battles))));
    }

    private static int run(
            CommandSourceStack source,
            BattleManager battles,
            DefinitionRepository definitions
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BattleInstance existing = battleForPlayer(battles, player.getUUID());
        if (existing != null) {
            source.sendFailure(Component.literal(
                    "A battle is already active for this player. Finish or clean it up before the showcase."));
            return 0;
        }

        DefinitionRepository.Snapshot snapshot = definitions.snapshot();
        DebugVisualShowcaseScenario.Scenario scenario;
        try {
            scenario = DebugVisualShowcaseScenario.create(snapshot.registry(), snapshot.hash(), player.getUUID());
        } catch (RuntimeException error) {
            source.sendFailure(Component.literal("Showcase setup failed: " + error.getMessage()));
            return 0;
        }

        UUID battleId = UUID.randomUUID();
        BattleInstance battle = new BattleInstance(
                battleId,
                battleId.getMostSignificantBits() ^ battleId.getLeastSignificantBits(),
                scenario.participants());
        boolean registered = false;
        try {
            DebugVisualShowcaseScenario.prepareBattle(battle, scenario);
            battles.register(
                    battle,
                    List.of(),
                    scenario.participants(),
                    scenario.context(),
                    null,
                    scenario.controllers());
            registered = true;
            battle.start();

            BattleNetworkGateway gateway = new BattleNetworkGateway(battles);
            for (DebugVisualShowcaseScenario.BurstPlan plan : scenario.bursts()) {
                if (!plan.actorId().equals(battle.currentActorId())) {
                    throw new IllegalStateException(
                            "showcase initiative changed; expected=" + plan.actorId()
                                    + " actual=" + battle.currentActorId());
                }
                BattleCommand command = new BattleCommand(
                        battle.revision(),
                        plan.actorId(),
                        plan.actionId(),
                        "showcase-" + UUID.randomUUID(),
                        plan.targetIds());
                BattleNetworkPayloads.DecodedCommand decoded = BattleNetworkPayloads.BattleCommandC2S
                        .of(battle.battleId(), command)
                        .decode();
                BattleNetworkGateway.Result result = gateway.submit(player.getUUID(), decoded);
                if (!result.accepted()) {
                    throw new IllegalStateException(
                            plan.actionId() + " rejected: " + result.code() + " / " + result.detail());
                }
            }
        } catch (RuntimeException error) {
            if (registered) battles.cleanup(battleId);
            source.sendFailure(Component.literal("Showcase execution failed: " + error.getMessage()));
            return 0;
        }

        sync(player, battle, battles, 0);
        source.sendSuccess(() -> Component.literal(
                "Showcase queued: Horizon Break -> Arrow Storm. "
                        + "Use /turnbound_re_showcase cleanup after the visual audit."), false);
        return 1;
    }

    private static int cleanup(CommandSourceStack source, BattleManager battles) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BattleInstance battle = battles.battleForController(player.getUUID()).orElse(null);
        if (battle == null || !isShowcaseBattle(battle, battles)) {
            source.sendFailure(Component.literal("No visual showcase battle is active for this player."));
            return 0;
        }

        PacketDistributor.sendToPlayer(player, BattleNetworkPayloads.BattleEventsS2C.from(
                battle.battleId(),
                battle.revision(),
                -1,
                List.of(new BattleEvent(
                        battle.revision(),
                        DebugBattleClientState.CLEAR_EVENT_TYPE,
                        "",
                        "visual_showcase_cleanup"))));
        battles.cleanup(battle.battleId());
        source.sendSuccess(() -> Component.literal("Visual showcase cleared."), false);
        return 1;
    }

    private static BattleInstance battleForPlayer(BattleManager battles, UUID playerId) {
        BattleInstance bound = battles.battleForEntity(playerId).orElse(null);
        return bound != null ? bound : battles.battleForController(playerId).orElse(null);
    }

    private static boolean isShowcaseBattle(BattleInstance battle, BattleManager battles) {
        if (!battles.bindings(battle.battleId()).isEmpty()) return false;
        BattleDefinitionContext context = battles.definitionContext(battle.battleId()).orElse(null);
        if (context == null) return false;
        return "turnbound_re:enderman".equals(
                context.characterId(DebugVisualShowcaseScenario.ENDERMAN_ACTOR_ID))
                && "turnbound_re:skeleton".equals(
                context.characterId(DebugVisualShowcaseScenario.SKELETON_ACTOR_ID));
    }

    private static void sync(
            ServerPlayer player,
            BattleInstance battle,
            BattleManager battles,
            int fromIndex
    ) {
        List<BattleEvent> all = battle.eventLog();
        int start = Math.max(0, Math.min(fromIndex, all.size()));
        PacketDistributor.sendToPlayer(player, BattleNetworkPayloads.BattleEventsS2C.from(
                battle.battleId(),
                battle.revision(),
                start,
                List.copyOf(all.subList(start, all.size()))));
        BattleDefinitionContext definitions = battles.definitionContext(battle.battleId()).orElse(null);
        PacketDistributor.sendToPlayer(player, BattleNetworkPayloads.BattleSnapshotS2C.from(
                battle,
                definitions,
                battles));
    }
}
