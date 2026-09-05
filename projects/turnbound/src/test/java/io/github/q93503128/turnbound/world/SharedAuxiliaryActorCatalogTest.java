package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class SharedAuxiliaryActorCatalogTest {
    @Test
    void roleTagsRoundTripWithoutMinecraftRuntime() {
        for (String key : new String[]{
                "field_incident:M_PATROL",
                "character_quest:CQ_P01",
                "region_quest:RQ_M01_broken_cart",
                "exploration_codex:0",
                "signature_trial:P01",
                "endgame_selector:BATTLE_B01",
                "endgame_challenge_board"}) {
            String tag = SharedAuxiliaryActorCatalog.roleTag(key);
            assertEquals(key, SharedAuxiliaryActorCatalog.fromTag(tag));
        }
    }

    @Test
    void malformedTagsAreRejected() {
        assertNull(SharedAuxiliaryActorCatalog.fromTag(null));
        assertNull(SharedAuxiliaryActorCatalog.fromTag("other"));
        assertNull(SharedAuxiliaryActorCatalog.fromTag(SharedAuxiliaryActorCatalog.ROLE_PREFIX));
        assertNull(SharedAuxiliaryActorCatalog.fromTag(SharedAuxiliaryActorCatalog.ROLE_PREFIX + "bad key"));
        assertThrows(IllegalArgumentException.class, () -> SharedAuxiliaryActorCatalog.roleTag(""));
        assertThrows(IllegalArgumentException.class, () -> SharedAuxiliaryActorCatalog.roleTag("bad key"));
    }
}
