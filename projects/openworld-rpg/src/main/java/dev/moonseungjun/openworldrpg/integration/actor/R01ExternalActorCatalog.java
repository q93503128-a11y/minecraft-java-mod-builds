package dev.moonseungjun.openworldrpg.integration.actor;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Exact R01 dependency registry surface proven against the pinned 26.2 creature JARs.
 *
 * <p>The pinned Alex's Mobs Cave Centipede is a multipart surface. Its authored R01 combat authority
 * belongs to the head only; body and tail registry targets are verified so donor multipart forwarding
 * can remain intact without creating duplicate project HP/poise owners. Nature Spirit keeps the
 * pinned donor's legacy {@code nature_hamony} registry spelling while the project-facing role remains
 * Nature Spirit. Registry/stat closure does not imply production-spawn acceptance.</p>
 */
public final class R01ExternalActorCatalog {
    public static final String GAZELLE = "alexsmobs:gazelle";
    public static final String BISON = "alexsmobs:bison";
    public static final String RACCOON = "alexsmobs:raccoon";
    public static final String CROW = "alexsmobs:crow";
    public static final String GRIZZLY = "alexsmobs:grizzly_bear";
    public static final String CAVE_CENTIPEDE_HEAD = "alexsmobs:centipede_head";
    public static final String CAVE_CENTIPEDE_BODY = "alexsmobs:centipede_body";
    public static final String CAVE_CENTIPEDE_TAIL = "alexsmobs:centipede_tail";

    public static final String LOUXIA = "threateningly_mobs:louxia";
    public static final String STEELBOAR = "threateningly_mobs:steelboar";
    public static final String NATURE_SPIRIT = "threateningly_mobs:nature_hamony";
    public static final String REGALHART = "threateningly_mobs:the_regalhart";
    public static final String EARTHLOONG = "threateningly_mobs:the_earthloong";

    private static final Set<String> REQUIRED_REGISTRY_TARGETS = Set.of(
            GAZELLE,
            BISON,
            RACCOON,
            CROW,
            GRIZZLY,
            CAVE_CENTIPEDE_HEAD,
            CAVE_CENTIPEDE_BODY,
            CAVE_CENTIPEDE_TAIL,
            LOUXIA,
            STEELBOAR,
            NATURE_SPIRIT,
            REGALHART,
            EARTHLOONG
    );

    private static final Map<String, String> COMBAT_OWNER_BY_TARGET = Map.of(
            CAVE_CENTIPEDE_BODY, CAVE_CENTIPEDE_HEAD,
            CAVE_CENTIPEDE_TAIL, CAVE_CENTIPEDE_HEAD
    );

    private static final Set<String> PRODUCTION_SPAWN_READY =
            Set.of(EARTHLOONG);

    private static final Set<String> PROJECT_LOOT_AUTHORITY_TARGETS = Set.of(
            BISON,
            GRIZZLY,
            CAVE_CENTIPEDE_HEAD,
            CAVE_CENTIPEDE_BODY,
            CAVE_CENTIPEDE_TAIL,
            STEELBOAR,
            NATURE_SPIRIT,
            REGALHART,
            EARTHLOONG
    );

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
                            CAVE_CENTIPEDE_HEAD,
                            4,
                            165.0F,
                            19.0,
                            10.0,
                            42.0,
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
                            NATURE_SPIRIT,
                            7,
                            790.0F,
                            25.0,
                            39.0,
                            78.0,
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

    /**
     * Returns the registry id that owns canonical project combat state for an attacked target.
     *
     * <p>Cave Centipede body/tail entities are hit proxies only. They route to the head instead of
     * receiving independent HP/poise pools.</p>
     */
    public static String combatOwnerId(String entityId) {
        if (entityId == null) {
            throw new IllegalArgumentException("entityId cannot be null.");
        }
        return COMBAT_OWNER_BY_TARGET.getOrDefault(entityId, entityId);
    }

    public static boolean multipartCombatProxy(String entityId) {
        return entityId != null && COMBAT_OWNER_BY_TARGET.containsKey(entityId);
    }

    public static boolean registryTargetClosed(String entityId) {
        return REQUIRED_REGISTRY_TARGETS.contains(entityId);
    }

    public static boolean productionSpawnReady(String entityId) {
        return PRODUCTION_SPAWN_READY.contains(entityId);
    }

    public static boolean projectOwnsLoot(String entityId) {
        return entityId != null && PROJECT_LOOT_AUTHORITY_TARGETS.contains(entityId);
    }
}
