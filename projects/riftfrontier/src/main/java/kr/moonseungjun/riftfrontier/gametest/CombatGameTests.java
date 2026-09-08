package kr.moonseungjun.riftfrontier.gametest;

import kr.moonseungjun.riftfrontier.Riftfrontier;
import kr.moonseungjun.riftfrontier.combat.AttackTimeline;
import kr.moonseungjun.riftfrontier.combat.BossAttackSelectionPolicy;
import kr.moonseungjun.riftfrontier.combat.CombatRuntimeCatalog;
import kr.moonseungjun.riftfrontier.combat.MinecraftAttackAdapter;
import kr.moonseungjun.riftfrontier.combat.MinecraftBossCombatAdapter;
import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.ContentRegistry;
import kr.moonseungjun.riftfrontier.content.CoreDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;
import java.util.function.Consumer;

/** Native in-world combat regression tests for the M3 server attack adapters. */
public final class CombatGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(
        BuiltInRegistries.TEST_FUNCTION,
        Riftfrontier.MOD_ID
    );

    static {
        TEST_FUNCTIONS.register("attack_hit_window", () -> CombatGameTests::attackHitWindow);
        TEST_FUNCTIONS.register("boss_phase_damage_window", () -> CombatGameTests::bossPhaseDamageWindow);
    }

    private CombatGameTests() {}

    public static void register(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    private static void attackHitWindow(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos attackerPos = helper.absolutePos(new BlockPos(4, 3, 4));
        BlockPos targetPos = helper.absolutePos(new BlockPos(5, 3, 4));

        Zombie attacker = new Zombie(level);
        attacker.setNoAi(true);
        attacker.snapTo(attackerPos.getX() + 0.5D, attackerPos.getY(), attackerPos.getZ() + 0.5D, 0.0F, 0.0F);
        helper.assertTrue(level.addFreshEntity(attacker), "Technical attacker must enter the GameTest world");

        Zombie target = new Zombie(level);
        target.setNoAi(true);
        target.snapTo(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D, 0.0F, 0.0F);
        helper.assertTrue(level.addFreshEntity(target), "Technical target must enter the GameTest world");

        CoreDefinition.AttackPattern pattern = new CoreDefinition.AttackPattern(
            ContentId.rift("gametest/attack_hit_window"),
            "bounded_aabb",
            2,
            2,
            2,
            Set.of("step_out"),
            "technical_test_cue"
        );
        MinecraftAttackAdapter adapter = new MinecraftAttackAdapter(new MinecraftAttackAdapter.AabbHitVolume(2.0D, 1.5D), 3.0F);

        long start = level.getGameTime();
        float initialHealth = target.getHealth();
        adapter.begin(pattern, start);

        MinecraftAttackAdapter.TickResult telegraph = adapter.tick(level, attacker, start);
        helper.assertTrue(telegraph.phase() == AttackTimeline.Phase.TELEGRAPH, "Attack must begin in TELEGRAPH");
        helper.assertTrue(!telegraph.hitWindowOpen(), "Telegraph must never open the damage window");
        helper.assertTrue(target.getHealth() == initialHealth, "Telegraph must not damage the target");

        MinecraftAttackAdapter.TickResult active = adapter.tick(level, attacker, start + 2L);
        helper.assertTrue(active.phase() == AttackTimeline.Phase.ACTIVE, "Attack must enter ACTIVE at the authored boundary");
        helper.assertTrue(active.hitWindowOpen(), "ACTIVE must expose the shared authoritative hit window");
        helper.assertTrue(active.damagedCount() == 1, "ACTIVE must apply one real server damage event to the bounded target");
        float afterActive = target.getHealth();
        helper.assertTrue(afterActive < initialHealth, "ACTIVE must reduce target health through Minecraft damage handling");

        MinecraftAttackAdapter.TickResult activeAgain = adapter.tick(level, attacker, start + 3L);
        helper.assertTrue(activeAgain.hitWindowOpen(), "Second ACTIVE tick remains inside the authored hit window");
        helper.assertTrue(activeAgain.damagedCount() == 0, "One execution must not multi-hit the same target every server tick");
        helper.assertTrue(target.getHealth() == afterActive, "Duplicate ACTIVE sampling must not repeat damage to the same target");

        MinecraftAttackAdapter.TickResult recovery = adapter.tick(level, attacker, start + 4L);
        helper.assertTrue(recovery.phase() == AttackTimeline.Phase.RECOVERY, "Attack must enter RECOVERY at the authored boundary");
        helper.assertTrue(!recovery.hitWindowOpen(), "Recovery must close the damage window");
        helper.assertTrue(target.getHealth() == afterActive, "Recovery must not damage the target");

        MinecraftAttackAdapter.TickResult complete = adapter.tick(level, attacker, start + 6L);
        helper.assertTrue(complete.phase() == AttackTimeline.Phase.COMPLETE && complete.finished(), "Attack must complete exactly at total authored cadence");
        helper.assertTrue(!adapter.isExecuting(), "Completed execution must leave no active server attack state");
        helper.assertTrue(target.getHealth() == afterActive, "Completion must not apply a late hit");

        adapter.begin(pattern, start + 10L);
        helper.assertTrue(adapter.cancel(), "Explicit interruption must cancel an executing attack");
        MinecraftAttackAdapter.TickResult cancelled = adapter.tick(level, attacker, start + 12L);
        helper.assertTrue(!cancelled.hitWindowOpen(), "Cancelled attack must never reopen its former ACTIVE window");
        helper.assertTrue(target.getHealth() == afterActive, "Cancellation must prevent damage at the former active tick");

        helper.succeed();
    }

    private static void bossPhaseDamageWindow(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos bossPos = helper.absolutePos(new BlockPos(4, 3, 4));
        BlockPos targetPos = helper.absolutePos(new BlockPos(5, 3, 4));

        Zombie bossEntity = new Zombie(level);
        bossEntity.setNoAi(true);
        bossEntity.snapTo(bossPos.getX() + 0.5D, bossPos.getY(), bossPos.getZ() + 0.5D, 0.0F, 0.0F);
        helper.assertTrue(level.addFreshEntity(bossEntity), "Technical boss entity must enter the GameTest world");

        Zombie target = new Zombie(level);
        target.setNoAi(true);
        target.snapTo(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D, 0.0F, 0.0F);
        helper.assertTrue(level.addFreshEntity(target), "Boss target must enter the GameTest world");

        ContentId strike = ContentId.rift("gametest/boss_phase_strike");
        ContentId boss = ContentId.rift("gametest/boss_phase_runtime");
        ContentRegistry registry = new ContentRegistry();
        registry.register(new CoreDefinition.AttackPattern(
            strike,
            "bounded_aabb",
            2,
            3,
            2,
            Set.of("step_out"),
            "technical_boss_strike"
        ));
        registry.register(new CoreDefinition.BossProfile(boss, 2, Set.of(strike), "keep_escape_lane"));

        MinecraftBossCombatAdapter adapter = new MinecraftBossCombatAdapter(
            new CombatRuntimeCatalog(registry),
            boss,
            BossAttackSelectionPolicy.deterministicRoundRobin(),
            new MinecraftAttackAdapter.AabbHitVolume(2.0D, 1.5D),
            3.0F
        );

        long start = level.getGameTime();
        float initialHealth = target.getHealth();
        adapter.beginNextAttack(start);

        MinecraftBossCombatAdapter.TickResult telegraph = adapter.tick(level, bossEntity, start);
        helper.assertTrue(telegraph.bossPhase() == 1, "Boss must begin in authoritative phase 1");
        helper.assertTrue(telegraph.attack().phase() == AttackTimeline.Phase.TELEGRAPH, "Boss attack must begin with authored TELEGRAPH");
        helper.assertTrue(target.getHealth() == initialHealth, "Boss telegraph must never damage the target");

        MinecraftBossCombatAdapter.TickResult active = adapter.tick(level, bossEntity, start + 2L);
        helper.assertTrue(active.attack().phase() == AttackTimeline.Phase.ACTIVE, "Boss attack must enter authored ACTIVE");
        helper.assertTrue(active.attack().damagedCount() == 1, "Boss ACTIVE must apply real server damage");
        float afterPhaseOneHit = target.getHealth();
        helper.assertTrue(afterPhaseOneHit < initialHealth, "Boss ACTIVE must reduce target health");

        var transition = adapter.transitionToPhase(2);
        helper.assertTrue(transition.previousPhase() == 1 && transition.newPhase() == 2, "Server must own the 1 -> 2 phase transition");
        helper.assertTrue(transition.interruptedAttack().orElseThrow().equals(strike), "Phase transition must explicitly interrupt the old attack");
        helper.assertTrue(!adapter.attackExecuting(), "Old attack lifecycle must be closed before phase 2 becomes authoritative");

        MinecraftBossCombatAdapter.TickResult formerActive = adapter.tick(level, bossEntity, start + 3L);
        helper.assertTrue(!formerActive.attack().hitWindowOpen(), "Old ACTIVE window must not survive the phase transition");
        helper.assertTrue(target.getHealth() == afterPhaseOneHit, "Old phase must not apply damage after transition");

        long phaseTwoStart = start + 10L;
        var phaseTwoSelection = adapter.beginNextAttack(phaseTwoStart);
        helper.assertTrue(phaseTwoSelection.patternId().equals(strike), "New authoritative phase must resume deterministic attack selection");
        helper.assertTrue(adapter.phase() == 2, "New attack must execute under phase 2");

        MinecraftBossCombatAdapter.TickResult phaseTwoTelegraph = adapter.tick(level, bossEntity, phaseTwoStart);
        helper.assertTrue(phaseTwoTelegraph.attack().phase() == AttackTimeline.Phase.TELEGRAPH, "Phase 2 attack must still respect authored telegraph");
        helper.assertTrue(target.getHealth() == afterPhaseOneHit, "Phase 2 telegraph must not damage the target");

        MinecraftBossCombatAdapter.TickResult phaseTwoActive = adapter.tick(level, bossEntity, phaseTwoStart + 2L);
        helper.assertTrue(phaseTwoActive.attack().phase() == AttackTimeline.Phase.ACTIVE, "Phase 2 must reopen damage only at the authored ACTIVE boundary");
        helper.assertTrue(phaseTwoActive.attack().damagedCount() == 1, "New phase attack execution may damage the target once");
        float afterPhaseTwoHit = target.getHealth();

        MinecraftBossCombatAdapter.TickResult phaseTwoRecovery = adapter.tick(level, bossEntity, phaseTwoStart + 5L);
        helper.assertTrue(phaseTwoRecovery.attack().phase() == AttackTimeline.Phase.RECOVERY, "Phase 2 attack must enter authored recovery");
        helper.assertTrue(!phaseTwoRecovery.attack().hitWindowOpen(), "Recovery must close boss damage in every phase");
        helper.assertTrue(target.getHealth() == afterPhaseTwoHit, "Boss recovery must not damage the target");

        helper.succeed();
    }
}
