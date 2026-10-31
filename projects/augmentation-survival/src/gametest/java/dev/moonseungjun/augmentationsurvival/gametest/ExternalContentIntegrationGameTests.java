package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.core.AugmentCoreItems;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import dev.moonseungjun.augmentationsurvival.core.BossCoreRewardCatalog;
import dev.moonseungjun.augmentationsurvival.core.CoreDistributionCatalog;
import dev.moonseungjun.augmentationsurvival.core.CoreSourceResolver;
import dev.moonseungjun.augmentationsurvival.integration.EntityCompatibilityProfileService;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;

/** Full-pack regressions for external structures, dynamic dungeon enemies, and boss core rewards. */
public final class ExternalContentIntegrationGameTests implements CustomTestMethodInvoker {
    private static final String MCE_DUNGEON_ENEMY =
            "de.fuballer.mcendgame.main.accessor.LivingEntityDungeonEnemyAccessor";
    private static final String MCE_ELITE =
            "de.fuballer.mcendgame.main.accessor.LivingEntityEliteAccessor";
    private static final String MCE_LOOT_GOBLIN =
            "de.fuballer.mcendgame.main.accessor.LivingEntityLootGoblinAccessor";
    private static final String MCE_DUNGEON_BOSS =
            "de.fuballer.mcendgame.main.accessor.MobEntityDungeonBossAccessor";

