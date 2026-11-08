package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.core.AugmentCoreItems;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import dev.moonseungjun.augmentationsurvival.core.BossCoreRewardCatalog;
import dev.moonseungjun.augmentationsurvival.core.CoreDistributionCatalog;
import dev.moonseungjun.augmentationsurvival.core.CoreSourceResolver;
import dev.moonseungjun.augmentationsurvival.core.StructureCoreRewardService;
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
    public void shrineRewardLootTablesAndTierProfilesAreRecognized(GameTestHelper helper) {
        for (int tier = 1; tier <= 5; tier++) {
            String id = "nova_structures:shrine_combat_tier_" + tier;
            CoreSourceResolver.StructureResolution shrine =
                    CoreSourceResolver.resolveStructure(Identifier.parse(id));
            String expectedProfile = tier <= 2 ? "dangerous_mid" : "dangerous_high";
            double expectedMultiplier = switch (tier) {
                case 1, 4 -> 1.0D;
                case 2 -> 1.1D;
                case 3 -> 0.95D;
                default -> 1.15D;
            };
            require(shrine.hasDistribution() && expectedProfile.equals(shrine.distributionProfile())
                            && Math.abs(shrine.coreRollMultiplier() - expectedMultiplier) < 0.000001D,
                    "Shrine core reward tier mapping drifted: " + id + " = " + shrine);
        }
        for (String key : new String[] {
                "minecraft:chests/trial_chambers/reward",
                "nova_structures:chests/shrine_vault",
                "nova_structures:vaults/shrine_reward",
                "nova_structures:rewards/shrine_vault"
        }) {
            require(StructureCoreRewardService.isStructureRewardTable(Identifier.parse(key)),
                    "Natural vault reward table was not eligible: " + key);
        }
        require(!StructureCoreRewardService.isStructureRewardTable(Identifier.parse("minecraft:entities/zombie")),
                "Mob loot must not be treated as structure treasure");
        require(!StructureCoreRewardService.isStructureRewardTable(Identifier.parse("minecraft:gameplay/fishing")),
                "General fishing loot must not be treated as a vault");
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 20)
    public void coreAcquisitionBalanceAndTreasureFishingProfileAreLoaded(GameTestHelper helper) {
        CoreDistributionCatalog.Catalog catalog = CoreDistributionCatalog.current();
        Map<String, Double> structureChances = Map.of(
                "routine_exploration", 0.40D,
                "dangerous_mid", 0.55D,
                "dangerous_high", 0.70D,
                "pinnacle_cache", 0.85D
        );
        for (Map.Entry<String, Double> entry : structureChances.entrySet()) {
            CoreDistributionCatalog.DistributionTemplate profile = catalog.distributionTemplates().get(entry.getKey());
            require(profile != null && Math.abs(profile.coreRollChance() - entry.getValue()) < 0.000001D,
                    "Structure core chance drifted: " + entry.getKey());
        }
        require(Math.abs(catalog.distributionTemplates().get("routine_exploration")
                        .weights().get(AugmentTier.RARE) - 0.05D) < 0.000001D,
                "Routine structure rare core weight drifted");
        Map<String, Double> hostileChances = Map.of(
                "normal_hostile", 0.035D, "progression_hostile", 0.12D,
                "special_hostile", 0.20D, "elite_hostile", 0.42D
        );
        for (Map.Entry<String, Double> entry : hostileChances.entrySet()) {
            CoreDistributionCatalog.MobProfile profile = catalog.mobProfiles().get(entry.getKey());
            require(profile != null && Math.abs(profile.coreRollChance() - entry.getValue()) < 0.000001D,
                    "Hostile core chance drifted: " + entry.getKey());
        }
        for (String event : new String[] {"raid_victory", "elder_guardian"}) {
            CoreDistributionCatalog.EventProfile profile = catalog.eventProfiles().get(event);
            require(profile != null && profile.guaranteedCoreCount() == 2
                            && Math.abs(profile.weights().get(AugmentTier.EPIC) - 0.10D) < 0.000001D,
                    "Event core epic weight drifted: " + event);
        }
        CoreDistributionCatalog.EventProfile warden = catalog.eventProfiles().get("warden");
        require(warden != null && warden.guaranteedCoreCount() == 2
                        && Math.abs(warden.weights().get(AugmentTier.LEGENDARY) - 0.10D) < 0.000001D
                        && Math.abs(warden.weights().get(AugmentTier.EPIC) - 0.40D) < 0.000001D,
                "Warden core reward weights drifted");
        CoreDistributionCatalog.FishingProfile fishing = catalog.fishingProfile();
        require(fishing != null && "treasure_fishing".equals(fishing.id())
                        && "successful_treasure_category_catch".equals(fishing.trigger())
                        && Math.abs(fishing.coreRollChance() - 0.30D) < 0.000001D
                        && Math.abs(fishing.weights().get(AugmentTier.RARE) - 0.05D) < 0.000001D
                        && Math.abs(fishing.weights().get(AugmentTier.UNCOMMON) - 0.25D) < 0.000001D,
                "Treasure fishing core profile was not bound correctly");
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 20)
    public void coreAttunementBoostsOnlyMobCoreChanceByTwentyPercentPerLevel(GameTestHelper helper) {
        var catalog = CoreDistributionCatalog.current();
        var tuning = catalog.mobCoreRateAugment();
        require(tuning.augmentId().equals("augmentation_survival:core_attunement"),
                "Core attunement source ID drifted");
        require(AugmentCatalog.require(tuning.augmentId()).maxLevel() == 5,
                "Core attunement must have five levels");
        require(Math.abs(tuning.bonusPerLevel() - 0.20D) < 0.000001D,
                "Core attunement per-level bonus drifted");
        for (var profile : catalog.mobProfiles().values()) {
            for (int level = 0; level <= 5; level++) {
                double expected = Math.min(1.0D, profile.coreRollChance() * (1.0D + 0.20D * level));
                require(Math.abs(CoreDistributionCatalog.effectiveMobCoreChance(profile.id(), level)
                        - expected) < 0.000001D,
                        "Mob core attunement scaling drifted: " + profile.id() + " Lv." + level);
            }
            require(Math.abs(CoreDistributionCatalog.effectiveMobCoreChance(profile.id(), -100)
                    - profile.coreRollChance()) < 0.000001D,
                    "Negative augment level must not reduce base core chances");
            require(Math.abs(CoreDistributionCatalog.effectiveMobCoreChance(profile.id(), 100)
                    - Math.min(1.0D, profile.coreRollChance() * 2.0D)) < 0.000001D,
                    "Core attunement must cap at five levels");
        }
        require(catalog.eventProfiles().get("raid_victory").guaranteedCoreCount() == 2,
                "Raid reward was affected by core attunement");
        require(catalog.eventProfiles().get("warden").guaranteedCoreCount() == 2,
                "Warden guaranteed reward was affected by core attunement");
        require(Math.abs(catalog.fishingProfile().coreRollChance() - 0.30D) < 0.000001D,
                "Fishing rate was affected by core attunement");
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 20)
    public void externalProgressionHostilesAndBossProfilesAreAuthored(GameTestHelper helper) {
        CoreDistributionCatalog.MobProfile progression =
                CoreDistributionCatalog.current().mobProfiles().get("progression_hostile");
        require(progression != null, "progression_hostile profile is missing");
        require(Math.abs(progression.coreRollChance() - 0.12D) < 0.000001D,
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

        for (var band : BossCoreRewardCatalog.current().rewardBands().values()) {
            require(band.guaranteedCoreCount() == 2 && band.firstKillBonusCoreCount() == 1,
                    "Boss core reward count drifted: " + band.id());
        }
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

        LivingEntity firstBoss = helper.spawnMob(EntityTypes.ZOMBIE, 2, 2, 3).spawn();
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

        // This test owns reward routing, not MCE's level-scaled combat mitigation. Drive the same
        // vanilla death boundary directly with a player-attributed source so AFTER_DEATH and the
        // external boss profile are both exercised without depending on dungeon combat balance.
        firstBoss.setHealth(0.0F);
        firstBoss.die(helper.getLevel().damageSources().playerAttack(player));

        GameTestPlayers.runAfterDelay(helper, 3, () -> {
            require(!firstBoss.isAlive(), "first MCE dungeon boss did not remain dead after die() boundary");
            int afterFirst = coreCount(player);
            require(afterFirst == 2,
                    "first MCE dungeon boss should grant exactly 2 personal major cores, got " + afterFirst);
            require(lowTierCoreCount(player) == 0,
                    "boss-major reward leaked common/uncommon/rare cores");

            LivingEntity repeatBoss = helper.spawnMob(EntityTypes.ZOMBIE, 2, 2, 3).spawn();
            try {
                setFlag(repeatBoss, MCE_DUNGEON_ENEMY, "mcendgame$setDungeonEnemy", true);
                setFlag(repeatBoss, MCE_DUNGEON_BOSS, "mcendgame$setDungeonBoss", true);
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError("Could not flag repeat MCE boss", exception);
            }
            require(EntityCompatibilityProfileService.isBoss(repeatBoss),
                    "repeat MCE dungeon boss was not recognized after runtime promotion");
            repeatBoss.setHealth(0.0F);
            repeatBoss.die(helper.getLevel().damageSources().playerAttack(player));

            GameTestPlayers.runAfterDelay(helper, 3, () -> {
                require(!repeatBoss.isAlive(), "repeat MCE dungeon boss did not remain dead after die() boundary");
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
