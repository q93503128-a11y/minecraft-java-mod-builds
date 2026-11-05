package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.augment.active.AuthorityPowerCatalog;
import dev.moonseungjun.augmentationsurvival.augment.active.AuthorityTheftService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/** Regression contracts for server-owned mythic authority collection. */
public final class AuthoritySignatureGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void canonicalAuthoritySourcesHaveDistinctSupportedIdentities(GameTestHelper helper) {
        require(AuthorityPowerCatalog.ENTRIES.size() == 26,
                "Expected 26 bound boss signatures (four vanilla + six Chrono Dawn + eleven World Bosses + five MCEndgame)");
        var ids = new HashSet<String>();
        for (var entry : AuthorityPowerCatalog.ENTRIES) {
            require(ids.add(entry.id()), "duplicate authority identity " + entry.id());
            require(AuthorityTheftService.supports(entry.id()), "unbound authority " + entry.id());
            require(!entry.effect().isBlank(), "undocumented authority effect " + entry.id());
        }
        require(ids.contains("vanilla_warden") && ids.contains("vanilla_elder_guardian"),
                "Warden and Elder Guardian signatures must both be represented");
        helper.succeed();
    }

    @GameTest
    public void disablingAllAuthorityPowersCannotSpendTheGlobalCooldown(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        String authority = AuthorityTheftService.AUTHORITY_THEFT_ID;
        String wither = "vanilla_wither";
        var disabled = AugmentationPlayerState.initial()
                .withAugmentLevel(authority, 1)
                .withStolenAuthority(wither)
                .withPersistentInt(AuthorityTheftService.DISABLED_PREFIX + wither, 1);
        AugmentationPlayerStateService.replace(player, disabled);
        require(!AuthorityTheftService.isEnabled(AugmentationPlayerStateService.state(player), wither),
                "disabled authority incorrectly considered enabled");
        require(!AuthorityTheftService.tryRelease(player),
                "disabled authority incorrectly started a release sequence");
        String cd = authority + "#release";
        require(AugmentationPlayerStateService.state(player).cooldownReadyTick(cd) <= player.level().getGameTime(),
                "disabled authority consumed its 120-second cooldown");
        AugmentationPlayerStateService.replace(player,
                AugmentationPlayerStateService.state(player)
                        .withPersistentInt(AuthorityTheftService.DISABLED_PREFIX + wither, 0));
        require(AuthorityTheftService.isEnabled(AugmentationPlayerStateService.state(player), wither),
                "re-enabled authority not selectable");
        require(AuthorityTheftService.tryRelease(player),
                "re-enabled authority failed to begin a server-owned release");
        require(AugmentationPlayerStateService.state(player).cooldownReadyTick(cd)
                        >= player.level().getGameTime() + 120 * 20 - 1,
                "enabled release did not reserve the canonical 120-second cooldown");
        GameTestPlayers.succeed(helper);
    }


    @GameTest(maxTicks = 80)
    public void aureonCageDamagesHostilesWithoutHarmingNearbyAnimals(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D);
        LivingEntity zombie = helper.spawnMob(EntityTypes.ZOMBIE, 3, 2, 2).withNoFreeWill().spawn();
        LivingEntity cow = helper.spawnMob(EntityTypes.COW, 3, 2, 4).withNoFreeWill().spawn();
        float zombieHp = zombie.getHealth();
        float cowHp = cow.getHealth();
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(AuthorityTheftService.AUTHORITY_THEFT_ID, 1)
                .withStolenAuthority("worldbosses_aureon"));
        require(AuthorityTheftService.tryRelease(player), "Aureon authority did not activate");
        GameTestPlayers.runAfterDelay(helper, 45, () -> {
            require(zombie.getHealth() < zombieHp, "Thunder Cage did not damage the hostile target");
            require(Math.abs(cow.getHealth() - cowHp) < 0.001F,
                    "Thunder Cage harmed a nearby neutral animal");
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest(maxTicks = 80)
    public void astraelNovaEventuallyDamagesHostileAfterScheduledCast(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D);
        LivingEntity zombie = helper.spawnMob(EntityTypes.ZOMBIE, 3, 2, 3).withNoFreeWill().spawn();
        float before = zombie.getHealth();
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(AuthorityTheftService.AUTHORITY_THEFT_ID, 1)
                .withStolenAuthority("worldbosses_astrael"));
        require(AuthorityTheftService.tryRelease(player), "Astrael authority did not activate");
        // Initial mob health can change during the first GameTest ticks when threat-scaled
        // attributes are reconciled. Damage timing is enforced by the production pattern's
        // firstPulseDelay=26; assert the real delayed hit below without a false positive.
        GameTestPlayers.runAfterDelay(helper, 42, () -> {
            require(zombie.getHealth() < before, "Cataclysm Nova never dealt delayed damage");
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest(maxTicks = 75)
    public void sylvaronRootsBindEnemiesWithoutPoisoningNeutralAnimals(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        // Creepers can receive Poison. Undead zombies are poison-immune and make this test invalid.
        LivingEntity creeper = helper.spawnMob(EntityTypes.CREEPER, 3, 2, 2).withNoFreeWill().spawn();
        LivingEntity cow = helper.spawnMob(EntityTypes.COW, 3, 2, 3).withNoFreeWill().spawn();
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(AuthorityTheftService.AUTHORITY_THEFT_ID, 1)
                .withStolenAuthority("worldbosses_sylvaron"));
        require(AuthorityTheftService.tryRelease(player), "Sylvaron authority failed to start");
        GameTestPlayers.runAfterDelay(helper, 38, () -> {
            require(creeper.hasEffect(MobEffects.POISON), "Sylvaron spore field did not poison its hostile target");
            require(!cow.hasEffect(MobEffects.POISON), "Sylvaron poisoned a neutral animal");
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest(maxTicks = 85)
    public void morvathFearSoulDrainPreservesNeutralAnimalHealth(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        LivingEntity zombie = helper.spawnMob(EntityTypes.ZOMBIE, 3, 2, 2).withNoFreeWill().spawn();
        LivingEntity cow = helper.spawnMob(EntityTypes.COW, 3, 2, 4).withNoFreeWill().spawn();
        float cowStart = cow.getHealth();
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(AuthorityTheftService.AUTHORITY_THEFT_ID, 1)
                .withStolenAuthority("worldbosses_morvath"));
        require(AuthorityTheftService.tryRelease(player), "Morvath authority failed to start");
        GameTestPlayers.runAfterDelay(helper, 55, () -> {
            require(zombie.hasEffect(MobEffects.WEAKNESS), "Fear Pulse did not weaken an enemy");
            require(Math.abs(cow.getHealth() - cowStart) < 0.001F,
                    "Morvath soul drain harmed a neutral animal");
            GameTestPlayers.succeed(helper);
        });
    }


    @GameTest(maxTicks = 85)
    public void stormGriffinMarkStrikesOnlyHostileTargets(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        LivingEntity zombie = helper.spawnMob(EntityTypes.ZOMBIE, 3, 2, 2).withNoFreeWill().spawn();
        LivingEntity cow = helper.spawnMob(EntityTypes.COW, 3, 2, 4).withNoFreeWill().spawn();
        float enemyHp = zombie.getHealth();
        float animalHp = cow.getHealth();
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(AuthorityTheftService.AUTHORITY_THEFT_ID, 1)
                .withStolenAuthority("worldbosses_storm_griffin"));
        require(AuthorityTheftService.tryRelease(player), "Storm Griffin authority did not activate");
        GameTestPlayers.runAfterDelay(helper, 45, () -> {
            require(zombie.getHealth() < enemyHp, "Storm Griffin marked lightning did not damage its enemy");
            require(Math.abs(cow.getHealth() - animalHp) < 0.001F,
                    "Storm Griffin lightning harmed a neutral cow");
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest(maxTicks = 90)
    public void shadowKingDelayedNovaSpreadsDarknessButNotToNeutralAnimals(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        LivingEntity zombie = helper.spawnMob(EntityTypes.ZOMBIE, 3, 2, 2).withNoFreeWill().spawn();
        LivingEntity cow = helper.spawnMob(EntityTypes.COW, 3, 2, 4).withNoFreeWill().spawn();
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(AuthorityTheftService.AUTHORITY_THEFT_ID, 1)
                .withStolenAuthority("worldbosses_shadow_king"));
        require(AuthorityTheftService.tryRelease(player), "Shadow King authority did not activate");
        GameTestPlayers.runAfterDelay(helper, 55, () -> {
            require(zombie.hasEffect(MobEffects.DARKNESS), "Shadow King's darkness nova did not activate");
            require(!cow.hasEffect(MobEffects.DARKNESS), "Shadow King's nova affected a neutral cow");
            GameTestPlayers.succeed(helper);
        });
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        GameTestPlayers.invoke(this, helper, method);
    }
}
