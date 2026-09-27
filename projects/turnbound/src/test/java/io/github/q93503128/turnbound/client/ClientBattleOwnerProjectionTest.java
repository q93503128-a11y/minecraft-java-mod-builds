package io.github.q93503128.turnbound.client;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ClientBattleOwnerProjectionTest {
    @Test void parsesSharedActorOwnershipWithoutChangingUnitIds() {
        UUID first=UUID.randomUUID(),second=UUID.randomUUID();
        String raw="H|1|0|1|RUNNING|p2_ally_p01|0|1|0|0\n"
                +"U|p1_ally_p01|P01|ALLY|A|10|10|0|0|0|0|64|0|\n"
                +"U|p2_ally_p01|P01|ALLY|B|10|10|0|0|0|1|64|0|\n"
                +"O|p1_ally_p01|"+first+"|1\n"
                +"O|p2_ally_p01|"+second+"|0\n";
        ClientBattleState.update(raw);
        assertTrue(ClientBattleState.sharedBattle());
        assertTrue(ClientBattleState.ownerOf("p1_ally_p01").local());
        assertEquals(second,ClientBattleState.ownerOf("p2_ally_p01").playerId());
        assertEquals("p2_ally_p01",ClientBattleState.snapshot().actorId());
    }
}
