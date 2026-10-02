package dev.moonseungjun.openworldrpg.integration.actor;

import java.util.List;
import java.util.Set;

/**
 * Exact R01 dependency registry surface proven against the pinned 26.2 creature JARs.
 *
 * <p>Cave Centipede is deliberately excluded because the pinned Alex's Mobs implementation uses a
 * multipart entity surface that still needs a dedicated binding review. Nature Spirit is also
 * excluded because the pinned Threateningly build exposes legacy NATURE_HAMONY naming that must be
 * resolved from its registry initializer before production binding. Neither is guessed here.</p>
 */
public final class R01ExternalActorCatalog {
    public static final String GAZELLE = "alexsmobs:gazelle";
    public static final String BISON = "alexsmobs:bison";
    public static final String RACCOON = "alexsmobs:raccoon";
    public static final String CROW = "alexsmobs:crow";
    public static final String GRIZZLY = "alexsmobs:grizzly_bear";

    public static final String LOUXIA = "threateningly_mobs:louxia";
    public static final String STEELBOAR = "threateningly_mobs:steelboar";
    public static final String REGALHART = "threateningly_mobs:the_regalhart";
    public static final String EARTHLOONG = "threateningly_mobs:the_earthloong";

    private static final Set<String> REQUIRED_REGISTRY_TARGETS = Set.of(
            GAZELLE,
            BISON,
            RACCOON,
            CROW,
            GRIZZLY,
            LOUXIA,
            STEELBOAR,
            REGALHART,
            EARTHLOONG
    );

    private static final Set<String> PRODUCTION_SPAWN_READY =
            Set.of(EARTHLOONG);

    private static final List<ExternalActorCombatProfile> COMBAT_PROFILES =
            List.of(
                    new ExternalActorCombatProfile(
                            BISON,
                            3,
                            155.0F,
                            23.0,
                            7.0,
                            50.0,
                            ExternalActorCombatProfile.CombatRank.STURDY_COMMON,
                            ExternalActorReactionCapabilities.none()
                    ),
                    new ExternalActorCombatProfile(
                            GRIZZLY,
                            6,
                            320.0F,
                            24.0,
                            11.0,
                            58.0,
                            ExternalActorCombatProfile.CombatRank.STURDY_COMMON,
                            ExternalActorReactionCapabilities.none()
                    ),
                    new ExternalActorCombatProfile(
                            STEELBOAR,
                            6,
                            680.0F,
                            37.0,
                            11.0,
                            82.0,
                            ExternalActorCombatProfile.CombatRank.NORMAL_ELITE,
                            ExternalActorReactionCapabilities.none()
                    ),
                    new ExternalActorCombatProfile(
                            REGALHART,
                            8,
                            6600.0F,
                            29.0,
                            23.0,
                            180.0,
                            ExternalActorCombatProfile.CombatRank.BOSS,
                            ExternalActorReactionCapabilities.none()
                    ),
                    ExternalActorCombatProfile.r01Earthloong()
            );

    private R01ExternalActorCatalog() {
    }

    public static Set<String> requiredRegistryTargets() {
        return REQUIRED_REGISTRY_TARGETS;
    }

    public static List<ExternalActorCombatProfile> combatProfiles() {
        return COMBAT_PROFILES;
    }

    public static boolean registryTargetClosed(String entityId) {
        return REQUIRED_REGISTRY_TARGETS.contains(entityId);
    }

    public static boolean productionSpawnReady(String entityId) {
        return PRODUCTION_SPAWN_READY.contains(entityId);
    }
}
