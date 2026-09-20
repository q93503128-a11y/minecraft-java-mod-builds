package io.github.q93503128.turnbound.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleStatusPresentationTest {
    @Test
    void v1SignatureResourcesUseCanonicalLabelsAndThresholdColor() {
        var bram = unit("P03", 1000, 1000, List.of("@r:guard:50:100"));
        var raze = unit("P08", 900, 1000, List.of("@r:fury:90:100"));

        assertEquals("Guard 50/100", BattleStatusPresentation.badges(bram).getFirst().text());
        assertEquals(TurnboundUiTokens.SUCCESS, BattleStatusPresentation.badges(bram).getFirst().color());
        assertEquals("Fury 90/100", BattleStatusPresentation.badges(raze).getFirst().text());
        assertEquals(TurnboundUiTokens.DANGER, BattleStatusPresentation.badges(raze).getFirst().color());
    }

    @Test
    void targetAndSanctuaryMarkersAreReadableWithoutInternalIds() {
        var target = unit("E001", 1000, 1000,
                List.of("@m:duel", "@m:sightline", "@s:sanctuary:1:2:0.000"));
        List<BattleStatusPresentation.Badge> badges = BattleStatusPresentation.badges(target);

        assertTrue(badges.stream().anyMatch(b -> "결투 대상".equals(b.text())));
        assertTrue(badges.stream().anyMatch(b -> "Sightline".equals(b.text())));
        assertTrue(badges.stream().anyMatch(b -> "안식 표식 2T".equals(b.text())));
    }

    private static ClientBattleState.Unit unit(String defId, int hp, int maxHp, List<String> statuses) {
        return new ClientBattleState.Unit("u", defId, "ALLY", defId, hp, maxHp, 0, 0, false,
                0.0, 0.0, 0.0, statuses);
    }
}
