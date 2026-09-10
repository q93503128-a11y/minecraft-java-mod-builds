package kr.moonseungjun.riftfrontier.gametest;

import kr.moonseungjun.riftfrontier.Riftfrontier;
import kr.moonseungjun.riftfrontier.combat.AttackTimeline;
import kr.moonseungjun.riftfrontier.combat.BossAttackSelectionPolicy;
import kr.moonseungjun.riftfrontier.combat.CombatRuntimeCatalog;
import kr.moonseungjun.riftfrontier.combat.MinecraftAttackAdapter;
import kr.moonseungjun.riftfrontier.combat.MinecraftBossCombatAdapter;
import kr.moonseungjun.riftfrontier.combat.MinecraftPlayerWeaponCombatAdapter;
import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.ContentRegistry;
import kr.moonseungjun.riftfrontier.content.CoreDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/** Native regression coverage for shared server actor/target combat admission. */
public final class CombatAuthorityGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(
        BuiltInRegistries.TEST_FUNCTION,
        Riftfrontier.MOD_ID
    );

    static {
        TEST_FUNCTIONS.register("combat_authority_boundary", () -> CombatAuthorityGameTests::combatAuthorityBoundary);
    }

    private CombatAuthorityGameTests() {}

    public static void register(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    private static void combatAuthorityBoundary(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(4, 3, 4));

        Zombie attacker = spawn(helper, origin, 0.0D);
        Zombie validTarget = spawn(helper, origin, 1.0D);
        Zombie spectatorTarget = new Zombie(level) {
            @Override
            public boolean isSpectator() {
                return true;
            }
        };
        spectatorTarget.setNoAi(true);
        spectatorTarget.snapTo(origin.getX() + 2.0D, origin.getY(), origin.getZ() + 0.5D, 0.0F, 0.0F);
        helper.assertTrue(level.addFreshEntity(spectatorTarget), "Technical spectator target must enter the test world");

        Zombie removedTarget = new Zombie(level);
        removedTarget.discard();
        helper.assertTrue(removedTarget.isRemoved(), "Technical removed target must expose removed state");

        ContentId attackId = ContentId.rift("gametest/combat_authority_attack");
        CoreDefinition.AttackPattern pattern = new CoreDefinition.AttackPattern(
            attackId, "technical_authority", 1, 1, 1, Set.of("step_out"), "technical_authority_cue"
        );
        long start = level.getGameTime();
        float validHealth = validTarget.getHealth();
        float spectatorHealth = spectatorTarget.getHealth();

        MinecraftAttackAdapter direct = new MinecraftAttackAdapter(
            (serverLevel, actor, snapshot) -> List.of(validTarget, spectatorTarget, removedTarget),
            2.0F
        );
        direct.begin(level, attacker, pattern, start);
        helper.assertTrue(direct.tick(level, attacker, start).phase() == AttackTimeline.Phase.TELEGRAPH,
            "Authority regression must preserve authored TELEGRAPH");
        var active = direct.tick(level, attacker, start + 1L);
        helper.assertTrue(active.candidateCount() == 1 && active.damagedCount() == 1,
            "Only an alive non-removed non-spectator target in the authoritative level may be damaged");
        helper.assertTrue(validTarget.getHealth() < validHealth,
            "Eligible target must still receive the technical ACTIVE damage event");
        helper.assertTrue(spectatorTarget.getHealth() == spectatorHealth,
            "Spectator targets returned by a custom hit resolver must be rejected centrally");

        Zombie replacementAttacker = spawn(helper, origin, 2.5D);
        Zombie replacementOnlyTarget = spawn(helper, origin, 3.0D);
        float replacementOnlyHealth = replacementOnlyTarget.getHealth();
        CoreDefinition.AttackPattern identityPattern = new CoreDefinition.AttackPattern(
            ContentId.rift("gametest/combat_attacker_identity"),
            "technical_identity",
            1,
            2,
            1,
            Set.of("step_out"),
            "technical_identity_cue"
        );
        MinecraftAttackAdapter identityBound = new MinecraftAttackAdapter(
            (serverLevel, actor, snapshot) -> actor == replacementAttacker ? List.of(replacementOnlyTarget) : List.of(),
            2.0F
        );
        identityBound.begin(level, attacker, identityPattern, start + 5L);
        helper.assertTrue(identityBound.tick(level, attacker, start + 5L).phase() == AttackTimeline.Phase.TELEGRAPH,
            "Identity-bound execution must still begin on the authored TELEGRAPH");
        var replacementAttempt = identityBound.tick(level, replacementAttacker, start + 6L);
        helper.assertTrue(!identityBound.isExecuting() && !replacementAttempt.hitWindowOpen(),
            "A different entity instance must cancel, not inherit, an already-bound generic attack execution");
        helper.assertTrue(replacementOnlyTarget.getHealth() == replacementOnlyHealth,
            "Attacker replacement must fail closed before the replacement actor can resolve ACTIVE damage");

        Zombie deadAttacker = spawn(helper, origin, 3.5D);
        Zombie protectedTarget = spawn(helper, origin, 4.0D);
        float protectedHealth = protectedTarget.getHealth();
        MinecraftAttackAdapter invalidActorAdapter = new MinecraftAttackAdapter(
            (serverLevel, actor, snapshot) -> List.of(protectedTarget),
            2.0F
        );
        invalidActorAdapter.begin(pattern, start + 10L);
        deadAttacker.setHealth(0.0F);
        var invalidActorTick = invalidActorAdapter.tick(level, deadAttacker, start + 11L);
        helper.assertTrue(!invalidActorAdapter.isExecuting() && !invalidActorTick.hitWindowOpen(),
            "An ineligible authoritative attacker must cancel its clock before hit resolution");
        helper.assertTrue(protectedTarget.getHealth() == protectedHealth,
            "Dead attacker must not reach a custom hit resolver damage path");

        ContentId bossId = ContentId.rift("gametest/combat_authority_boss");
        ContentRegistry bossRegistry = new ContentRegistry();
        bossRegistry.register(pattern);
        bossRegistry.register(new CoreDefinition.BossProfile(bossId, 1, Set.of(attackId), "keep_escape_lane"));
        Zombie boss = spawn(helper, origin, 5.0D);
        Zombie bossTarget = spawn(helper, origin, 6.0D);
        float bossTargetHealth = bossTarget.getHealth();
        MinecraftBossCombatAdapter bossAdapter = new MinecraftBossCombatAdapter(
            new CombatRuntimeCatalog(bossRegistry),
            bossId,
            BossAttackSelectionPolicy.deterministicRoundRobin(),
            (serverLevel, actor, snapshot) -> List.of(bossTarget),
            2.0F
        );
        bossAdapter.beginNextAttack(start + 20L);
        boss.setHealth(0.0F);
        var invalidBossTick = bossAdapter.tick(level, boss, start + 21L);
        helper.assertTrue(!bossAdapter.attackExecuting() && invalidBossTick.presentation().isEmpty(),
            "Boss actor invalidation must close lifecycle and presentation together");
        helper.assertTrue(!invalidBossTick.attack().hitWindowOpen() && bossTarget.getHealth() == bossTargetHealth,
            "Invalid boss actor must not progress its damage adapter");

        Zombie bossOwner = spawn(helper, origin, 6.5D);
        Zombie bossReplacement = spawn(helper, origin, 7.0D);
        Zombie bossReplacementTarget = spawn(helper, origin, 7.5D);
        float bossReplacementHealth = bossReplacementTarget.getHealth();
        MinecraftBossCombatAdapter identityBossAdapter = new MinecraftBossCombatAdapter(
            new CombatRuntimeCatalog(bossRegistry),
            bossId,
            BossAttackSelectionPolicy.deterministicRoundRobin(),
            (serverLevel, actor, snapshot) -> actor == bossReplacement ? List.of(bossReplacementTarget) : List.of(),
            2.0F
        );
        identityBossAdapter.beginNextAttack(start + 25L);
        helper.assertTrue(identityBossAdapter.tick(level, bossOwner, start + 25L).attack().phase() == AttackTimeline.Phase.TELEGRAPH,
            "Boss damage execution must bind to the first authoritative boss entity that advances it");
        try {
            identityBossAdapter.tick(level, bossReplacement, start + 26L);
            helper.assertTrue(false, "Boss replacement must not inherit another entity's ACTIVE attack execution");
        } catch (IllegalStateException expected) {
            helper.assertTrue(!identityBossAdapter.attackExecuting(),
                "Boss identity divergence must fail closed across lifecycle and damage clocks");
            helper.assertTrue(bossReplacementTarget.getHealth() == bossReplacementHealth,
                "Boss replacement must be rejected before custom hit resolution can damage a target");
        }

        ContentId familyId = ContentId.rift("gametest/combat_authority_family");
        ContentRegistry playerRegistry = new ContentRegistry();
        playerRegistry.register(pattern);
        playerRegistry.register(new CoreDefinition.WeaponFamily(
            familyId, Set.of(attackId), Set.of("mobile_pressure"), Set.of("technique")
        ));
        Zombie playerActor = spawn(helper, origin, 8.0D);
        Zombie playerTarget = spawn(helper, origin, 8.5D);
        MinecraftPlayerWeaponCombatAdapter playerAdapter = new MinecraftPlayerWeaponCombatAdapter(
            new CombatRuntimeCatalog(playerRegistry),
            ignored -> Optional.of(new MinecraftPlayerWeaponCombatAdapter.Loadout(familyId, Optional.empty())),
            (serverLevel, actor, snapshot) -> List.of(playerTarget, spectatorTarget, removedTarget)
        );
        playerAdapter.beginMove(playerActor, attackId, start + 30L);
        playerAdapter.tick(level, playerActor, start + 30L);
        var playerActive = playerAdapter.tick(level, playerActor, start + 31L);
        helper.assertTrue(playerActive.hitWindowOpen() && playerActive.uniqueCandidateCount() == 1,
            "Player hit candidate authority must reject resolver-supplied spectator/removed targets too");

        helper.succeed();
    }

    private static Zombie spawn(GameTestHelper helper, BlockPos origin, double xOffset) {
        Zombie entity = new Zombie(helper.getLevel());
        entity.setNoAi(true);
        entity.snapTo(origin.getX() + xOffset + 0.5D, origin.getY(), origin.getZ() + 0.5D, 0.0F, 0.0F);
        helper.assertTrue(helper.getLevel().addFreshEntity(entity), "Technical combat authority entity must enter test world");
        return entity;
    }
}
