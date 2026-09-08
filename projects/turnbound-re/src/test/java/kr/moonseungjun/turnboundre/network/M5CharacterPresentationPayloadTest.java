package kr.moonseungjun.turnboundre.network;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class M5CharacterPresentationPayloadTest {
    @Test
    void visualCatalogRoundTripsServerPublishedSourceEntities() {
        Map<String, String> input = new LinkedHashMap<>();
        input.put("turnbound_re:zombie", "minecraft:zombie");
        input.put("turnbound_re:spider", "minecraft:spider");
        input.put("turnbound_re:iron_golem", "minecraft:iron_golem");

        Map<String, String> decoded = CharacterPresentationNetworkPayloads.CatalogS2C.of(input).decode();
        assertEquals(input, decoded);
    }

    @Test
    void duplicateCharacterVisualIdsAreRejectedOnDecode() {
        String encoded = CharacterPresentationNetworkPayloads.CatalogS2C
                .of(Map.of("turnbound_re:zombie", "minecraft:zombie")).wire();
        assertThrows(IllegalArgumentException.class,
                () -> new CharacterPresentationNetworkPayloads.CatalogS2C(encoded + ";" + encoded).decode());
    }
}
