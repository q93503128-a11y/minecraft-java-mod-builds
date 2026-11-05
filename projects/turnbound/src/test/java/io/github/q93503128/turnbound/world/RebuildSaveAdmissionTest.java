package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static io.github.q93503128.turnbound.world.RebuildSaveAdmission.Decision.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RebuildSaveAdmissionTest {
    @Test void freshWorldStartsEmptyOnlyWithNoOldCampaignData() {
        assertEquals(START_FRESH, RebuildSaveAdmission.decide(false, false, false));
        assertEquals(RESTORE_REBUILD, RebuildSaveAdmission.decide(true, false, false));
    }

    @Test void anyOldDataBlocksRebuildAndDoesNotInviteMigration() {
        assertEquals(REJECT_LEGACY, RebuildSaveAdmission.decide(false, true, false));
        assertEquals(REJECT_LEGACY, RebuildSaveAdmission.decide(false, false, true));
        assertEquals(REJECT_LEGACY, RebuildSaveAdmission.decide(false, true, true));
        assertEquals(REJECT_CONFLICT, RebuildSaveAdmission.decide(true, true, false));
        assertEquals(REJECT_CONFLICT, RebuildSaveAdmission.decide(true, false, true));
        assertEquals(REJECT_CONFLICT, RebuildSaveAdmission.decide(true, true, true));
    }
}
