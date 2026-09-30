package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvsalQuestCatalogTest {
    @Test
    void journalCatalogIsValidAndUnlocksMqAv02AfterArrival() {
        assertTrue(AvsalQuestCatalog.validate().isEmpty(),
                () -> String.join("; ", AvsalQuestCatalog.validate()));
        var mq1 = AvsalQuestCatalog.all().stream().filter(q -> "MQ_AV01".equals(q.id())).findFirst().orElseThrow();
        var mq2 = AvsalQuestCatalog.all().stream().filter(q -> "MQ_AV02".equals(q.id())).findFirst().orElseThrow();

        assertTrue(AvsalQuestCatalog.visible(mq1, Set.of(AvsalExpansionProgress.BRIEFED), Set.of()));
        assertFalse(AvsalQuestCatalog.completed(mq1, Set.of(AvsalExpansionProgress.BRIEFED), Set.of()));
        assertTrue(AvsalQuestCatalog.completed(mq1, Set.of(AvsalExpansionProgress.OUTSKIRTS_REACHED), Set.of()));
        assertTrue(AvsalQuestCatalog.visible(mq2, Set.of(AvsalExpansionProgress.OUTSKIRTS_REACHED), Set.of()));
        assertFalse(AvsalQuestCatalog.completed(mq2, Set.of(AvsalExpansionProgress.OUTSKIRTS_REACHED), Set.of()));
        assertTrue(AvsalQuestCatalog.completed(mq2, Set.of(AvsalExpansionProgress.INVESTIGATION_COMPLETE), Set.of()));
        assertTrue(AvsalQuestCatalog.all().stream()
                .allMatch(quest -> quest.rewardCrystal() > 0 && quest.rewardGold() > 0));
    }
}
