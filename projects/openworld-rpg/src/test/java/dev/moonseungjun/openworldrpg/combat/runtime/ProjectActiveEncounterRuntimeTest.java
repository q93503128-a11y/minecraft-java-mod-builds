package dev.moonseungjun.openworldrpg.combat.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProjectActiveEncounterRuntimeTest {
    private static final UUID EARTHLOONG_A =
            UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID EARTHLOONG_B =
            UUID.fromString("20000000-0000-0000-0000-000000000002");

    @Test
    void unengagedSupportCasterMayJoinRecipientsExactEncounter() {
        var recipient = Optional.of(ref(EARTHLOONG_A, 8));

        assertEquals(
                OptionalInt.of(8),
                ProjectActiveEncounterRuntime
                        .resolveSupportEncounterLevel(
                                Optional.empty(),
                                recipient
                        )
        );
    }

    @Test
    void sameExactEncounterKeepsSupportAuthority() {
        var first = ref(EARTHLOONG_A, 8);
        var second = ref(EARTHLOONG_A, 8);

        assertTrue(
                ProjectActiveEncounterRuntime.sameEncounter(
                        first,
                        second
                )
        );
        assertEquals(
                OptionalInt.of(8),
                ProjectActiveEncounterRuntime
                        .resolveSupportEncounterLevel(
                                Optional.of(first),
                                Optional.of(second)
                        )
        );
    }

    @Test
    void sameContentLevelButDifferentActorsFailClosed() {
        var first = ref(EARTHLOONG_A, 8);
        var second = ref(EARTHLOONG_B, 8);

        assertFalse(
                ProjectActiveEncounterRuntime.sameEncounter(
                        first,
                        second
                )
        );
        assertTrue(
                ProjectActiveEncounterRuntime
                        .resolveSupportEncounterLevel(
                                Optional.of(first),
                                Optional.of(second)
                        )
                        .isEmpty()
        );
    }

    @Test
    void inconsistentLevelForSameActorFailsClosed() {
        assertTrue(
                ProjectActiveEncounterRuntime
                        .resolveSupportEncounterLevel(
                                Optional.of(ref(EARTHLOONG_A, 8)),
                                Optional.of(ref(EARTHLOONG_A, 9))
                        )
                        .isEmpty()
        );
    }

    @Test
    void noLiveEncounterProducesNoSupportContext() {
        assertTrue(
                ProjectActiveEncounterRuntime
                        .resolveSupportEncounterLevel(
                                Optional.empty(),
                                Optional.empty()
                        )
                        .isEmpty()
        );
    }

    private static ProjectActiveEncounterRuntime.ActiveEncounterRef ref(
            UUID actorId,
            int level
    ) {
        return new ProjectActiveEncounterRuntime.ActiveEncounterRef(
                "openworld_rpg:r01/earthloong",
                actorId,
                level
        );
    }
}
