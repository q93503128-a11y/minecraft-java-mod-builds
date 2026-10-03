package dev.moonseungjun.openworldrpg.camp;

import dev.moonseungjun.openworldrpg.gathering.R01GatheringRules;
import java.util.Map;

public final class R01CampRules {
    public static final String KIT_ID = "openworld_rpg:utility/field_camp_kit";
    public static final String TOUGH_HIDE = "openworld_rpg:tough_hide";
    public static final Map<String, Integer> KIT_MATERIAL_COSTS =
            Map.of(
                    R01GatheringRules.HARDWOOD, 4,
                    TOUGH_HIDE, 2
            );
    public static final long KIT_GOLD_COST = 40L;

    public static final int DEPLOY_TICKS = 50;
    public static final int SUPPORT_SAMPLE_COUNT = 25;
    public static final int MIN_STABLE_SUPPORT_SAMPLES = 20;
    public static final double MAX_SUPPORT_HEIGHT_VARIANCE = 0.75;
    public static final double MIN_SHRINE_OR_SERVICE_DISTANCE = 24.0;
    public static final double MIN_OTHER_CAMP_DISTANCE = 12.0;
    public static final double MIN_BOSS_ARENA_DISTANCE = 24.0;
    public static final double MIN_CLEAR_APPROACH_WIDTH = 1.5;

    public static final String REST_SERVICE = "openworld_rpg:camp_service/rest";
    public static final String COOKING_SERVICE = "openworld_rpg:camp_service/cooking";
    public static final String MANAGEMENT_SERVICE = "openworld_rpg:camp_service/management";

    private R01CampRules() {
    }
}
