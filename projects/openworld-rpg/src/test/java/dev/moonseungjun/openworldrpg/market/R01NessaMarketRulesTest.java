package dev.moonseungjun.openworldrpg.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.ProjectEquipmentSlot;
import dev.moonseungjun.openworldrpg.inventory.ProjectItemGrade;
import java.util.HashSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class R01NessaMarketRulesTest {
    private static final String PLAYER =
            UUID.fromString("11111111-2222-3333-4444-555555555555").toString();

    @Test
    void cycleBoundaryUsesTenActiveMinutesSinceAlderfordShrine() {
        long epoch = 500L;
        assertEquals(0L, R01NessaMarketRules.cycleIndex(epoch, epoch));
        assertEquals(
                0L,
                R01NessaMarketRules.cycleIndex(
                        epoch + R01NessaMarketRules.CYCLE_TICKS - 1L,
                        epoch
                )
        );
        assertEquals(
                1L,
                R01NessaMarketRules.cycleIndex(
                        epoch + R01NessaMarketRules.CYCLE_TICKS,
                        epoch
                )
        );
    }

    @Test
    void generatedCycleHasExactFiveSlotContractAndAtMostOneSuperior() {
        for (long cycle = 0L; cycle < 100L; cycle++) {
            var items = R01NessaMarketRules.generateCycle(9988L, PLAYER, cycle);
            assertEquals(5, items.size());

            assertEquals(
                    R01NessaMarketRules.MarketCategory.WEAPON,
                    items.get(0).category()
            );
            assertEquals(
                    ProjectEquipmentSlot.MAIN_WEAPON,
                    items.get(0).equipmentSlot()
            );

            assertTrue(
                    items.get(1).category()
                            == R01NessaMarketRules.MarketCategory.WEAPON
                    || items.get(1).category()
                            == R01NessaMarketRules.MarketCategory.OFF_HAND
            );
            if (items.get(1).category()
                    == R01NessaMarketRules.MarketCategory.WEAPON) {
                assertNotEquals(items.get(0).baseId(), items.get(1).baseId());
            }

            assertEquals(
                    R01NessaMarketRules.MarketCategory.ARMOR,
                    items.get(2).category()
            );
            assertEquals(
                    R01NessaMarketRules.MarketCategory.ARMOR,
                    items.get(3).category()
            );
            assertNotEquals(
                    items.get(2).equipmentSlot(),
                    items.get(3).equipmentSlot()
            );

            assertEquals(
                    R01NessaMarketRules.MarketCategory.ACCESSORY,
                    items.get(4).category()
            );

            long superiorCount = items.stream()
                    .filter(value -> value.grade() == ProjectItemGrade.SUPERIOR)
                    .count();
            assertTrue(superiorCount <= 1L);

            for (var item : items) {
                assertTrue(
                        item.grade() == ProjectItemGrade.STANDARD
                        || item.grade() == ProjectItemGrade.REFINED
                        || item.grade() == ProjectItemGrade.SUPERIOR
                );
                assertEquals(
                        R01NessaMarketRules.itemLevel(item.grade()),
                        item.itemLevel()
                );
                assertEquals(
                        R01NessaMarketRules.priceGold(
                                item.category(),
                                item.grade()
                        ),
                        item.priceGold()
                );
            }
        }
    }

    @Test
    void generationIsDeterministicAndCycleRefreshChangesSeededStock() {
        var first = R01NessaMarketRules.generateCycle(77L, PLAYER, 5L);
        var same = R01NessaMarketRules.generateCycle(77L, PLAYER, 5L);
        var next = R01NessaMarketRules.generateCycle(77L, PLAYER, 6L);

        assertEquals(first, same);
        assertNotEquals(
                first.stream().map(value -> value.affixSeed()).toList(),
                next.stream().map(value -> value.affixSeed()).toList()
        );
    }

    @Test
    void statePersistsSoldSlotsAndNewCycleClearsThem() {
        var state = R01NessaMarketState.initial()
                .ensureCycle(123L, PLAYER, 2L);

        var plan = state.purchasePlan(PLAYER, 2L, 3).orElseThrow();
        state = state.markSold(
                PLAYER,
                plan.cycleIndex(),
                plan.slotIndex(),
                plan.transactionId()
        );

        assertTrue(
                state.currentCycle().orElseThrow().soldSlots().contains(3)
        );
        assertTrue(state.purchasePlan(PLAYER, 2L, 3).isEmpty());

        var repeated = state.markSold(
                PLAYER,
                plan.cycleIndex(),
                plan.slotIndex(),
                plan.transactionId()
        );
        assertEquals(state, repeated);

        state = state.ensureCycle(123L, PLAYER, 3L);
        assertTrue(state.currentCycle().orElseThrow().soldSlots().isEmpty());
        assertEquals(3L, state.currentCycle().orElseThrow().cycleIndex());
    }

    @Test
    void staleCycleCannotProducePurchasePlan() {
        var state = R01NessaMarketState.initial()
                .ensureCycle(123L, PLAYER, 7L);

        assertTrue(state.purchasePlan(PLAYER, 6L, 1).isEmpty());
        assertTrue(state.purchasePlan(PLAYER, 7L, 1).isPresent());
    }

    @Test
    void pricesAndSellbackMatchCanon() {
        assertEquals(
                150L,
                R01NessaMarketRules.priceGold(
                        R01NessaMarketRules.MarketCategory.WEAPON,
                        ProjectItemGrade.STANDARD
                )
        );
        assertEquals(
                500L,
                R01NessaMarketRules.priceGold(
                        R01NessaMarketRules.MarketCategory.WEAPON,
                        ProjectItemGrade.SUPERIOR
                )
        );
        assertEquals(
                380L,
                R01NessaMarketRules.priceGold(
                        R01NessaMarketRules.MarketCategory.ACCESSORY,
                        ProjectItemGrade.SUPERIOR
                )
        );
        assertEquals(125L, R01NessaMarketRules.sellBackGold(500L));
    }
}
