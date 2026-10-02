package dev.moonseungjun.openworldrpg.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorCombatProfile;
import dev.moonseungjun.openworldrpg.integration.actor.R01ExternalActorCatalog;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class R01ExternalActorCatalogTest {
    @Test
    void pinnedRegistrySurfaceContainsOnlyClosedR01Targets() {
        assertEquals(9, R01ExternalActorCatalog.requiredRegistryTargets().size());
        assertTrue(R01ExternalActorCatalog.registryTargetClosed("alexsmobs:bison"));
        assertTrue(R01ExternalActorCatalog.registryTargetClosed("alexsmobs:grizzly_bear"));
        assertTrue(R01ExternalActorCatalog.registryTargetClosed("threateningly_mobs:louxia"));
        assertTrue(R01ExternalActorCatalog.registryTargetClosed("threateningly_mobs:steelboar"));
        assertTrue(R01ExternalActorCatalog.registryTargetClosed("threateningly_mobs:the_regalhart"));
        assertTrue(R01ExternalActorCatalog.registryTargetClosed("threateningly_mobs:the_earthloong"));

        assertFalse(R01ExternalActorCatalog.registryTargetClosed("alexsmobs:cave_centipede"));
        assertFalse(R01ExternalActorCatalog.registryTargetClosed("threateningly_mobs:nature_spirit"));
        assertFalse(R01ExternalActorCatalog.registryTargetClosed("threateningly_mobs:nature_hamony"));
    }

    @Test
    void combatProfilesMatchTheCanonicalR01StatTable() {
        Map<String, ExternalActorCombatProfile> profiles =
                R01ExternalActorCatalog.combatProfiles().stream()
                        .collect(Collectors.toMap(
                                ExternalActorCombatProfile::entityId,
                                Function.identity()
                        ));

        assertEquals(5, profiles.size());
        assertProfile(
                profiles.get(R01ExternalActorCatalog.BISON),
                3, 155.0, 23.0, 7.0, 50.0,
                ExternalActorCombatProfile.CombatRank.STURDY_COMMON
        );
        assertProfile(
                profiles.get(R01ExternalActorCatalog.GRIZZLY),
                6, 320.0, 24.0, 11.0, 58.0,
                ExternalActorCombatProfile.CombatRank.STURDY_COMMON
        );
        assertProfile(
                profiles.get(R01ExternalActorCatalog.STEELBOAR),
                6, 680.0, 37.0, 11.0, 82.0,
                ExternalActorCombatProfile.CombatRank.NORMAL_ELITE
        );
        assertProfile(
                profiles.get(R01ExternalActorCatalog.REGALHART),
                8, 6600.0, 29.0, 23.0, 180.0,
                ExternalActorCombatProfile.CombatRank.BOSS
        );
        assertProfile(
                profiles.get(R01ExternalActorCatalog.EARTHLOONG),
                8, 4900.0, 45.0, 35.0, 190.0,
                ExternalActorCombatProfile.CombatRank.BOSS
        );
    }

    private static void assertProfile(
            ExternalActorCombatProfile profile,
            int level,
            double hp,
            double defense,
            double mr,
            double poise,
            ExternalActorCombatProfile.CombatRank rank
    ) {
        assertEquals(level, profile.contentLevel());
        assertEquals(hp, profile.maxHealth(), 0.0001);
        assertEquals(defense, profile.defense(), 0.0001);
        assertEquals(mr, profile.magicResistance(), 0.0001);
        assertEquals(poise, profile.poiseMax(), 0.0001);
        assertEquals(rank, profile.combatRank());
    }
}
