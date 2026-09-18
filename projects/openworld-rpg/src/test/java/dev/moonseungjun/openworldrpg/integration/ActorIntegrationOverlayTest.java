package dev.moonseungjun.openworldrpg.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.integration.overlay.ActorIntegrationOverlay;
import dev.moonseungjun.openworldrpg.integration.overlay.ActorIntegrationOverlayLoader;
import dev.moonseungjun.openworldrpg.integration.overlay.ActorIntegrationOverlayValidator;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

class ActorIntegrationOverlayTest {
    @Test
    void completeSyntheticOverlayPassesValidation() {
        String json = """
                {
                  "target": "example_mod:boss",
                  "role": "integration_contract_test",
                  "required": true,
                  "policy": {
                    "presentation": "PASS_THROUGH",
                    "animation": "ADAPT",
                    "movement_ai": "ADAPT",
                    "combat_ai": "OVERRIDE",
                    "spawn": "OVERRIDE",
                    "stats": "OVERRIDE",
                    "damage": "OVERRIDE",
                    "loot": "OVERRIDE",
                    "recipes": "SUPPRESS",
                    "worldgen": "SUPPRESS",
                    "capture_or_duplication": "SUPPRESS",
                    "progression": "OVERRIDE",
                    "save_ownership": "OVERRIDE"
                  },
                  "tags": [
                    "openworld_rpg:actors/boss",
                    "openworld_rpg:actors/no_capture"
                  ],
                  "encounter": "openworld_rpg:test/contract"
                }
                """;

        ActorIntegrationOverlay overlay = ActorIntegrationOverlayLoader.parse(new StringReader(json));
        assertFalse(ActorIntegrationOverlayValidator.validate(overlay).hasErrors());
    }

    @Test
    void incompleteOverlayFailsValidation() {
        String json = """
                {
                  "target": "not a resource id",
                  "role": "",
                  "required": true,
                  "policy": {
                    "presentation": "PASS_THROUGH"
                  }
                }
                """;

        ActorIntegrationOverlay overlay = ActorIntegrationOverlayLoader.parse(new StringReader(json));
        assertTrue(ActorIntegrationOverlayValidator.validate(overlay).hasErrors());
    }

    @Test
    void missingNonCombatOwnershipDimensionFailsValidation() {
        String json = """
                {
                  "target": "example_mod:boss",
                  "role": "integration_contract_test",
                  "required": true,
                  "policy": {
                    "presentation": "PASS_THROUGH",
                    "animation": "ADAPT",
                    "movement_ai": "ADAPT",
                    "combat_ai": "OVERRIDE",
                    "spawn": "OVERRIDE",
                    "stats": "OVERRIDE",
                    "damage": "OVERRIDE",
                    "loot": "OVERRIDE",
                    "recipes": "SUPPRESS",
                    "worldgen": "SUPPRESS",
                    "progression": "OVERRIDE",
                    "save_ownership": "OVERRIDE"
                  }
                }
                """;

        ActorIntegrationOverlay overlay = ActorIntegrationOverlayLoader.parse(new StringReader(json));
        var report = ActorIntegrationOverlayValidator.validate(overlay);

        assertTrue(report.hasErrors());
        assertTrue(report.issues().stream()
                .anyMatch(issue -> "overlay.policy.capture_or_duplication".equals(issue.code())));
    }
}