    @GameTest(maxTicks = 20)
    public void chronodawnProgressionStructuresHaveAuthoredCoreProfiles(GameTestHelper helper) {
        Map<String, ExpectedStructure> expected = new LinkedHashMap<>();
        expected.put("chronodawn:ancient_ruins", new ExpectedStructure("routine_exploration", 1.00D));
        expected.put("chronodawn:forgotten_library", new ExpectedStructure("dangerous_mid", 0.75D));
        expected.put("chronodawn:desert_clock_tower", new ExpectedStructure("dangerous_high", 0.55D));
        expected.put("chronodawn:guardian_vault", new ExpectedStructure("dangerous_high", 0.55D));
        expected.put("chronodawn:clockwork_depths", new ExpectedStructure("dangerous_high", 0.55D));
        expected.put("chronodawn:phantom_catacombs", new ExpectedStructure("dangerous_high", 0.55D));
        expected.put("chronodawn:entropy_crypt", new ExpectedStructure("dangerous_high", 0.55D));
        expected.put("chronodawn:master_clock", new ExpectedStructure("dangerous_high", 0.40D));

        for (Map.Entry<String, ExpectedStructure> entry : expected.entrySet()) {
            CoreSourceResolver.StructureResolution resolved =
                    CoreSourceResolver.resolveStructure(Identifier.parse(entry.getKey()));
            require(resolved.hasDistribution(), "Missing structure core profile for " + entry.getKey());
            require(resolved.distributionProfile().equals(entry.getValue().profile()),
                    "Wrong structure core profile for " + entry.getKey() + ": " + resolved);
            require(Math.abs(resolved.coreRollMultiplier() - entry.getValue().multiplier()) < 0.000001D,
                    "Wrong structure core multiplier for " + entry.getKey() + ": " + resolved);
        }
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 20)
    public void externalProgressionHostilesAndBossProfilesAreAuthored(GameTestHelper helper) {
        CoreDistributionCatalog.MobProfile progression =
                CoreDistributionCatalog.current().mobProfiles().get("progression_hostile");
        require(progression != null, "progression_hostile profile is missing");
        require(Math.abs(progression.coreRollChance() - 0.008D) < 0.000001D,
                "progression_hostile chance drifted: " + progression.coreRollChance());

        CoreSourceResolver.HostileResolution chrono = CoreSourceResolver.resolveHostile(
                Identifier.parse("chronodawn:temporal_wraith"), true
        );
        CoreSourceResolver.HostileResolution worldBossMob = CoreSourceResolver.resolveHostile(
                Identifier.parse("worldbosses:realm_test_hostile"), true
        );
        require(chrono.kind() == CoreSourceResolver.HostileResolution.Kind.MOB
                        && "progression_hostile".equals(chrono.profileId()),
                "Chrono Dawn hostile did not use progression_hostile: " + chrono);
        require(worldBossMob.kind() == CoreSourceResolver.HostileResolution.Kind.MOB
                        && "progression_hostile".equals(worldBossMob.profileId()),
                "World Bosses hostile did not use progression_hostile: " + worldBossMob);

        String[] compatibilityBossProfiles = {
                "chronodawn_time_guardian", "chronodawn_chronos_warden",
                "chronodawn_clockwork_colossus", "chronodawn_entropy_keeper",
                "chronodawn_temporal_phantom", "chronodawn_time_tyrant",
                "worldbosses_sylvaron", "worldbosses_aureon", "worldbosses_nekhraz",
                "worldbosses_varkhul", "worldbosses_morvath", "worldbosses_elyndra",
                "worldbosses_astrael", "worldbosses_shadow_king", "worldbosses_dragon_mother",
                "worldbosses_storm_griffin", "worldbosses_shadow_weaver",
                "mcendgame_dungeon_boss"
        };
        for (String profile : compatibilityBossProfiles) {
            BossCoreRewardCatalog.Assignment assignment = BossCoreRewardCatalog.compatibilityAssignment(profile)
                    .orElseThrow(() -> new AssertionError("Missing external boss reward assignment: " + profile));
            require("boss_major".equals(assignment.rewardBand()),
                    "External boss reward band drifted for " + profile + ": " + assignment);
        }
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 30)
    public void mcendgameRuntimeFlagsPromoteDungeonEnemiesAndElites(GameTestHelper helper) throws Exception {
        if (!FabricLoader.getInstance().isModLoaded("mcendgame")) {
            GameTestPlayers.succeed(helper);
            return;
        }

        LivingEntity dungeonEnemy = helper.spawnMob(EntityTypes.ZOMBIE, 2, 2, 2).withNoFreeWill().spawn();
        setFlag(dungeonEnemy, MCE_DUNGEON_ENEMY, "mcendgame$setDungeonEnemy", true);
        CoreSourceResolver.HostileResolution dungeonResolution = CoreSourceResolver.resolveHostile(dungeonEnemy);
        require(dungeonResolution.kind() == CoreSourceResolver.HostileResolution.Kind.MOB
                        && "special_hostile".equals(dungeonResolution.profileId()),
                "MCE dungeon enemy did not promote to special_hostile: " + dungeonResolution);

        LivingEntity elite = helper.spawnMob(EntityTypes.ZOMBIE, 4, 2, 2).withNoFreeWill().spawn();
        setFlag(elite, MCE_DUNGEON_ENEMY, "mcendgame$setDungeonEnemy", true);
        setFlag(elite, MCE_ELITE, "mcendgame$setElite", true);
        CoreSourceResolver.HostileResolution eliteResolution = CoreSourceResolver.resolveHostile(elite);
        require(eliteResolution.kind() == CoreSourceResolver.HostileResolution.Kind.MOB
                        && "elite_hostile".equals(eliteResolution.profileId()),
                "MCE elite did not override dungeon-enemy profile: " + eliteResolution);

        LivingEntity lootGoblin = helper.spawnMob(EntityTypes.ZOMBIE, 6, 2, 2).withNoFreeWill().spawn();
        setFlag(lootGoblin, MCE_LOOT_GOBLIN, "mcendgame$setLootGoblin", true);
        CoreSourceResolver.HostileResolution goblinResolution = CoreSourceResolver.resolveHostile(lootGoblin);
        require(goblinResolution.kind() == CoreSourceResolver.HostileResolution.Kind.MOB
                        && "elite_hostile".equals(goblinResolution.profileId()),
                "MCE loot goblin did not use elite_hostile: " + goblinResolution);

        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 80)
    public void mcendgameDynamicBossGetsPersonalMajorCoresWithoutGenericMobRoll(GameTestHelper helper) throws Exception {
        if (!FabricLoader.getInstance().isModLoaded("mcendgame")) {
            GameTestPlayers.succeed(helper);
            return;
        }

        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos playerPos = helper.absolutePos(new BlockPos(2, 2, 2));
        player.setPos(playerPos.getX() + 0.5D, playerPos.getY(), playerPos.getZ() + 0.5D);

        LivingEntity firstBoss = helper.spawnMob(EntityTypes.ZOMBIE, 2, 2, 3).withNoFreeWill().spawn();
        // Prove a pre-promotion false query does not poison the dynamic compatibility cache.
        require(!EntityCompatibilityProfileService.isBoss(firstBoss),
                "fresh ordinary zombie unexpectedly classified as an external boss");
        setFlag(firstBoss, MCE_DUNGEON_ENEMY, "mcendgame$setDungeonEnemy", true);
        setFlag(firstBoss, MCE_DUNGEON_BOSS, "mcendgame$setDungeonBoss", true);
        require(EntityCompatibilityProfileService.resolve(firstBoss)
                        .map(profile -> "mcendgame_dungeon_boss".equals(profile.id()))
                        .orElse(false),
                "MCE boss promotion was hidden by stale dynamic compatibility state");
        require(CoreSourceResolver.resolveHostile(firstBoss).kind()
                        == CoreSourceResolver.HostileResolution.Kind.BOSS_SUPPRESSED,
                "MCE boss was still eligible for generic hostile core rolls");

        firstBoss.setHealth(1.0F);
        firstBoss.invulnerableTime = 0;
        player.attack(firstBoss);

        GameTestPlayers.runAfterDelay(helper, 3, () -> {
            int afterFirst = coreCount(player);
            require(afterFirst == 2,
                    "first MCE dungeon boss should grant exactly 2 personal major cores, got " + afterFirst);
            require(lowTierCoreCount(player) == 0,
                    "boss-major reward leaked common/uncommon/rare cores");

            LivingEntity repeatBoss = helper.spawnMob(EntityTypes.ZOMBIE, 2, 2, 3).withNoFreeWill().spawn();
            try {
                setFlag(repeatBoss, MCE_DUNGEON_ENEMY, "mcendgame$setDungeonEnemy", true);
                setFlag(repeatBoss, MCE_DUNGEON_BOSS, "mcendgame$setDungeonBoss", true);
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError("Could not flag repeat MCE boss", exception);
            }
            repeatBoss.setHealth(1.0F);
            repeatBoss.invulnerableTime = 0;
            player.attack(repeatBoss);

            GameTestPlayers.runAfterDelay(helper, 3, () -> {
                int afterRepeat = coreCount(player);
                require(afterRepeat == 3,
                        "repeat MCE dungeon boss should add exactly 1 core after first-kill bonus, got " + afterRepeat);
                GameTestPlayers.succeed(helper);
            });
        });
    }

    private static void setFlag(
            LivingEntity entity,
            String accessorName,
            String setterName,
            boolean value
    ) throws ReflectiveOperationException {
        Class<?> accessor = Class.forName(accessorName, false, ExternalContentIntegrationGameTests.class.getClassLoader());
        require(accessor.isInstance(entity),
                "Entity does not implement expected MCE accessor " + accessorName + ": " + entity.getType());
        Method setter = accessor.getMethod(setterName, boolean.class);
        setter.invoke(entity, value);
    }

    private static int coreCount(ServerPlayer player) {
        int total = 0;
        for (AugmentTier tier : AugmentTier.values()) {
            total += player.getInventory().countItem(AugmentCoreItems.item(tier));
        }
        return total;
    }

    private static int lowTierCoreCount(ServerPlayer player) {
        return player.getInventory().countItem(AugmentCoreItems.COMMON_CORE)
                + player.getInventory().countItem(AugmentCoreItems.UNCOMMON_CORE)
                + player.getInventory().countItem(AugmentCoreItems.RARE_CORE);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private record ExpectedStructure(String profile, double multiplier) {
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        GameTestPlayers.invoke(this, helper, method);
    }
}
