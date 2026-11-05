package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.progression.PlayerProfile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EmptyRebuildCampaignTest {
    @Test
    void freshRebuildStartsWithNoLegacyPartyOrCurrenciesAndRoundTrips() {
        UUID id = UUID.randomUUID();
        UUID reloaded = UUID.randomUUID();
        try {
            assertTrue(CampaignProgressStore.startEmptyRebuildGame(id));
            assertFalse(CampaignProgressStore.startEmptyRebuildGame(id), "must never replace an already loaded profile");
            assertTrue(CampaignProgressStore.hasRuntime(id));
            assertTrue(CampaignProgressStore.isEmptyRebuildGame(id));
            assertTrue(CampaignProgressStore.ownedCharacters(id).isEmpty());
            assertTrue(CampaignProgressStore.activeParty(id).isEmpty());
            assertEquals(0, CampaignProgressStore.gold(id));
            assertEquals(0, CampaignProgressStore.currency(id, PlayerProfile.Currency.SUMMON_CRYSTAL));
            assertThrows(IllegalStateException.class, () -> CampaignProgressStore.summonStandard(id, 1));
            assertThrows(IllegalStateException.class, () -> CampaignProgressStore.summonStarter(id));
            assertEquals(0, CampaignProgressStore.gold(id), "unavailable summon must not spend money");

            String save = CampaignProgressStore.encodeEmptyRebuildGame(id);
            assertTrue(save.contains("\"schemaVersion\": 7"));
            assertFalse(save.contains("P01"));
            CampaignProgressStore.restoreEmptyRebuildGame(reloaded, save);
            assertEquals(CampaignProgressStore.snapshot(id), CampaignProgressStore.snapshot(reloaded));
            assertEquals(save, CampaignProgressStore.encodeEmptyRebuildGame(reloaded));
            assertFalse(CampaignProgressStore.startEmptyRebuildGame(reloaded));
        } finally {
            CampaignProgressStore.removeRuntime(id);
            CampaignProgressStore.removeRuntime(reloaded);
        }
    }

    @Test
    void emptyPartyAllowedOnlyWhenCollectionIsEmpty() {
        UUID id = UUID.randomUUID();
        try {
            CampaignProgressStore.startEmptyRebuildGame(id);
            CampaignProgressStore.setActiveParty(id, List.of());
            assertEquals(List.of(), CampaignProgressStore.activeParty(id));
            assertThrows(IllegalArgumentException.class,
                    () -> CampaignProgressStore.setActiveParty(id, List.of("P01")));
        } finally {
            CampaignProgressStore.removeRuntime(id);
        }
    }

    @Test
    void legacySaveCannotBeDisguisedAsRebuildOrSilentlyReset() {
        UUID legacyId = UUID.randomUUID();
        try {
            CampaignProgressStore.ensureNewGame(legacyId);
            String oldSave = CampaignSaveCodec.encode(CampaignProgressStore.snapshot(legacyId));
            assertThrows(IllegalStateException.class, () -> CampaignSaveCodec.decodeRebuild(oldSave));
            assertThrows(IllegalStateException.class, () -> CampaignSaveCodec.encodeRebuild(
                    CampaignProgressStore.snapshot(legacyId)));
            assertThrows(IllegalStateException.class,
                    () -> CampaignProgressStore.restoreEmptyRebuildGame(legacyId, oldSave),
                    "an existing runtime may not be overwritten");
            assertTrue(CampaignProgressStore.ownedCharacters(legacyId).contains("P01"));
            assertFalse(CampaignProgressStore.isEmptyRebuildGame(legacyId));
        } finally {
            CampaignProgressStore.removeRuntime(legacyId);
        }
    }

    @Test
    void rejectedEmptyLegacySaveDoesNotBecomeARebuildSave() {
        UUID id = UUID.randomUUID();
        try {
            CampaignProgressStore.startEmptyRebuildGame(id);
            String rebuild = CampaignProgressStore.encodeEmptyRebuildGame(id);
            String masqueradingAsLegacy = rebuild.replace("\"schemaVersion\": 7", "\"schemaVersion\": 6");
            assertThrows(IllegalStateException.class, () -> CampaignSaveCodec.decode(masqueradingAsLegacy));
            assertThrows(IllegalStateException.class, () -> CampaignSaveCodec.decodeRebuild(masqueradingAsLegacy));
            String retiredCharacter = rebuild.replace("\"ownedCharacters\": []", "\"ownedCharacters\": [\"P01\"]");
            assertThrows(IllegalStateException.class, () -> CampaignSaveCodec.decodeRebuild(retiredCharacter));
            assertThrows(IllegalStateException.class, () -> CampaignSaveCodec.decodeRebuild(
                    rebuild.replace("\"saveKind\": \"turnbound-empty-foundation\"",
                                    "\"saveKind\": \"legacy\"")));
        } finally {
            CampaignProgressStore.removeRuntime(id);
        }
    }
}
