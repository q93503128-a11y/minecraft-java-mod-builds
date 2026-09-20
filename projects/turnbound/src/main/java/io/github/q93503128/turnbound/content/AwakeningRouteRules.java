package io.github.q93503128.turnbound.content;

/**
 * v1 Awakening route classifier.
 *
 * <p>P01~P08 awaken through their personal character progression. Signature Equipment Trials are separate.
 * F01~F04 are legacy save-compatible identities and remain unavailable until promoted into fully authored v1 roles.</p>
 */
public final class AwakeningRouteRules {
    public enum Route { PERSONAL_QUEST, LEGACY_UNAVAILABLE }

    private static final String LEGACY_UNAVAILABLE =
            "현재 이 동료의 각성 경로는 열려 있지 않습니다.";

    private AwakeningRouteRules() { }

    public static boolean defined(String characterId) {
        return SignatureTrialCatalog.contains(characterId) || materialCharacter(characterId);
    }

    public static Route route(String characterId) {
        if (SignatureTrialCatalog.contains(characterId)) return Route.PERSONAL_QUEST;
        if (materialCharacter(characterId)) return Route.LEGACY_UNAVAILABLE;
        throw new IllegalArgumentException("Unknown Awakening route for " + characterId);
    }

    /** Retained for old callers; Signature Trial is no longer the Awakening prerequisite. */
    public static boolean signatureTrialRoute(String characterId) {
        return false;
    }

    public static boolean canonGap(String characterId) {
        return defined(characterId) && route(characterId) == Route.LEGACY_UNAVAILABLE;
    }

    public static String blockReason(String characterId) {
        return canonGap(characterId) ? LEGACY_UNAVAILABLE : "";
    }

    private static boolean materialCharacter(String characterId) {
        return characterId != null && characterId.matches("F0[1-4]");
    }
}
