package kr.moonseungjun.turnboundre.world;

import kr.moonseungjun.turnboundre.battle.BattlePreparationBonus;
import kr.moonseungjun.turnboundre.data.CharacterDefinition;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.EncounterDefinition;
import kr.moonseungjun.turnboundre.data.EquipmentDefinition;
import kr.moonseungjun.turnboundre.data.RewardTableDefinition;
import kr.moonseungjun.turnboundre.fixtures.ProductionDefinitionFixture;
import kr.moonseungjun.turnboundre.progression.CharacterProgress;
import kr.moonseungjun.turnboundre.progression.EquipmentProgress;
import kr.moonseungjun.turnboundre.progression.EquipmentRules;
import kr.moonseungjun.turnboundre.progression.PlayerProgressStore;
import kr.moonseungjun.turnboundre.progression.ProgressionRules;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Locks the economic/readiness envelope for the first Hub -> REGION_01 -> elite loop. */
class M6FirstExpeditionBalanceAcceptanceTest {
    private static final String PATROL = "turnbound_re:debug_overworld_patrol";
    private static final String ELITE = "turnbound_re:debug_rift_elite";
    private static final String BULWARK = "turnbound_re:iron_bulwark";
    private static final String EDGE = "turnbound_re:copper_edge";
    private static final String ZOMBIE = "turnbound_re:zombie";

    @Test
    void twoWorstCasePatrolsFundOneGearAndLevelTwoForTheWholeStarterParty() throws IOException {
        DefinitionRegistry registry = ProductionDefinitionFixture.load().registry();
        RewardTableDefinition patrolReward = registry.rewards().get(PATROL);
        long patrolCoin = guaranteedMinimum(patrolReward, "COIN");
        long patrolEssence = guaranteedMinimum(patrolReward, "ESSENCE");
        assertEquals(60L, patrolCoin);
        assertEquals(20L, patrolEssence);

        List<EquipmentDefinition> guaranteedFirstGear = registry.equipment().values().stream()
                .filter(definition -> {
                    EquipmentDefinition.Tier tier = definition.tier(1);
                    return tier != null
                            && tier.coinCost() <= patrolCoin
                            && tier.materialCount() <= ProductionWorldSlicePlan.firstMineMaterialBudget(definition.ingredientItem());
                })
                .toList();
        assertEquals(Set.of(BULWARK, EDGE), guaranteedFirstGear.stream().map(EquipmentDefinition::id).collect(java.util.stream.Collectors.toSet()));

        var tuning = registry.progressions().get(PlayerProgressStore.DEFAULT_PROGRESSION_ID);
        long partyLevelCoin = 0L;
        long partyLevelEssence = 0L;
        for (String id : tuning.starterParty()) {
            CharacterDefinition character = registry.characters().get(id);
            ProgressionRules.Cost cost = ProgressionRules.levelUpCost(tuning, character.originStar(), 1);
            partyLevelCoin += cost.coin();
            partyLevelEssence += cost.essence();
        }
        assertEquals(59L, partyLevelCoin);
        assertEquals(32L, partyLevelEssence);

        long cheapestGuaranteedGear = guaranteedFirstGear.stream().mapToLong(definition -> definition.tier(1).coinCost()).min().orElseThrow();
        assertTrue(patrolCoin < cheapestGuaranteedGear + partyLevelCoin,
                "one patrol must still force a meaningful gear-vs-growth choice");
        assertTrue(patrolCoin * 2L >= cheapestGuaranteedGear + partyLevelCoin,
                "two minimum patrol rewards must fund one first gear plus Lv2 across the starter party");
        assertTrue(patrolEssence * 2L >= partyLevelEssence,
                "two minimum patrol rewards must cover the paired Essence cost");

        for (EquipmentDefinition gear : guaranteedFirstGear) {
            int remainingIron = ProductionWorldSlicePlan.IRON_ORE_BLOCKS;
            if ("minecraft:iron_ingot".equals(gear.ingredientItem())) remainingIron -= gear.tier(1).materialCount();
            assertTrue(remainingIron >= BattlePreparationService.IRON_REINFORCEMENT.consumeCount(),
                    "every guaranteed first-gear choice must leave a local battle-preparation route");
        }
        assertTrue(ProductionWorldSlicePlan.COAL_ORE_BLOCKS > 0,
                "the authored mine must contain local furnace fuel for the first smelting return loop");
    }

