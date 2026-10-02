package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvsalQuestCatalogTest {
    @Test
    void sevenMainQuestBeatsCloseTheFirstAvsalSliceWithAPostBossReport() {
        assertTrue(AvsalQuestCatalog.validate().isEmpty(),
                () -> String.join("; ", AvsalQuestCatalog.validate()));

        var mains = AvsalQuestCatalog.all().stream().filter(q -> q.kind() == AvsalQuestCatalog.Kind.MAIN).toList();
        assertTrue(mains.stream().map(AvsalQuestCatalog.Quest::id).toList()
                .containsAll(java.util.List.of("MQ_AV01","MQ_AV02","MQ_AV03","MQ_AV04","MQ_AV05","MQ_AV06","MQ_AV07")));

        var mq1=mains.stream().filter(q->"MQ_AV01".equals(q.id())).findFirst().orElseThrow();
        var mq2=mains.stream().filter(q->"MQ_AV02".equals(q.id())).findFirst().orElseThrow();
        var mq3=mains.stream().filter(q->"MQ_AV03".equals(q.id())).findFirst().orElseThrow();
        var mq4=mains.stream().filter(q->"MQ_AV04".equals(q.id())).findFirst().orElseThrow();
        var mq5=mains.stream().filter(q->"MQ_AV05".equals(q.id())).findFirst().orElseThrow();
        var mq6=mains.stream().filter(q->"MQ_AV06".equals(q.id())).findFirst().orElseThrow();
        var mq7=mains.stream().filter(q->"MQ_AV07".equals(q.id())).findFirst().orElseThrow();

        assertTrue(AvsalQuestCatalog.visible(mq1,Set.of(AvsalExpansionProgress.BRIEFED),Set.of()));
        assertTrue(AvsalQuestCatalog.completed(mq1,Set.of(AvsalExpansionProgress.ROADSIDE_ECHO_SEEN),Set.of()));
        assertTrue(AvsalQuestCatalog.visible(mq2,Set.of(AvsalExpansionProgress.ROADSIDE_ECHO_SEEN),Set.of()));
        assertTrue(AvsalQuestCatalog.completed(mq2,Set.of("AVSAL_ROAD_COURIER_OFFERED"),Set.of()));
        assertTrue(AvsalQuestCatalog.visible(mq3,Set.of("AVSAL_ROAD_COURIER_OFFERED"),Set.of()));
        assertTrue(AvsalQuestCatalog.completed(mq3,Set.of(AvsalExpansionProgress.OUTSKIRTS_REACHED),Set.of()));
        assertTrue(AvsalQuestCatalog.visible(mq4,Set.of(AvsalExpansionProgress.OUTSKIRTS_REACHED),Set.of()));
        assertTrue(AvsalQuestCatalog.completed(mq4,Set.of(AvsalExpansionProgress.INVESTIGATION_COMPLETE),Set.of()));
        assertTrue(AvsalQuestCatalog.visible(mq5,Set.of(AvsalExpansionProgress.INVESTIGATION_COMPLETE),Set.of()));
        assertTrue(AvsalQuestCatalog.completed(mq5,Set.of(AvsalExpansionProgress.RELAY_COMPLETE),Set.of()));
        assertTrue(AvsalQuestCatalog.visible(mq6,Set.of(AvsalExpansionProgress.RELAY_COMPLETE),Set.of("AV_FIRST_BOSS")));
        assertTrue(AvsalQuestCatalog.completed(mq6,Set.of(AvsalExpansionProgress.RELAY_COMPLETE),Set.of("AV_FIRST_BOSS")));
        assertTrue(AvsalQuestCatalog.visible(mq7,Set.of(AvsalExpansionProgress.FIRST_BOSS_CLEARED),Set.of()));
        assertFalse(AvsalQuestCatalog.completed(mq7,Set.of(AvsalExpansionProgress.FIRST_BOSS_CLEARED),Set.of("AV_FIRST_BOSS")));
        assertTrue(AvsalQuestCatalog.completed(mq7,Set.of(AvsalExpansionProgress.FIRST_BOSS_REPORTED),Set.of("AV_FIRST_BOSS")));
        assertTrue(AvsalQuestCatalog.all().stream().allMatch(q -> q.rewardCrystal()>0 && q.rewardGold()>0));
        assertTrue(AvsalQuestCatalog.all().size()>=18,
                "Av'Sal is the Chapter 1 endpoint and needs a full chapter-sized quest slate");
        assertTrue(AvsalQuestCatalog.all().stream().filter(q->q.kind()==AvsalQuestCatalog.Kind.SIDE).count()>=11);
    }
}
