package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionEntityAttachments;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionEntityState;
import dev.moonseungjun.augmentationsurvival.augment.movement.MovementAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.movement.MovementTuning;
import dev.moonseungjun.augmentationsurvival.augment.projectile.ProjectileAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.projectile.ProjectileRuntimeCarrier;
import dev.moonseungjun.augmentationsurvival.augment.projectile.ProjectileRuntimeState;
import dev.moonseungjun.augmentationsurvival.augment.projectile.ProjectileTuning;
import dev.moonseungjun.augmentationsurvival.mixin.AbstractArrowAugmentAccessor;
import dev.moonseungjun.augmentationsurvival.mixin.LivingEntityJumpPowerAccessor;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/** Real runtime regressions for the 4-A aerial-movement and tracking-arrow playtest bundle. */
public final class AerialProjectilePlaytestGameTests implements CustomTestMethodInvoker {
    private static final String PREFIX = AugmentationSurvival.MOD_ID + ":";

    @GameTest
    public void doubleJumpUsesStrengthenedImpulseAndReboostsExistingMomentum(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        player.setOnGround(true);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("double_jump"), 1)
        );

        BlockPos air = helper.absolutePos(new BlockPos(2, 6, 2));
        player.setPos(air.getX() + 0.5D, air.getY(), air.getZ() + 0.5D);
        player.setOnGround(false);
        Vec3 before = new Vec3(0.12D, -0.10D, 0.08D);
        player.setDeltaMovement(before);

        float vanillaJumpPower = ((LivingEntityJumpPowerAccessor) player)
                .augmentationSurvival$invokeGetJumpPower();
        MovementTuning.Snapshot tuning = MovementTuning.current();
        double expectedVertical = vanillaJumpPower * tuning.doubleJumpVerticalMultiplier();
        Vec3 beforeHorizontal = new Vec3(before.x, 0.0D, before.z);
        double expectedHorizontalSpeed = beforeHorizontal.length() + tuning.doubleJumpHorizontalReboost();

        require(MovementAugmentService.tryAirJump(player),
                "owned double jump rejected its first charged airborne use");
        Vec3 after = player.getDeltaMovement();
        requireClose(after.y, expectedVertical, 1.0E-6D,
                "double jump did not use the strengthened configured vertical impulse");
        requireClose(new Vec3(after.x, 0.0D, after.z).length(), expectedHorizontalSpeed, 1.0E-9D,
                "double jump did not apply its configured horizontal reboost");
        require(new Vec3(after.x, 0.0D, after.z).normalize().dot(beforeHorizontal.normalize()) > 0.999999D,
                "double jump reboost changed horizontal travel direction");
        require(!MovementAugmentService.tryAirJump(player),
                "double jump allowed a second airborne use without a recharge");

        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void aerialComboWorksWithoutDoubleJumpAndConsumesHeldRecharge(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("aerial_combo"), 1)
        );
        require(AugmentationPlayerStateService.state(player).level(id("double_jump")) == 0,
                "aerial combo standalone test accidentally owns double jump");

        BlockPos air = helper.absolutePos(new BlockPos(2, 5, 2));
        player.setPos(air.getX() + 0.5D, air.getY(), air.getZ() + 0.5D);
        player.setOnGround(false);
        player.setDeltaMovement(Vec3.ZERO);
        MovementAugmentService.setJumpHeld(player, true);

        LivingEntity zombie = helper.spawnMob(EntityTypes.ZOMBIE, 2, 5, 3)
                .withNoFreeWill()
                .spawn();
        zombie.setHealth(zombie.getMaxHealth());
        float before = zombie.getHealth();

        require(rootMelee(player, zombie, 4.0F),
                "aerial combo test ROOT melee damage was rejected");
        require(zombie.getHealth() < before,
                "aerial combo test melee damage did not reach its hostile target");
        require(player.getDeltaMovement().y > 0.0D,
                "airborne ROOT melee hit did not immediately consume held aerial-combo recharge");
        double expectedMinimum = ((LivingEntityJumpPowerAccessor) player)
                .augmentationSurvival$invokeGetJumpPower()
                * MovementTuning.current().doubleJumpVerticalMultiplier() - 1.0E-6D;
        require(player.getDeltaMovement().y >= expectedMinimum,
                "standalone aerial combo used a weaker-than-air-jump vertical impulse");

        MovementAugmentService.setJumpHeld(player, false);
        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void aerialComboStoresRechargeUntilLaterJumpWhenNotHeld(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("aerial_combo"), 1)
        );

        BlockPos air = helper.absolutePos(new BlockPos(4, 6, 4));
        player.setPos(air.getX() + 0.5D, air.getY(), air.getZ() + 0.5D);
        player.setOnGround(false);
        player.setDeltaMovement(0.0D, -0.05D, 0.0D);
        MovementAugmentService.setJumpHeld(player, false);

        LivingEntity zombie = helper.spawnMob(EntityTypes.ZOMBIE, 4, 6, 5)
                .withNoFreeWill()
                .spawn();
        require(rootMelee(player, zombie, 4.0F),
                "stored aerial-combo ROOT melee damage was rejected");
        require(player.getDeltaMovement().y <= 0.0D,
                "aerial combo auto-jumped even though jump was not held");
        require(MovementAugmentService.tryAirJump(player),
                "airborne aerial-combo recharge was erased before a later jump press");
        require(!MovementAugmentService.tryAirJump(player),
                "one aerial-combo hit granted more than one stored air jump");

        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void aerialComboIgnoresDominatedEnemyCompanions(GameTestHelper helper) {
        ServerPlayer attacker = GameTestPlayers.survivalPlayer(helper);
        ServerPlayer companionOwner = GameTestPlayers.survivalPlayer(helper);
        AugmentationPlayerStateService.replace(
                attacker,
                AugmentationPlayerState.initial().withAugmentLevel(id("aerial_combo"), 1)
        );

        BlockPos air = helper.absolutePos(new BlockPos(6, 6, 6));
        attacker.setPos(air.getX() + 0.5D, air.getY(), air.getZ() + 0.5D);
        attacker.setOnGround(false);
        attacker.setDeltaMovement(0.0D, -0.05D, 0.0D);
        MovementAugmentService.setJumpHeld(attacker, false);

        LivingEntity zombie = helper.spawnMob(EntityTypes.ZOMBIE, 6, 6, 7)
                .withNoFreeWill()
                .spawn();
        zombie.setAttached(
                CompanionEntityAttachments.ENTITY_STATE,
                CompanionEntityState.dominatedBy(companionOwner.getUUID())
        );
        float before = zombie.getHealth();
        require(rootMelee(attacker, zombie, 4.0F),
                "dominated-companion regression setup did not produce actual damage");

        require(zombie.getHealth() < before,
                "dominated-companion regression setup did not damage the target");
        require(!MovementAugmentService.tryAirJump(attacker),
                "aerial combo recharged from a dominated companion that is no longer a hostile target");
        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void trackingArrowCorrectsTowardMovingTargetWithoutSnapTurn(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("tracking_arrow"), 3)
        );

        BlockPos origin = helper.absolutePos(new BlockPos(2, 5, 2));
        Arrow arrow = new Arrow(
                player.level(),
                origin.getX() + 0.5D,
                origin.getY() + 0.5D,
                origin.getZ() + 0.5D,
                Items.ARROW.getDefaultInstance(),
                null
        );
        arrow.setOwner(player);
        arrow.setDeltaMovement(0.0D, 0.0D, 2.0D);
        require(player.level().addFreshEntity(arrow), "tracking test arrow failed to enter the level");

        LivingEntity zombie = helper.spawnMob(EntityTypes.ZOMBIE, 6, 5, 10)
                .withNoFreeWill()
                .spawn();
        Vec3 before = arrow.getDeltaMovement().normalize();
        Vec3 desiredBefore = zombie.getBoundingBox().getCenter().subtract(arrow.position()).normalize();
        double dotBefore = before.dot(desiredBefore);

        ProjectileAugmentService.beforeArrowTick(arrow);
        Vec3 first = arrow.getDeltaMovement().normalize();
        double firstTurn = angleDegrees(before, first);
        double maxTurn = ProjectileTuning.current().trackingTurnDegrees(3);
        require(firstTurn > 0.05D,
                "tracking arrow did not correct toward an eligible off-axis target");
        require(firstTurn <= maxTurn + 1.0E-4D,
                "tracking arrow snapped farther than its per-tick turn limit: " + firstTurn);
        require(first.dot(desiredBefore) > dotBefore,
                "tracking arrow correction did not improve aim toward the target");
        requireClose(arrow.getDeltaMovement().length(), 2.0D, 1.0E-6D,
                "tracking correction changed projectile speed");

        zombie.setPos(zombie.getX() + 1.5D, zombie.getY(), zombie.getZ());
        Vec3 desiredMoved = zombie.getBoundingBox().getCenter().subtract(arrow.position()).normalize();
        double movedBefore = first.dot(desiredMoved);
        ProjectileAugmentService.beforeArrowTick(arrow);
        Vec3 second = arrow.getDeltaMovement().normalize();
        double secondTurn = angleDegrees(first, second);
        require(secondTurn <= maxTurn + 1.0E-4D,
                "tracking arrow exceeded turn cap while following a moved target: " + secondTurn);
        require(second.dot(desiredMoved) > movedBefore,
                "tracking arrow did not continue correcting toward the moved target");

        zombie.setPos(arrow.getX(), arrow.getY(), arrow.getZ() - 6.0D);
        Vec3 beforeBehind = arrow.getDeltaMovement().normalize();
        ProjectileAugmentService.beforeArrowTick(arrow);
        Vec3 afterBehind = arrow.getDeltaMovement().normalize();
        require(angleDegrees(beforeBehind, afterBehind) < 1.0E-4D,
                "tracking arrow performed a snap turn toward a target behind its acquisition cone");

        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void projectileFlightStateSurvivesEntitySaveAndReload(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos origin = helper.absolutePos(new BlockPos(2, 5, 2));
        Arrow arrow = new Arrow(
                player.level(), origin.getX() + 0.5D, origin.getY() + 0.5D,
                origin.getZ() + 0.5D, Items.ARROW.getDefaultInstance(), null
        );
        arrow.setOwner(player);
        require(player.level().addFreshEntity(arrow), "save/reload test arrow failed to enter the level");

        ProjectileRuntimeState runtime = new ProjectileRuntimeState();
        UUID priorTarget = UUID.randomUUID();
        runtime.initialize(player.getUUID(), (byte) 2, 3, 2, 3, 1);
        runtime.setBaseDamageScale(0.75D);
        runtime.setProvenance(CombatProvenance.DERIVED_PROJECTILE);
        runtime.recordBounce();
        runtime.recordSegmentHit(priorTarget);
        runtime.setTrackingTargetId(priorTarget);
        runtime.markChainUsed();
        ((ProjectileRuntimeCarrier) arrow).augmentationSurvival$setProjectileRuntime(runtime);
        ((AbstractArrowAugmentAccessor) arrow).augmentationSurvival$invokeSetPierceLevel((byte) 5);

        TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        arrow.saveWithoutId(output);
        CompoundTag saved = output.buildResult();
        Arrow restored = new Arrow(
                player.level(), origin.getX() + 0.5D, origin.getY() + 0.5D,
                origin.getZ() + 0.5D, Items.ARROW.getDefaultInstance(), null
        );
        restored.load(TagValueInput.create(
                ProblemReporter.DISCARDING, player.level().registryAccess(), saved
        ));

        ProjectileRuntimeState loaded = ((ProjectileRuntimeCarrier) restored)
                .augmentationSurvival$getProjectileRuntime();
        require(loaded != null && loaded.initialized(), "saved projectile runtime was not restored");
        require(player.getUUID().equals(loaded.originalOwnerId()), "projectile owner identity was not persisted");
        require(loaded.provenance() == CombatProvenance.DERIVED_PROJECTILE,
                "projectile provenance changed across save/reload");
        requireClose(loaded.baseDamageScale(), 0.75D, 1.0E-9D,
                "projectile base damage scale changed across save/reload");
        require(loaded.vanillaPierceLevel() == 2 && loaded.piercingLevel() == 3,
                "vanilla and augment pierce budgets were not kept separate across save/reload");
        ProjectileAugmentService.beforeArrowTick(restored);
        require(Byte.toUnsignedInt(restored.getPierceLevel()) == 5,
                "restored projectile re-added augment pierce to its already combined vanilla value");
        require(loaded.trackingLevel() == 2 && loaded.ricochetLevel() == 3 && loaded.chainLevel() == 1,
                "projectile augment levels changed across save/reload");
        require(loaded.bounceCount() == 1 && loaded.chainUsed(),
                "flight-wide ricochet or chain state reset across save/reload");
        require(loaded.segmentHitCount() == 1 && loaded.hasHitInCurrentSegment(priorTarget),
                "segment hit identity was not preserved across save/reload");
        require(priorTarget.equals(loaded.trackingTargetId()),
                "tracking target identity was not preserved across save/reload");

        GameTestPlayers.succeed(helper);
    }

    private static boolean rootMelee(ServerPlayer attacker, LivingEntity target, float damage) {
        boolean applied = CombatProvenanceContext.callAs(
                CombatProvenance.ROOT_MELEE,
                () -> target.hurtServer(
                        (ServerLevel) target.level(),
                        attacker.damageSources().playerAttack(attacker),
                        damage
                )
        );
        if (applied) {
            // Mirrors the canonical post-Player.attack boundary owned by PlayerRootMeleeProvenanceMixin.
            MovementAugmentService.onSuccessfulRootMelee(attacker, target);
        }
        return applied;
    }

    private static double angleDegrees(Vec3 first, Vec3 second) {
        double dot = Math.max(-1.0D, Math.min(1.0D, first.normalize().dot(second.normalize())));
        return Math.toDegrees(Math.acos(dot));
    }

    private static String id(String path) {
        return PREFIX + path;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireClose(double actual, double expected, double tolerance, String message) {
        if (Math.abs(actual - expected) > tolerance) {
            throw new AssertionError(message + "; expected=" + expected + ", actual=" + actual);
        }
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        GameTestPlayers.invoke(this, helper, method);
    }
}