    @Test
    void riftVanguardStaysEliteWithoutReturningToLateGameStatScale() throws IOException {
        DefinitionRegistry registry = ProductionDefinitionFixture.load().registry();
        var tuning = registry.progressions().get(PlayerProgressStore.DEFAULT_PROGRESSION_ID);
        BattlePreparationBonus preparation = BattlePreparationService.IRON_REINFORCEMENT.bonus();
        EquipmentProgress bulwark = new EquipmentProgress(BULWARK, 1);

        Aggregate player = Aggregate.ZERO;
        for (String id : tuning.starterParty()) {
            CharacterDefinition definition = registry.characters().get(id);
            CharacterProgress progress = new CharacterProgress(id, definition.originStar(), definition.originStar(), 2);
            CharacterDefinition.Stats stats = ProgressionRules.stats(definition, progress);
            if (ZOMBIE.equals(id)) stats = EquipmentRules.apply(registry, bulwark, stats);
            player = player.plus(preparation.apply(stats));
        }

        EncounterDefinition elite = registry.encounters().get(ELITE);
        assertEquals(List.of(4, 3, 3, 4), elite.enemies().stream().map(EncounterDefinition.EnemySlot::level).toList());
        Aggregate enemy = Aggregate.ZERO;
        for (EncounterDefinition.EnemySlot slot : elite.enemies()) {
            CharacterDefinition definition = registry.characters().get(slot.character());
            CharacterProgress progress = new CharacterProgress(
                    definition.id(), definition.originStar(), slot.currentStar(), slot.level());
            enemy = enemy.plus(ProgressionRules.stats(definition, progress));
        }

        assertTrue(enemy.hp > player.hp && enemy.atk > player.atk && enemy.def > player.def,
                "the first elite must remain statistically stronger than the prepared readiness target");
        assertRatioAtMost(enemy.hp, player.hp, 130, "HP");
        assertRatioAtMost(enemy.atk, player.atk, 150, "ATK");
        assertRatioAtMost(enemy.def, player.def, 130, "DEF");
        assertRatioAtMost(enemy.spd, player.spd, 120, "SPD");
        assertRatioAtMost(enemy.poise, player.poise, 110, "POISE");
    }

    @Test
    void minimumEliteRewardFundsTheNextWholePartyGrowthStep() throws IOException {
        DefinitionRegistry registry = ProductionDefinitionFixture.load().registry();
        RewardTableDefinition eliteReward = registry.rewards().get(ELITE);
        long eliteCoin = guaranteedMinimum(eliteReward, "COIN");
        long eliteEssence = guaranteedMinimum(eliteReward, "ESSENCE");
        assertEquals(180L, eliteCoin);
        assertEquals(60L, eliteEssence);

        var tuning = registry.progressions().get(PlayerProgressStore.DEFAULT_PROGRESSION_ID);
        long nextCoin = 0L;
        long nextEssence = 0L;
        for (String id : tuning.starterParty()) {
            CharacterDefinition character = registry.characters().get(id);
            ProgressionRules.Cost cost = ProgressionRules.levelUpCost(tuning, character.originStar(), 2);
            nextCoin += cost.coin();
            nextEssence += cost.essence();
        }
        assertEquals(72L, nextCoin);
        assertEquals(41L, nextEssence);
        assertTrue(eliteCoin >= nextCoin);
        assertTrue(eliteEssence >= nextEssence);
    }

    private static long guaranteedMinimum(RewardTableDefinition table, String type) {
        return table.rolls().stream()
                .filter(roll -> type.equals(roll.type()) && roll.chance() >= 1.0D)
                .mapToLong(RewardTableDefinition.Roll::min)
                .sum();
    }

    private static void assertRatioAtMost(long actual, long baseline, int percent, String label) {
        assertTrue(actual * 100L <= baseline * percent,
                label + " exceeds first-expedition threat ceiling: " + actual + " vs " + baseline);
    }

    private record Aggregate(long hp, long atk, long def, long spd, long poise) {
        private static final Aggregate ZERO = new Aggregate(0, 0, 0, 0, 0);

        Aggregate plus(CharacterDefinition.Stats stats) {
            return new Aggregate(
                    hp + stats.hp(),
                    atk + stats.atk(),
                    def + stats.def(),
                    spd + stats.spd(),
                    poise + stats.poise());
        }
    }
}
