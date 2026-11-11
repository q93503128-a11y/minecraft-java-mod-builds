package io.github.q93503128.turnbound;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class TurnboundFoundryGateServiceTest {
    private static Map<String, Integer> source() {
        return Map.of("minecraft:air", 35, "minecraft:andesite", 13,
                "minecraft:basalt", 10, "minecraft:cobblestone", 10,
                "minecraft:mossy_cobblestone", 3, "minecraft:polished_basalt", 11,
                "minecraft:stone", 8);
    }
    private static Map<String, Integer> closed() {
        return Map.of("minecraft:andesite", 2, "minecraft:basalt", 17,
                "minecraft:bedrock", 45, "minecraft:cobblestone", 4,
                "minecraft:polished_basalt", 18, "minecraft:stone", 4);
    }

    @Test void onlyVerifiedOriginalClosedGeometryIsEligible() {
        assertTrue(TurnboundFoundryGateService.matchesExpectedPhysicalPalette(source(), closed(), 59));
        assertFalse(TurnboundFoundryGateService.matchesExpectedPhysicalPalette(source(), closed(), 58));
        assertFalse(TurnboundFoundryGateService.matchesExpectedPhysicalPalette(source(), source(), 0));
        Map<String, Integer> altered = new HashMap<>(closed());
        altered.put("minecraft:bedrock", 44);
        altered.put("minecraft:air", 1);
        assertFalse(TurnboundFoundryGateService.matchesExpectedPhysicalPalette(source(), altered, 59));
        Map<String, Integer> wrongSource = new HashMap<>(source());
        wrongSource.put("minecraft:air", 34);
        wrongSource.put("minecraft:stone", 9);
        assertFalse(TurnboundFoundryGateService.matchesExpectedPhysicalPalette(wrongSource, closed(), 59));
    }
}
