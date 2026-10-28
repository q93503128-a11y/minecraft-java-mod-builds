package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatTuning;
import dev.moonseungjun.augmentationsurvival.augment.movement.MovementAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.movement.MovementTuning;
import dev.moonseungjun.augmentationsurvival.augment.resource.ResourceHarvestService;
import dev.moonseungjun.augmentationsurvival.augment.resource.ResourceHarvestTuning;
import dev.moonseungjun.augmentationsurvival.augment.survival.SurvivalTuning;
import dev.moonseungjun.augmentationsurvival.augment.utility.UtilityAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.utility.UtilityAugmentTuning;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Focused server-runtime coverage for the twenty Common-tier augments. */
public final class CommonAugmentGameTests implements CustomTestMethodInvoker {
    private static final String PREFIX = AugmentationSurvival.MOD_ID + ":";

    @GameTest
    public void commonCombatModifiersRespectDamageContextAndMaxClamp(GameTestHelper helper) {
        ServerPlayer attacker = player(helper, 1, 1);
        LivingEntity target = helper.spawnMob(EntityTypes.ZOMBIE, 1, 2, 4).withNoFreeWill().spawn();
        ServerLevel level = (ServerLevel) attacker.level();
        DamageSource playerAttack = level.damageSources().playerAttack(attacker);
        float base = 10.0F;
        AugmentationPlayerState empty = AugmentationPlayerState.initial();
        AugmentationPlayerStateService.replace(attacker, empty);

        requireClose(
                CombatProvenanceContext.callAs(
                        CombatProvenance.ROOT_MELEE,
                        () -> CombatAugmentService.modifyOutgoingDamage(target, playerAttack, base)
                ),
                base,
                1.0E-6D,
                "unowned keen edge changed root melee damage"
        );
        requireClose(
                CombatProvenanceContext.callAs(
                        CombatProvenance.ROOT_PROJECTILE,
                        () -> CombatAugmentService.modifyOutgoingDamage(target, playerAttack, base)
                ),
                base,
                1.0E-6D,
                "unowned precise aim changed root projectile damage"
        );

        String keenEdge = id("keen_edge");
        AugmentDefinition keen = AugmentCatalog.require(keenEdge);
        AugmentationPlayerStateService.replace(attacker, empty.withAugmentLevel(keenEdge, keen.maxLevel()));
        double expectedKeen = base * (1.0D + CombatTuning.current().keenEdgePerLevel() * keen.maxLevel());
        requireClose(
                CombatProvenanceContext.callAs(
                        CombatProvenance.ROOT_MELEE,
                        () -> CombatAugmentService.modifyOutgoingDamage(target, playerAttack, base)
                ),
                expectedKeen,
                1.0E-6D,
                "keen edge max-level melee bonus diverged from tuning"
        );
        AugmentationPlayerStateService.replace(attacker, empty.withAugmentLevel(keenEdge, keen.maxLevel() + 50));
        requireClose(
                CombatProvenanceContext.callAs(
                        CombatProvenance.ROOT_MELEE,
                        () -> CombatAugmentService.modifyOutgoingDamage(target, playerAttack, base)
                ),
                expectedKeen,
                1.0E-6D,
                "keen edge exceeded canonical max-level clamp"
        );

        String preciseAim = id("precise_aim");
        AugmentDefinition precise = AugmentCatalog.require(preciseAim);
        AugmentationPlayerStateService.replace(attacker, empty.withAugmentLevel(preciseAim, precise.maxLevel()));
        double expectedPrecise = base
                * (1.0D + CombatTuning.current().preciseAimPerLevel() * precise.maxLevel());
        requireClose(
                CombatProvenanceContext.callAs(
                        CombatProvenance.ROOT_PROJECTILE,
                        () -> CombatAugmentService.modifyOutgoingDamage(target, playerAttack, base)
                ),
                expectedPrecise,
                1.0E-6D,
                "precise aim max-level projectile bonus diverged from tuning"
        );
        AugmentationPlayerStateService.replace(
                attacker, empty.withAugmentLevel(preciseAim, precise.maxLevel() + 50));
        requireClose(
                CombatProvenanceContext.callAs(
                        CombatProvenance.ROOT_PROJECTILE,
                        () -> CombatAugmentService.modifyOutgoingDamage(target, playerAttack, base)
                ),
                expectedPrecise,
                1.0E-6D,
                "precise aim exceeded canonical max-level clamp"
        );

        String toughSkin = id("tough_skin");
        AugmentDefinition tough = AugmentCatalog.require(toughSkin);
        AugmentationPlayerStateService.replace(attacker, empty.withAugmentLevel(toughSkin, tough.maxLevel()));
        requireClose(
                CombatAugmentService.modifyPostMitigationDamage(
                        attacker, level.damageSources().generic(), base),
                base * (1.0D - CombatTuning.current().toughSkinPerLevel() * tough.maxLevel()),
                1.0E-6D,
                "tough skin max-level reduction diverged from tuning"
        );

        String lightLanding = id("light_landing");
        AugmentDefinition landing = AugmentCatalog.require(lightLanding);
        AugmentationPlayerStateService.replace(
                attacker, empty.withAugmentLevel(lightLanding, landing.maxLevel() + 50));
        requireClose(
                CombatAugmentService.modifyPostMitigationDamage(
                        attacker, level.damageSources().fall(), base),
                base * (1.0D - CombatTuning.current().lightLandingPerLevel() * landing.maxLevel()),
                1.0E-6D,
                "light landing did not respect fall-only tuning and max clamp"
        );

        String strongLungs = id("strong_lungs");
        AugmentDefinition lungs = AugmentCatalog.require(strongLungs);
        AugmentationPlayerStateService.replace(
                attacker, empty.withAugmentLevel(strongLungs, lungs.maxLevel() + 50));
        requireClose(
                CombatAugmentService.modifyPostMitigationDamage(
                        attacker, level.damageSources().drown(), base),
                base * (1.0D - CombatTuning.current().strongLungsPerLevel() * lungs.maxLevel()),
                1.0E-6D,
                "strong lungs did not respect drowning-only tuning and max clamp"
        );

        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void webbedFeetMovementRuleRequiresWaterAndClampsLevel(GameTestHelper helper) {
        String webbedFeet = id("webbed_feet");
        AugmentDefinition definition = AugmentCatalog.require(webbedFeet);
        AugmentationPlayerState empty = AugmentationPlayerState.initial();
        AugmentationPlayerState max = empty.withAugmentLevel(webbedFeet, definition.maxLevel());
        AugmentationPlayerState overMax = empty.withAugmentLevel(webbedFeet, definition.maxLevel() + 50);
        double expected = MovementTuning.current().webbedFeetMovementPerLevel() * definition.maxLevel();

        requireClose(
                invokeWebbedFeetMovementBonus(empty, true),
                0.0D,
                1.0E-9D,
                "unowned webbed feet produced a water movement bonus"
        );
        requireClose(
                invokeWebbedFeetMovementBonus(max, false),
                0.0D,
                1.0E-9D,
                "webbed feet produced a movement bonus outside water"
        );
        requireClose(
                invokeWebbedFeetMovementBonus(max, true),
                expected,
                1.0E-9D,
                "webbed feet max-level water bonus diverged from movement tuning"
        );
        requireClose(
                invokeWebbedFeetMovementBonus(overMax, true),
                expected,
                1.0E-9D,
                "webbed feet exceeded canonical max-level clamp"
        );

        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 30)
    public void coldAdaptationReducesObservedFreezeBuildup(GameTestHelper helper) {
        ServerPlayer control = player(helper, 1, 1);
        ServerPlayer adapted = player(helper, 3, 1);
        String coldAdaptation = id("cold_adaptation");
        AugmentDefinition definition = AugmentCatalog.require(coldAdaptation);

        AugmentationPlayerStateService.replace(control, AugmentationPlayerState.initial());
        AugmentationPlayerStateService.replace(
                adapted,
                AugmentationPlayerState.initial().withAugmentLevel(coldAdaptation, definition.maxLevel())
        );
        control.setTicksFrozen(100);
        adapted.setTicksFrozen(100);

        GameTestPlayers.runAfterDelay(helper, 2, () -> {
            int controlFrozen = control.getTicksFrozen();
            int adaptedFrozen = adapted.getTicksFrozen();
            require(controlFrozen > 0, "freeze control unexpectedly cleared before comparison");
            require(adaptedFrozen < controlFrozen,
                    "cold adaptation did not reduce freeze buildup; control=" + controlFrozen
                            + ", adapted=" + adaptedFrozen);
            double reduction = SurvivalTuning.current().freezeReduction(definition.maxLevel());
            require(reduction > 0.0D && reduction < 1.0D,
                    "cold adaptation tuning is outside a valid fractional range");
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest
    public void fuelSaverUsesNearbyOwnershipAndCanonicalMaxClamp(GameTestHelper helper) {
        ServerPlayer player = player(helper, 1, 1);
        ServerLevel level = (ServerLevel) player.level();
        BlockPos machinePos = helper.absolutePos(new BlockPos(2, 2, 1));
        int baseDuration = 1000;
        AugmentationPlayerState empty = AugmentationPlayerState.initial();
        AugmentationPlayerStateService.replace(player, empty);
        require(UtilityAugmentService.modifyFuelBurnDuration(level, machinePos, baseDuration) == baseDuration,
                "unowned fuel saver changed burn duration");

        String fuelSaver = id("fuel_saver");
        AugmentDefinition definition = AugmentCatalog.require(fuelSaver);
        AugmentationPlayerStateService.replace(
                player, empty.withAugmentLevel(fuelSaver, definition.maxLevel()));
        int expected = (int) Math.round(
                baseDuration * (1.0D + UtilityAugmentTuning.current().fuelBurnTimeBonus(definition.maxLevel()))
        );
        require(UtilityAugmentService.modifyFuelBurnDuration(level, machinePos, baseDuration) == expected,
                "fuel saver max-level burn bonus diverged from tuning");

        AugmentationPlayerStateService.replace(
                player, empty.withAugmentLevel(fuelSaver, definition.maxLevel() + 50));
        require(UtilityAugmentService.modifyFuelBurnDuration(level, machinePos, baseDuration) == expected,
                "fuel saver exceeded canonical max-level clamp");
        GameTestPlayers.succeed(helper);
    }

    @GameTest
    public void cropBonusesUseOwnedLevelAndClampTheirProcChance(GameTestHelper helper) {
        ServerPlayer player = player(helper, 1, 1);
        ServerLevel level = (ServerLevel) player.level();
        BlockState matureWheat = Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7);
        List<ItemStack> baseDrops = List.of(new ItemStack(Items.WHEAT));
        AugmentationPlayerState empty = AugmentationPlayerState.initial();

        String bumperCrop = id("bumper_crop");
        AugmentDefinition bumper = AugmentCatalog.require(bumperCrop);
        double bumperThreshold = ResourceHarvestTuning.current().bumperCropChancePerLevel()
                * bumper.maxLevel();
        BlockPos bumperPos = helper.absolutePos(new BlockPos(2, 2, 1));
        AugmentationPlayerState bumperState = empty.withAugmentLevel(bumperCrop, bumper.maxLevel());
        AugmentationPlayerStateService.replace(player, bumperState);
        resetFirstRollInto(player.getRandom(), 0.0D, bumperThreshold);
        invokeCropBonuses(level, player, bumperPos, matureWheat, baseDrops, bumperState);
        require(countNearby(level, bumperPos, Items.WHEAT) == 1,
                "bumper crop forced proc did not create exactly one bonus crop product");
        clearNearbyItems(level, bumperPos);

        AugmentationPlayerState bumperOverMax = empty.withAugmentLevel(bumperCrop, bumper.maxLevel() + 50);
        AugmentationPlayerStateService.replace(player, bumperOverMax);
        resetFirstRollInto(player.getRandom(), bumperThreshold + 0.02D, Math.min(0.99D, bumperThreshold + 0.25D));
        invokeCropBonuses(level, player, bumperPos, matureWheat, baseDrops, bumperOverMax);
        require(countNearby(level, bumperPos, Items.WHEAT) == 0,
                "bumper crop chance exceeded its canonical max-level clamp");

        String seedSaver = id("seed_saver");
        AugmentDefinition seeds = AugmentCatalog.require(seedSaver);
        double seedThreshold = ResourceHarvestTuning.current().seedSaverChancePerLevel()
                * seeds.maxLevel();
        BlockPos seedPos = helper.absolutePos(new BlockPos(4, 2, 1));
        AugmentationPlayerState seedState = empty.withAugmentLevel(seedSaver, seeds.maxLevel());
        AugmentationPlayerStateService.replace(player, seedState);
        resetFirstRollInto(player.getRandom(), 0.0D, seedThreshold);
        invokeCropBonuses(level, player, seedPos, matureWheat, baseDrops, seedState);
        require(countNearby(level, seedPos, Items.WHEAT_SEEDS) == 1,
                "seed saver forced proc did not create exactly one bonus planting item");
        clearNearbyItems(level, seedPos);

        AugmentationPlayerState seedOverMax = empty.withAugmentLevel(seedSaver, seeds.maxLevel() + 50);
        AugmentationPlayerStateService.replace(player, seedOverMax);
        resetFirstRollInto(player.getRandom(), seedThreshold + 0.02D, Math.min(0.99D, seedThreshold + 0.25D));
        invokeCropBonuses(level, player, seedPos, matureWheat, baseDrops, seedOverMax);
        require(countNearby(level, seedPos, Items.WHEAT_SEEDS) == 0,
                "seed saver chance exceeded its canonical max-level clamp");

        GameTestPlayers.succeed(helper);
    }

    private static double invokeWebbedFeetMovementBonus(AugmentationPlayerState state, boolean inWater) {
        try {
            Method method = MovementAugmentService.class.getDeclaredMethod(
                    "webbedFeetMovementBonus",
                    AugmentationPlayerState.class,
                    boolean.class
            );
            method.setAccessible(true);
            return ((Number) method.invoke(null, state, inWater)).doubleValue();
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("unable to exercise webbed feet movement runtime rule", exception);
        }
    }

    private static void invokeCropBonuses(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            BlockState state,
            List<ItemStack> baseDrops,
            AugmentationPlayerState augments
    ) {
        try {
            Method method = ResourceHarvestService.class.getDeclaredMethod(
                    "applyCropBonusDrops",
                    ServerLevel.class,
                    ServerPlayer.class,
                    BlockPos.class,
                    BlockState.class,
                    List.class,
                    AugmentationPlayerState.class
            );
            method.setAccessible(true);
            method.invoke(null, level, player, pos, state, baseDrops, augments);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("unable to exercise crop bonus runtime", exception);
        }
    }

    private static void resetFirstRollInto(RandomSource random, double minimumInclusive, double maximumExclusive) {
        require(maximumExclusive > minimumInclusive,
                "invalid deterministic RNG search range: " + minimumInclusive + ".." + maximumExclusive);
        for (long seed = 0; seed < 100_000L; seed++) {
            random.setSeed(seed);
            double roll = random.nextDouble();
            if (roll >= minimumInclusive && roll < maximumExclusive) {
                random.setSeed(seed);
                return;
            }
        }
        throw new AssertionError(
                "failed to find deterministic RNG seed for range "
                        + minimumInclusive + ".." + maximumExclusive
        );
    }

    private static int countNearby(ServerLevel level, BlockPos pos, Item item) {
        return level.getEntitiesOfClass(
                ItemEntity.class,
                new AABB(pos).inflate(1.5D),
                entity -> entity.isAlive() && entity.getItem().is(item)
        ).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
    }

    private static void clearNearbyItems(ServerLevel level, BlockPos pos) {
        for (ItemEntity entity : level.getEntitiesOfClass(
                ItemEntity.class,
                new AABB(pos).inflate(1.5D),
                ItemEntity::isAlive
        )) {
            entity.discard();
        }
    }

    private static ServerPlayer player(GameTestHelper helper, int x, int z) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos anchor = helper.absolutePos(new BlockPos(x, 2, z));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        player.setHealth(player.getMaxHealth());
        return player;
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
