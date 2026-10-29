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

/** Bundle-four regressions that enter the same arrow/player boundaries used by live Minecraft. */
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

        // Enter through AbstractArrow.tick itself. The production mixin must initialize and steer it.
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

        // A descending trajectory must keep tracking instead of only steering at spawn/ricochet time.
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
    public void waterRunUsesActualPlayerTicksAndFluidStandBoundaryThenReleases(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(id("water_run"), 1)
        );
        BlockPos anchor = helper.absolutePos(new BlockPos(3, 4, 3));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        player.setOnGround(false);
        player.setSprinting(true);
        player.setDeltaMovement(0.18D, 0.0D, 0.0D);

        FluidState water = Blocks.WATER.defaultBlockState().getFluidState();
        require(water.is(FluidTags.WATER), "water-run regression setup did not produce water fluid state");

        // Player.tick is the production entry that records sprint intent; canStandOnFluid is the
        // vanilla fluid collision boundary modified by Water Run.
        player.tick();
        require(player.canStandOnFluid(water),
                "sprinting player with real horizontal travel could not stand on water after an actual player tick");

        player.setSprinting(false);
        player.setDeltaMovement(0.18D, 0.0D, 0.0D);
        require(player.canStandOnFluid(water),
                "water run did not bridge the intended short sprint-edge grace");

        GameTestPlayers.runAfterDelay(helper, 4L, () -> {
            player.setDeltaMovement(0.18D, 0.0D, 0.0D);
            require(!player.canStandOnFluid(water),
                    "water run continued from residual velocity after sprint intent and grace ended");

            player.setSprinting(true);
            player.setShiftKeyDown(true);
            player.tick();
            require(!player.canStandOnFluid(water),
                    "crouching player incorrectly retained water-run support");
            require(!player.canStandOnFluid(Blocks.LAVA.defaultBlockState().getFluidState()),
                    "water run incorrectly allowed standing on lava");
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest
    public void repairedAugmentDescriptionsMatchLiveBehaviorAndHideInternalTerms(GameTestHelper helper) {
        String[] ids = {
                "reroll", "return_to_sender", "portable_riptide", "retrace", "delayed_damage",
                "last_stand", "wall_slam", "aerial_combo", "temporary_platform", "tracking_arrow", "water_run"
        };
        for (String path : ids) {
            AugmentPresentationCatalog.Entry entry = AugmentPresentationCatalog.get(AugmentCatalog.require(id(path)));
            String text = (entry.summary() + " " + entry.detail()).toLowerCase(Locale.ROOT);
            for (String forbidden : new String[]{"root", "derived", "p0", "debug"}) {
                require(!text.contains(forbidden),
                        path + " player-facing description leaked internal term " + forbidden + ": " + text);
            }
            require(!entry.summary().isBlank() && !entry.detail().isBlank(),
                    path + " has an empty player-facing summary/detail");
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
        require(waterRun.contains("질주") && waterRun.contains("수평 이동") && waterRun.contains("일반 수영"),
                "water-run description does not explain live entry/continuation/release behavior");

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
