package io.github.q93503128.turnbound;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.q93503128.turnbound.combat.BattleState;
import io.github.q93503128.turnbound.combat.BattleStats;
import io.github.q93503128.turnbound.combat.CombatantDefinition;
import io.github.q93503128.turnbound.combat.CombatantSide;
import io.github.q93503128.turnbound.combat.CombatantState;
import io.github.q93503128.turnbound.combat.SkillDefinition;
import io.github.q93503128.turnbound.combat.SkillEffect;
import io.github.q93503128.turnbound.combat.TargetRule;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * CI-RUNNER FIXTURE ONLY; never commit to src/fabric or package in production.
 * Injected into a freshly checked-out workspace immediately before runServer.
 * The synthetic defeat is server-owned, never triggered by a player/client packet.
 */
public final class TurnboundDefeatProbeInitializer implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
                dispatcher.register(literal("tb_ci_server_defeat")
                        .requires(source -> source.hasPermissionLevel(4))
                        .then(argument("player", StringArgumentType.word())
                                .executes(context -> {
                                    var source = context.getSource();
                                    var server = source.getServer();
                                    if (!TurnboundWorldGate.active(server)) {
                                        throw new IllegalStateException("CI probe refused an inactive server");
                                    }
                                    String name = StringArgumentType.getString(context, "player");
                                    ServerPlayerEntity player = server.getPlayerManager().getPlayer(name);
                                    if (player == null) {
                                        throw new IllegalArgumentException("Missing synthetic test player");
                                    }
                                    var basic = new SkillDefinition("basic", "Basic",
                                            TargetRule.ENEMY_SINGLE, 0,
                                            List.of(SkillEffect.damage(1.0)));
                                    var hero = new CombatantState("hero",
                                            new CombatantDefinition("hero", "Hero",
                                                    new BattleStats(100, 20, 10, 150),
                                                    "basic", List.of(basic)),
                                            CombatantSide.ALLY, 0);
                                    var enemy = new CombatantState("enemy",
                                            new CombatantDefinition("enemy", "Enemy",
                                                    new BattleStats(100, 10, 10, 100),
                                                    "basic", List.of(basic)),
                                            CombatantSide.ENEMY, 0);
                                    BattleState battle = new BattleState(List.of(hero, enemy));
                                    TurnboundBattleBridge.open(
                                            server,
                                            player.getServerWorld().getRegistryKey().getValue().toString(),
                                            battle, Map.of(player.getUuid(), Set.of("hero")));
                                    // Trusted server fixture forces a terminal outcome after
                                    // the real session has been opened; production bridge
                                    // must settle it in its NEXT actual server tick.
                                    hero.forceDown();
                                    source.sendFeedback(
                                            () -> Text.literal("CI synthetic battle entered ENEMY_VICTORY"),
                                            false);
                                    return 1;
                                }))));
    }
}
