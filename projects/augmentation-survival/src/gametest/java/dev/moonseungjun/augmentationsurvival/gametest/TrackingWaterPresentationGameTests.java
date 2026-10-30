package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.AugmentPresentationCatalog;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionEntityAttachments;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionEntityState;
import dev.moonseungjun.augmentationsurvival.augment.projectile.ProjectileRuntimeCarrier;
import dev.moonseungjun.augmentationsurvival.augment.projectile.ProjectileRuntimeState;
import dev.moonseungjun.augmentationsurvival.augment.projectile.ProjectileTuning;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import java.util.Locale;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

/** Regressions for tracking, water traversal and repaired player-facing descriptions. */
public final class TrackingWaterPresentationGameTests implements CustomTestMethodInvoker {
    private static final String PREFIX = AugmentationSurvival.MOD_ID + ":";

    @GameTest
    public void trackingArrowUsesActualArrowTickAndTargetsNeutralButNotOwnedCompanion(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("tracking_arrow"), 3)
        );

        BlockPos origin = helper.absolutePos(new BlockPos(2, 6, 2));
        Arrow arrow = new Arrow(
                player.level(),
                origin.getX() + 0.5D,
                origin.getY() + 0.5D,
                origin.getZ() + 0.5D,
                Items.ARROW.getDefaultInstance(),
                null
        );
        arrow.setOwner(player);
        arrow.setNoGravity(true);
        arrow.setDeltaMovement(0.0D, 0.0D, 1.6D);
        require(player.level().addFreshEntity(arrow), "tracking arrow failed to enter the level");

        LivingEntity ownedCompanion = helper.spawnMob(EntityTypes.ZOMBIE, 3, 6, 6)
                .withNoFreeWill()
                .spawn();
        ownedCompanion.setAttached(
                CompanionEntityAttachments.ENTITY_STATE,
                CompanionEntityState.dominatedBy(player.getUUID())
        );

        LivingEntity neutral = helper.spawnMob(EntityTypes.IRON_GOLEM, 5, 6, 10)
                .withNoFreeWill()
                .spawn();
        Vec3 before = arrow.getDeltaMovement().normalize();
        Vec3 desired = neutral.getBoundingBox().getCenter().subtract(arrow.position()).normalize();
        double dotBefore = before.dot(desired);

        arrow.tick();

        ProjectileRuntimeState runtime = ((ProjectileRuntimeCarrier) arrow)
                .augmentationSurvival$getProjectileRuntime();
        require(runtime != null && runtime.initialized(),
                "actual AbstractArrow.tick did not initialize projectile augment runtime");
        require(neutral.getUUID().equals(runtime.trackingTargetId()),
                "tracking selected an owned companion or failed to acquire the neutral attackable target");

        Vec3 first = arrow.getDeltaMovement().normalize();
        double firstTurn = angleDegrees(before, first);
        double maxTurn = ProjectileTuning.current().trackingTurnDegrees(3);
        require(firstTurn > 0.02D,
                "actual arrow tick did not steer toward an off-axis neutral target");
        require(firstTurn <= maxTurn + 1.0E-3D,
                "actual arrow tick exceeded the configured tracking turn cap: " + firstTurn);
        require(first.dot(neutral.getBoundingBox().getCenter().subtract(arrow.position()).normalize()) > dotBefore,
                "actual arrow tick did not improve aim toward the acquired target");

        neutral.setPos(neutral.getX() + 1.0D, neutral.getY(), neutral.getZ());
        double speed = arrow.getDeltaMovement().length();
        arrow.setDeltaMovement(first.x * speed, -0.30D, first.z * speed);
        Vec3 fallingBefore = arrow.getDeltaMovement().normalize();
        Vec3 fallingDesired = neutral.getBoundingBox().getCenter().subtract(arrow.position()).normalize();
        double fallingDotBefore = fallingBefore.dot(fallingDesired);
        arrow.tick();
        Vec3 fallingAfter = arrow.getDeltaMovement().normalize();
        require(fallingAfter.dot(neutral.getBoundingBox().getCenter().subtract(arrow.position()).normalize())
                        > fallingDotBefore,
                "descending actual arrow tick stopped correcting toward the tracked target");
        require(neutral.getUUID().equals(runtime.trackingTargetId()),
                "tracking target was lost merely because the arrow entered a descending trajectory");

        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 20)
    public void waterRunSupportsStandingWalkingCrouchSinkAndFiveBlockDive(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("water_run"), 1)
        );
        BlockPos anchor = helper.absolutePos(new BlockPos(3, 4, 3));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        player.setOnGround(false);
        player.setSprinting(false);
        player.setDeltaMovement(Vec3.ZERO);

        FluidState water = Blocks.WATER.defaultBlockState().getFluidState();
        require(water.is(FluidTags.WATER), "water-run regression setup did not produce water fluid state");

        player.fallDistance = 0.0F;
        require(player.canStandOnFluid(water),
                "stationary player with Water Run could not stand on water");

        player.setDeltaMovement(0.12D, 0.0D, 0.0D);
        require(player.canStandOnFluid(water),
                "walking player with Water Run could not stay supported on water");

        player.setShiftKeyDown(true);
        require(!player.canStandOnFluid(water),
                "crouching player incorrectly retained water-surface support");
        player.setShiftKeyDown(false);

        player.fallDistance = 4.99F;
        require(player.canStandOnFluid(water),
                "fall below the five-block dive threshold incorrectly broke water support");
        player.fallDistance = 5.0F;
        require(!player.canStandOnFluid(water),
                "five-block-or-higher dive did not pass through the water surface");
        require(!player.canStandOnFluid(Blocks.LAVA.defaultBlockState().getFluidState()),
                "water traversal incorrectly allowed standing on lava");
        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void repairedAugmentDescriptionsMatchLiveBehaviorAndHideInternalTerms(GameTestHelper helper) {
        String[] augmentIds = {
                AugmentCatalog.REROLL_ID,
                id("return_to_sender"), id("portable_riptide"), id("retrace"), id("delayed_damage"),
                id("last_stand"), id("wall_slam"), id("aerial_combo"), id("temporary_platform"),
                id("tracking_arrow"), id("water_run")
        };
        for (String augmentId : augmentIds) {
            AugmentPresentationCatalog.Entry entry = AugmentPresentationCatalog.get(AugmentCatalog.require(augmentId));
            String text = (entry.summary() + " " + entry.detail()).toLowerCase(Locale.ROOT);
            for (String forbidden : new String[]{"root", "derived", "p0", "debug"}) {
                require(!text.contains(forbidden),
                        augmentId + " player-facing description leaked internal term " + forbidden + ": " + text);
            }
            require(!entry.summary().isBlank() && !entry.detail().isBlank(),
                    augmentId + " has an empty player-facing summary/detail");
        }

        String tracking = detail("tracking_arrow");
        require(tracking.contains("16") && tracking.contains("45")
                        && tracking.contains("4°") && tracking.contains("6°") && tracking.contains("8°")
                        && tracking.contains("철골렘") && tracking.contains("아군"),
                "tracking-arrow description does not state its live range/cone/turn/neutral-target policy");

        String wall = detail("wall_slam");
        require(wall.contains("1초") && wall.contains("30%") && wall.contains("60%") && wall.contains("100%")
                        && wall.contains("벽 바로 근처"),
                "wall-slam description does not match its forgiving live wall-contact window");

        String aerial = detail("aerial_combo");
        require(aerial.contains("두 번 점프가 없어도") && aerial.contains("같은 대상") && aerial.contains("공중 점프"),
                "aerial-combo description does not explain standalone repeated air-jump recharge");

        String waterRun = detail("water_run");
        require(waterRun.contains("걷") && waterRun.contains("웅크") && waterRun.contains("5블록")
                        && waterRun.contains("잠수"),
                "water-run description does not explain stand/walk/crouch/dive behavior");

        String returnToSender = detail("return_to_sender");
        require(returnToSender.contains("누적 10") && returnToSender.contains("7블록")
                        && returnToSender.contains("45°") && returnToSender.contains("60% / 80% / 100%"),
                "Return to Sender description does not match blocked-damage cone compression");

        String portableRiptide = detail("portable_riptide");
        require(portableRiptide.contains("소모되지") && portableRiptide.contains("회전"),
                "Portable Riptide description does not state unlimited bucket use and spin behavior");

        String lastStand = detail("last_stand");
        String delayedDamage = detail("delayed_damage");
        require(lastStand.contains("피해 지연") && lastStand.contains("모두 사라")
                        && delayedDamage.contains("모두 폐기") && delayedDamage.contains("새 예약"),
                "Last Stand / delayed-damage descriptions do not explain debt clearing and suppression");

        GameTestPlayers.succeed(helper);
    }

    private static String detail(String path) {
        return AugmentPresentationCatalog.get(AugmentCatalog.require(id(path))).detail();
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

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        GameTestPlayers.invoke(this, helper, method);
    }
}
