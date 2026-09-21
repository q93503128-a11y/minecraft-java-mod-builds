package dev.moonseungjun.openworldrpg.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.authority.CombatDamageAuthority;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorCombatProfile;
import dev.moonseungjun.openworldrpg.integration.overlay.ActorIntegrationOverlay;
import dev.moonseungjun.openworldrpg.integration.overlay.ActorIntegrationOverlayLoader;
import dev.moonseungjun.openworldrpg.integration.overlay.ActorIntegrationOverlayValidator;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class R01EarthloongBindingTest {
    @Test
    void canonicalOverlayHasCompleteOwnershipContract() {
        var stream = R01EarthloongBindingTest.class.getResourceAsStream(
                "/data/openworld_rpg/integration/actors/r01_earthloong.json"
        );
        assertTrue(stream != null);

        ActorIntegrationOverlay overlay = ActorIntegrationOverlayLoader.parse(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
        );

        assertFalse(ActorIntegrationOverlayValidator.validate(overlay).hasErrors());
        assertEquals("threateningly_mobs:the_earthloong", overlay.target());
        assertEquals("r01_dungeon_boss", overlay.role());
        assertTrue(overlay.required());
    }

    @Test
    void combatProfileFeedsProjectDamageAndPoiseTransaction() {
        ExternalActorCombatProfile profile = ExternalActorCombatProfile.r01Earthloong();
        ProjectImpactTransaction.DamageTargetSnapshot target = profile.projectTargetSnapshot();
        ProjectImpactTransaction.DamageSourceSnapshot explicitTestSource =
                new ProjectImpactTransaction.DamageSourceSnapshot(8, 30.0, 20.0, 0.0, 1.0);

        ProjectImpactTransaction.DirectDamageResult damage =
                CombatDamageAuthority.resolveProjectDirectDamage(
                        new ProjectImpactTransaction.DirectDamageRequest(
                                explicitTestSource,
                                target,
                                ProjectImpactTransaction.DamageSchool.MAGIC,
                                1.20,
                                1.0,
                                1.0
                        )
                );

        assertEquals(35.0, target.magicResistance());
        assertEquals(32.0, damage.finalDamage());

        ProjectImpactTransaction.PoiseResult poise = ProjectImpactTransaction.resolvePoise(
                new ProjectImpactTransaction.PoiseRequest(
                        target.poiseMax(),
                        target.poiseMax(),
                        explicitTestSource.poiseOutputMultiplier(),
                        0.50,
                        1.0,
                        1.0
                )
        );

        assertEquals(5.0, poise.poiseDamage());
        assertEquals(185.0, poise.remainingPoise());
        assertFalse(poise.broken());
    }

    @Test
    void canonicalCombatProfileMatchesR01EncounterCanon() {
        ExternalActorCombatProfile profile = ExternalActorCombatProfile.r01Earthloong();

        assertEquals("threateningly_mobs:the_earthloong", profile.entityId());
        assertEquals(8, profile.contentLevel());
        assertEquals(4900.0F, profile.maxHealth());
        assertEquals(45.0, profile.defense());
        assertEquals(35.0, profile.magicResistance());
        assertEquals(190.0, profile.poiseMax());
    }
}
