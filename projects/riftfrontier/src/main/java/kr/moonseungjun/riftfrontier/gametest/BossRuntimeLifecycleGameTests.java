package kr.moonseungjun.riftfrontier.gametest;

import kr.moonseungjun.riftfrontier.Riftfrontier;
import kr.moonseungjun.riftfrontier.combat.AttackTimeline;
import kr.moonseungjun.riftfrontier.combat.BossCombatSemanticProfile;
import kr.moonseungjun.riftfrontier.combat.CombatRuntimeCatalog;
import kr.moonseungjun.riftfrontier.combat.MinecraftAttackAdapter;
import kr.moonseungjun.riftfrontier.combat.MinecraftBossCombatAdapter;
import kr.moonseungjun.riftfrontier.combat.ValidatedBossCombatSemantics;
import kr.moonseungjun.riftfrontier.combat.presentation.BossPresentationProfile;
import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.ContentRegistry;
import kr.moonseungjun.riftfrontier.content.CoreDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** Native regression coverage for validated boss capability lifetime ownership. */
public final class BossRuntimeLifecycleGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(
        BuiltInRegistries.TEST_FUNCTION,
        Riftfrontier.MOD_ID
    );

    static {
        TEST_FUNCTIONS.register("boss_runtime_owner_lifecycle", () -> BossRuntimeLifecycleGameTests::bossRuntimeOwnerLifecycle);
    }

    private BossRuntimeLifecycleGameTests() {}

    public static void register(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    private static void bossRuntimeOwnerLifecycle(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(4, 3, 4));
        Zombie owner = spawn(helper, origin, 0.0D);
        Zombie replacement = spawn(helper, origin, 2.0D);

        ContentId attack = ContentId.rift("gametest/boss_runtime_owner_attack");
        ContentId boss = ContentId.rift("gametest/boss_runtime_owner_profile");
        ContentRegistry registry = new ContentRegistry();
        registry.register(new CoreDefinition.AttackPattern(
            attack,
            "technical_owner_lifecycle",
            1,
            1,
            1,
            Set.of("step_out"),
            "boss_runtime_owner_cue"
        ));
        registry.register(new CoreDefinition.BossProfile(boss, 1, Set.of(attack), "keep_escape_lane"));

        Map<BossPresentationProfile.BindingKey, BossPresentationProfile.AssetBinding> bindings = new LinkedHashMap<>();
        addPresentationBindings(bindings, "boss_runtime_owner_cue", "technical_owner_lifecycle");
        BossPresentationProfile presentation = new BossPresentationProfile(
            ContentId.rift("gametest/boss_runtime_owner_presentation"),
            boss,
            "default",
            ContentId.rift("gametest/boss_runtime_owner_model"),
            bindings
        );
        CombatRuntimeCatalog catalog = new CombatRuntimeCatalog(registry);
        ValidatedBossCombatSemantics semantics = ValidatedBossCombatSemantics.validate(
            catalog,
            new BossCombatSemanticProfile(boss, Map.of(1, Set.of(attack))),
            presentation
        );
        MinecraftBossCombatAdapter.ValidatedRuntime runtime = MinecraftBossCombatAdapter.validated(
            catalog,
            semantics,
            new MinecraftAttackAdapter.AabbHitVolume(1.0D, 1.0D),
            1.0F
        );

        long start = level.getGameTime();
        helper.assertTrue(runtime.beginNextAttack(level, owner, start).patternId().equals(attack),
            "Minecraft-facing validated begin must bind the capability to its first authoritative owner");
        helper.assertTrue(runtime.tick(level, owner, start).combat().attack().phase() == AttackTimeline.Phase.TELEGRAPH,
            "Bound owner must advance the authored attack clock");
        helper.assertTrue(runtime.cancelAttack(), "Fixture must explicitly close its first attack before reuse checks");

        try {
            runtime.beginNextAttack(level, replacement, start + 5L);
            helper.assertTrue(false,
                "A retained validated capability must not be adopted by a replacement entity after attack cancellation");
        } catch (IllegalStateException expected) {
            helper.assertTrue(runtime.beginNextAttack(level, owner, start + 6L).patternId().equals(attack),
                "Wrong-owner rejection must preserve the capability's original owner binding");
            runtime.cancelAttack();
        }

        MinecraftBossCombatAdapter.ValidatedRuntime parallelRuntime = MinecraftBossCombatAdapter.validated(
            catalog,
            semantics,
            new MinecraftAttackAdapter.AabbHitVolume(1.0D, 1.0D),
            1.0F
        );
        try {
            parallelRuntime.beginNextAttack(level, owner, start + 7L);
            helper.assertTrue(false,
                "A live boss entity must not own two independently mutable validated combat runtimes");
        } catch (IllegalStateException expected) {
            helper.assertTrue(runtime.beginNextAttack(level, owner, start + 8L).patternId().equals(attack),
                "Rejected parallel capability must not disturb the authoritative runtime already claimed by the boss");
            runtime.cancelAttack();
        }

        MinecraftBossCombatAdapter.entityLeaveLevel(new EntityLeaveLevelEvent(owner, level));
        try {
            runtime.phase();
            helper.assertTrue(false, "Owner leave must permanently invalidate the retained validated capability");
        } catch (IllegalStateException expected) {
            helper.assertTrue(!runtime.cancelAttack(),
                "Cleanup must remain callable after owner invalidation without resurrecting an attack");
        }

        try {
            runtime.beginNextAttack(level, owner, start + 10L);
            helper.assertTrue(false, "An invalidated capability must not resume even with its former owner instance");
        } catch (IllegalStateException expected) {
            // Expected fail-closed owner lifetime boundary.
        }

        helper.assertTrue(parallelRuntime.beginNextAttack(level, replacement, start + 12L).patternId().equals(attack),
            "A failed duplicate-owner claim must leave that runtime unbound so a different valid actor can own it later");
        parallelRuntime.cancelAttack();

        helper.succeed();
    }

    private static Zombie spawn(GameTestHelper helper, BlockPos origin, double xOffset) {
        Zombie entity = new Zombie(helper.getLevel());
        entity.setNoAi(true);
        entity.snapTo(origin.getX() + xOffset + 0.5D, origin.getY(), origin.getZ() + 0.5D, 0.0F, 0.0F);
        helper.assertTrue(helper.getLevel().addFreshEntity(entity),
            "Technical boss lifecycle entity must enter the GameTest world");
        return entity;
    }

    private static void addPresentationBindings(
        Map<BossPresentationProfile.BindingKey, BossPresentationProfile.AssetBinding> bindings,
        String cue,
        String delivery
    ) {
        for (AttackTimeline.Phase phase : List.of(
            AttackTimeline.Phase.TELEGRAPH,
            AttackTimeline.Phase.ACTIVE,
            AttackTimeline.Phase.RECOVERY
        )) {
            String suffix = phase.name().toLowerCase(Locale.ROOT);
            bindings.put(
                new BossPresentationProfile.BindingKey(cue, delivery, phase),
                new BossPresentationProfile.AssetBinding(
                    ContentId.rift("gametest/boss_runtime_owner_anim_" + suffix),
                    ContentId.rift("gametest/boss_runtime_owner_vfx_" + suffix),
                    ContentId.rift("gametest/boss_runtime_owner_sound_" + suffix)
                )
            );
        }
    }
}
