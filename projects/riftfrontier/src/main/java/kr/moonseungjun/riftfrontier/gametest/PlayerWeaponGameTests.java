package kr.moonseungjun.riftfrontier.gametest;

import kr.moonseungjun.riftfrontier.Riftfrontier;
import kr.moonseungjun.riftfrontier.combat.AttackTimeline;
import kr.moonseungjun.riftfrontier.combat.CombatRuntimeCatalog;
import kr.moonseungjun.riftfrontier.combat.MinecraftPlayerWeaponCombatAdapter;
import kr.moonseungjun.riftfrontier.combat.PlayerWeaponItemStackLoadoutResolver;
import kr.moonseungjun.riftfrontier.combat.PlayerWeaponLoadoutComponent;
import kr.moonseungjun.riftfrontier.combat.PlayerWeaponServerRuntime;
import kr.moonseungjun.riftfrontier.combat.RiftfrontierCombatDataComponents;
import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.ContentRegistry;
import kr.moonseungjun.riftfrontier.content.CoreDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** Native in-world regression coverage for the server-authoritative player weapon adapter. */
public final class PlayerWeaponGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(
        BuiltInRegistries.TEST_FUNCTION,
        Riftfrontier.MOD_ID
    );

    static {
        TEST_FUNCTIONS.register("player_weapon_authority", () -> PlayerWeaponGameTests::playerWeaponAuthority);
        TEST_FUNCTIONS.register("player_weapon_input_authority", () -> PlayerWeaponGameTests::playerWeaponInputAuthority);
    }

    private PlayerWeaponGameTests() {}

    public static void register(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    private static void playerWeaponAuthority(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos actorPos = helper.absolutePos(new BlockPos(4, 3, 4));
        BlockPos targetPos = helper.absolutePos(new BlockPos(5, 3, 4));

        Zombie actor = new Zombie(level);
        actor.setNoAi(true);
        actor.snapTo(actorPos.getX() + 0.5D, actorPos.getY(), actorPos.getZ() + 0.5D, 0.0F, 0.0F);
        helper.assertTrue(level.addFreshEntity(actor), "Technical weapon actor must enter the GameTest world");

        Zombie target = new Zombie(level);
        target.setNoAi(true);
        target.snapTo(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D, 0.0F, 0.0F);
        helper.assertTrue(level.addFreshEntity(target), "Technical weapon target must enter the GameTest world");

        ContentId mobileMove = ContentId.rift("gametest/mobile_pressure_move");
        ContentId reachMove = ContentId.rift("gametest/reach_commitment_move");
        ContentId mobileFamily = ContentId.rift("gametest/mobile_pressure_family");
        ContentId reachFamily = ContentId.rift("gametest/reach_commitment_family");
        ContentId recoveryModule = ContentId.rift("gametest/recovery_pivot_module");

        ContentRegistry registry = new ContentRegistry();
        registry.register(new CoreDefinition.AttackPattern(
            mobileMove, "technical_mobile", 2, 2, 2, Set.of("step_out"), "technical_mobile_cue"
        ));
        registry.register(new CoreDefinition.AttackPattern(
            reachMove, "technical_reach", 3, 2, 3, Set.of("sidestep"), "technical_reach_cue"
        ));
        registry.register(new CoreDefinition.WeaponFamily(
            mobileFamily, Set.of(mobileMove), Set.of("mobile_pressure"), Set.of("technique")
        ));
        registry.register(new CoreDefinition.WeaponFamily(
            reachFamily, Set.of(reachMove), Set.of("reach_commitment"), Set.of("technique")
        ));
        registry.register(new CoreDefinition.WeaponModule(
            recoveryModule, Set.of(mobileFamily), "technique", Set.of("recovery_pivot")
        ));

        AtomicReference<Optional<MinecraftPlayerWeaponCombatAdapter.Loadout>> equipped = new AtomicReference<>(Optional.of(
            new MinecraftPlayerWeaponCombatAdapter.Loadout(mobileFamily, Optional.of(recoveryModule))
        ));
        MinecraftPlayerWeaponCombatAdapter adapter = new MinecraftPlayerWeaponCombatAdapter(
            new CombatRuntimeCatalog(registry),
            ignored -> equipped.get(),
            (serverLevel, attacker, snapshot) -> List.of(target)
        );

        long start = level.getGameTime();
        adapter.beginMove(actor, mobileMove, start);

        var telegraph = adapter.tick(level, actor, start);
        helper.assertTrue(telegraph.phase() == AttackTimeline.Phase.TELEGRAPH,
            "Weapon intent must begin on the authored TELEGRAPH clock");
        helper.assertTrue(!telegraph.hitWindowOpen() && telegraph.uniqueCandidateCount() == 0,
            "TELEGRAPH must not expose hit-volume candidates");
        helper.assertTrue(!adapter.recoveryPivotAuthorized(actor, start),
            "recovery_pivot must remain closed before RECOVERY");

        var active = adapter.tick(level, actor, start + 2L);
        helper.assertTrue(active.phase() == AttackTimeline.Phase.ACTIVE && active.hitWindowOpen(),
            "Authoritative ACTIVE must be the only open hit-volume phase");
        helper.assertTrue(active.uniqueCandidateCount() == 1,
            "ACTIVE must expose the technical target once for this execution");

        var activeAgain = adapter.tick(level, actor, start + 3L);
        helper.assertTrue(activeAgain.uniqueCandidateCount() == 0,
            "Repeated ACTIVE sampling must not duplicate the same target candidate");

        var recovery = adapter.tick(level, actor, start + 4L);
        helper.assertTrue(recovery.phase() == AttackTimeline.Phase.RECOVERY && !recovery.hitWindowOpen(),
            "Authored RECOVERY must close the hit-volume window");
        helper.assertTrue(adapter.recoveryPivotAuthorized(actor, start + 4L),
            "Validated recovery_pivot module may authorize only during RECOVERY");

        equipped.set(Optional.of(new MinecraftPlayerWeaponCombatAdapter.Loadout(reachFamily, Optional.empty())));
        var invalidated = adapter.tick(level, actor, start + 5L);
        helper.assertTrue(invalidated.loadoutInvalidated(),
            "Server-observed equipment swap must invalidate the old weapon execution immediately");
        helper.assertTrue(!adapter.hasSession(actor.getUUID()),
            "Invalidated equipment must discard the old per-actor runtime session");
        helper.assertTrue(!adapter.recoveryPivotAuthorized(actor, start + 5L),
            "Old module semantics must not survive a server loadout swap");

        adapter.beginMove(actor, reachMove, start + 10L);
        actor.setHealth(0.0F);
        var deathInvalidated = adapter.tick(level, actor, start + 11L);
        helper.assertTrue(deathInvalidated.loadoutInvalidated(),
            "Death must invalidate an existing authoritative weapon execution before it can advance");
        helper.assertTrue(!adapter.hasSession(actor.getUUID()),
            "Dead actors must not retain authoritative weapon sessions");
        try {
            adapter.beginMove(actor, reachMove, start + 12L);
            helper.assertTrue(false, "Dead actors must not establish fresh weapon authority");
        } catch (IllegalStateException expected) {
            helper.assertTrue(!adapter.hasSession(actor.getUUID()),
                "Rejected dead-actor input must leave no weapon session behind");
        }

        Zombie spectatorActor = new Zombie(level) {
            @Override
            public boolean isSpectator() {
                return true;
            }
        };
        spectatorActor.setNoAi(true);
        spectatorActor.snapTo(actorPos.getX() + 1.5D, actorPos.getY(), actorPos.getZ() + 0.5D, 0.0F, 0.0F);
        helper.assertTrue(level.addFreshEntity(spectatorActor), "Technical spectator actor must enter the GameTest world");
        try {
            adapter.beginMove(spectatorActor, reachMove, start + 15L);
            helper.assertTrue(false, "Spectator actors must not establish weapon authority");
        } catch (IllegalStateException expected) {
            helper.assertTrue(!adapter.hasSession(spectatorActor.getUUID()),
                "Rejected spectator input must leave no weapon session behind");
        }

        Zombie otherActor = new Zombie(level);
        otherActor.setNoAi(true);
        otherActor.snapTo(actorPos.getX() + 2.5D, actorPos.getY(), actorPos.getZ() + 0.5D, 0.0F, 0.0F);
        helper.assertTrue(level.addFreshEntity(otherActor), "Second technical weapon actor must enter the GameTest world");
        adapter.beginMove(otherActor, reachMove, start + 20L);
        helper.assertTrue(adapter.hasSession(otherActor.getUUID()),
            "Another eligible actor may still own an isolated authoritative weapon session");
        helper.assertTrue(adapter.clearActor(otherActor.getUUID()),
            "Lifecycle cleanup must cancel and remove the selected actor session");
        helper.assertTrue(!adapter.hasSession(otherActor.getUUID()),
            "Clearing an actor must discard only that actor's session");

        helper.succeed();
    }

    @SuppressWarnings("removal")
    private static void playerWeaponInputAuthority(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack mobile = new ItemStack(Items.STICK);
        mobile.set(
            RiftfrontierCombatDataComponents.PLAYER_WEAPON_LOADOUT.value(),
            new PlayerWeaponLoadoutComponent(
                PlayerWeaponItemStackLoadoutResolver.MOBILE_PRESSURE.toString(),
                Optional.of(PlayerWeaponItemStackLoadoutResolver.RECOVERY_PIVOT.toString())
            )
        );
        player.setItemInHand(InteractionHand.MAIN_HAND, mobile);

        ContentId mobileEntry = ContentId.rift("attack/player/mobile_pressure_entry");
        ContentId reachStrike = ContentId.rift("attack/player/reach_commitment_strike");
        helper.assertTrue(
            PlayerWeaponServerRuntime.handleMoveIntent(player, reachStrike) == PlayerWeaponServerRuntime.IntentResult.REJECTED,
            "Server must reject a valid authored move from the wrong equipped family"
        );
        helper.assertTrue(
            PlayerWeaponServerRuntime.handleMoveIntent(player, mobileEntry) == PlayerWeaponServerRuntime.IntentResult.ACCEPTED,
            "Server must accept an authored move belonging to the current ItemStack family"
        );
        helper.assertTrue(
            PlayerWeaponServerRuntime.handleMoveIntent(player, ContentId.rift("attack/player/spoofed")) == PlayerWeaponServerRuntime.IntentResult.REJECTED,
            "Server must reject spoofed move ids even when a valid Riftfrontier weapon is equipped"
        );

        ItemStack reach = new ItemStack(Items.STICK);
        reach.set(
            RiftfrontierCombatDataComponents.PLAYER_WEAPON_LOADOUT.value(),
            new PlayerWeaponLoadoutComponent(PlayerWeaponItemStackLoadoutResolver.REACH_COMMITMENT.toString(), Optional.empty())
        );
        player.setItemInHand(InteractionHand.MAIN_HAND, reach);
        var swapResult = PlayerWeaponServerRuntime.tickPlayer(player);
        helper.assertTrue(swapResult.loadoutInvalidated(),
            "Server-observed main-hand swap must invalidate the previous execution before further ticks");
        helper.assertTrue(
            PlayerWeaponServerRuntime.handleMoveIntent(player, reachStrike) == PlayerWeaponServerRuntime.IntentResult.ACCEPTED,
            "Fresh move intent may establish a new session from the newly equipped family"
        );

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        var unequipResult = PlayerWeaponServerRuntime.tickPlayer(player);
        helper.assertTrue(unequipResult.loadoutInvalidated(),
            "Unequip must invalidate the authoritative weapon session"
        );
        helper.assertTrue(
            PlayerWeaponServerRuntime.handleMoveIntent(player, reachStrike) == PlayerWeaponServerRuntime.IntentResult.REJECTED,
            "An unequipped player cannot start a remembered weapon move"
        );
        PlayerWeaponServerRuntime.clearPlayer(player);
        helper.succeed();
    }
}
