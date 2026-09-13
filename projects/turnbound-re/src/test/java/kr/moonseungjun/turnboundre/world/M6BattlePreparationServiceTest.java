package kr.moonseungjun.turnboundre.world;

import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class M6BattlePreparationServiceTest {
    @Test
    void vanillaMaterialsMapToDistinctExplicitPreparations() {
        assertEquals("iron_reinforcement",
                BattlePreparationService.resolveItem(Items.IRON_INGOT, 1).id());
        assertEquals("golden_provision",
                BattlePreparationService.resolveItem(Items.GOLDEN_CARROT, 1).id());
        assertEquals("cooked_cod_ration",
                BattlePreparationService.resolveItem(Items.COOKED_COD, 1).id());
        assertEquals("cooked_salmon_ration",
                BattlePreparationService.resolveItem(Items.COOKED_SALMON, 1).id());
    }

    @Test
    void unknownEmptyOrMissingItemsMeanNoPreparation() {
        assertSame(BattlePreparationService.NONE,
                BattlePreparationService.resolveItem(Items.DIRT, 64));
        assertSame(BattlePreparationService.NONE,
                BattlePreparationService.resolveItem(Items.IRON_INGOT, 0));
        assertSame(BattlePreparationService.NONE,
                BattlePreparationService.resolveItem(null, 1));
    }

    @Test
    void previewIdentityMustStillMatchAtServerConfirm() {
        var iron = BattlePreparationService.resolveItem(Items.IRON_INGOT, 3);
        assertTrue(BattlePreparationService.matchesExpected("iron_reinforcement", iron));
        assertFalse(BattlePreparationService.matchesExpected("golden_provision", iron));
        assertTrue(BattlePreparationService.matchesExpected("", BattlePreparationService.NONE));
        assertTrue(BattlePreparationService.matchesExpected(null, BattlePreparationService.NONE));
    }

    @Test
    void eachActivePreparationConsumesExactlyOneSelectedItem() {
        assertEquals(1, BattlePreparationService.IRON_REINFORCEMENT.consumeCount());
        assertEquals(1, BattlePreparationService.GOLDEN_PROVISION.consumeCount());
        assertEquals(1, BattlePreparationService.COOKED_COD_RATION.consumeCount());
        assertEquals(1, BattlePreparationService.COOKED_SALMON_RATION.consumeCount());
        assertEquals(0, BattlePreparationService.NONE.consumeCount());
    }
}
