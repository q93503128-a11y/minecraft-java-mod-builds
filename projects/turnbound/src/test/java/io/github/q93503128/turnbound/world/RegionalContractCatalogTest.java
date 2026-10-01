package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionalContractCatalogTest {
    @Test
    void contractPoolIsValidAndExtendsIntoAvsalWithoutMainQuestGate() {
        assertTrue(RegionalContractCatalog.validate().isEmpty(),
                () -> String.join("; ", RegionalContractCatalog.validate()));

        var avsal = RegionalContractCatalog.all().stream()
                .filter(contract -> "Av'Sal".equals(contract.regionLabel()))
                .toList();
        assertEquals(3, avsal.size());
        assertTrue(avsal.stream().allMatch(contract -> contract.tier() == 4));
        assertTrue(avsal.stream().allMatch(contract -> contract.minPartyLevel() >= 16));
        assertTrue(avsal.stream().allMatch(contract ->
                AvsalExpansionProgress.REGION_DISCOVERED.equals(contract.requiredFlag())));
        assertTrue(avsal.stream().noneMatch(contract -> contract.requiredFlag().startsWith("MQ_")));
    }
}
