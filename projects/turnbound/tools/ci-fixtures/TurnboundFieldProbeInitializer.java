package io.github.q93503128.turnbound;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.q93503128.turnbound.combat.BattleState;
import io.github.q93503128.turnbound.combat.BattleStats;
import io.github.q93503128.turnbound.combat.CombatantDefinition;
import io.github.q93503128.turnbound.combat.CombatantSide;
import io.github.q93503128.turnbound.combat.CombatantState;
import io.github.q93503128.turnbound.combat.FieldEncounterClaims;
import io.github.q93503128.turnbound.combat.SkillDefinition;
import io.github.q93503128.turnbound.combat.SkillEffect;
import io.github.q93503128.turnbound.combat.TargetRule;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * SYNTHETIC CI-ONLY: injected in runServer checkout; never included in product JAR.
 * Registration calls the exact production server-authorized entity entrypoint.
 * The zombie and all combatants are synthetic fixture data only.
 */
public final class TurnboundFieldProbeInitializer implements ModInitializer {
    private static final Map<UUID, CombatantState> TEST_HERO = new HashMap<>();
    private static ServerPlayerEntity player(net.minecraft.server.command.ServerCommandSource source,
                                             String name) {
        ServerPlayerEntity p = source.getServer().getPlayerManager().getPlayer(name);
        if (p == null || !TurnboundWorldGate.active(p.getServer()))
            throw new IllegalArgumentException("Synthetic field probe requires connected opted-in player");
        return p;
    }

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
            dispatcher.register(literal("tb_ci_field_register")
                    .requires(source -> source.hasPermissionLevel(4))
                    .then(argument("player", StringArgumentType.word())
                            .executes(context -> {
                                var source = context.getSource();
                                var target = player(source, StringArgumentType.getString(context,"player"));
                                var world = target.getServerWorld();
                                List<MobEntity> zombies = world.getEntitiesByClass(MobEntity.class,
                                        new Box(18, 69, 8, 23, 75, 13),
                                        mob -> mob.getType() == EntityType.ZOMBIE && mob.isAlive());
                                if (zombies.size() != 1)
                                    throw new IllegalStateException("Expected exactly one CI fixture zombie");
                                MobEntity actor = zombies.get(0);
                                boolean ok = TurnboundFieldEncounterService.authorize(
                                        actor, new FieldEncounterClaims.Tuning(8, 1.6, 16, 20, 50),
                                        0.2, contacted -> {
                                            var basic = new SkillDefinition("basic", "Basic",
                                                    TargetRule.ENEMY_SINGLE, 0,
                                                    List.of(SkillEffect.damage(1.0)));
                                            var hero = new CombatantState("hero",
                                                    new CombatantDefinition("hero", "Hero",
                                                            new BattleStats(100,20,10,160),
                                                            "basic",List.of(basic)),
                                                    CombatantSide.ALLY,0);
                                            var enemy = new CombatantState("enemy",
                                                    new CombatantDefinition("enemy", "Enemy",
                                                            new BattleStats(100,10,10,80),
                                                            "basic",List.of(basic)),
                                                    CombatantSide.ENEMY,0);
                                            TEST_HERO.put(contacted.getUuid(), hero);
                                            return new TurnboundFieldEncounterService.EncounterPlan(
                                                    new BattleState(List.of(hero, enemy)),
                                                    Map.of(contacted.getUuid(), Set.of("hero")));
                                        });
                                if (!ok) throw new IllegalStateException("CI field registration refused");
                                source.sendFeedback(() -> Text.literal("CI_FIELD_REGISTERED"), false);
                                return 1;
                            })));
            dispatcher.register(literal("tb_ci_field_defeat")
                    .requires(source -> source.hasPermissionLevel(4))
                    .then(argument("player", StringArgumentType.word())
                            .executes(context -> {
                                var source = context.getSource();
                                var target = player(source, StringArgumentType.getString(context,"player"));
                                CombatantState hero = TEST_HERO.get(target.getUuid());
                                if (hero == null || !TurnboundBattleBridge.inBattle(
                                        source.getServer(), target.getUuid())) {
                                    throw new IllegalStateException("Missing server-owned field session");
                                }
                                hero.forceDown();
                                source.sendFeedback(() -> Text.literal("CI_FIELD_DEFEAT_APPLIED"), false);
                                return 1;
                            })));
            dispatcher.register(literal("tb_ci_field_status")
                    .requires(source -> source.hasPermissionLevel(4))
                    .then(argument("player", StringArgumentType.word())
                            .executes(context -> {
                                var source = context.getSource();
                                var target = player(source, StringArgumentType.getString(context,"player"));
                                boolean inBattle = TurnboundBattleBridge.inBattle(source.getServer(),target.getUuid());
                                source.sendFeedback(() -> Text.literal(inBattle ? "CI_FIELD_BATTLE" : "CI_FIELD_FREE"),
                                        false);
                                return 1;
                            })));
        });
    }
}
