package io.github.q93503128.turnbound.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientBattleStateGuidanceTest {
    @Test
    void optionalBattleMessageLineFeedsTheExistingActionHeaderState() {
        ClientBattleState.update(
                "H|1|0|1|RUNNING|ally_p01|0|1|1|1\n" +
                "C|CV_FIRST_COMMON\n" +
                "M|적을 선택하고 기본 공격을 사용하세요\n" +
                "A|0|0|0|0\n" +
                "V|0|0\n"
        );

        assertEquals("CV_FIRST_COMMON", ClientBattleState.encounterId());
        assertEquals("적을 선택하고 기본 공격을 사용하세요", ClientBattleState.snapshot().message());
    }

    @Test
    void olderSnapshotsWithoutMessageRemainCompatible() {
        ClientBattleState.update("H|1|0|1|RUNNING|ally_p01|0|1|1|1\nC|CV_DRABYEL_ROAD\n");
        assertEquals("", ClientBattleState.snapshot().message());
    }
}
