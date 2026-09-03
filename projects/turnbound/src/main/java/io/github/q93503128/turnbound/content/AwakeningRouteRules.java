package io.github.q93503128.turnbound.content;

/**
 * Canon-facing classifier for character Awakening routes.
 *
 * <p>P01~P08 have authored Signature Trials. F01~F04 explicitly have Awakening effects but no Signature
 * Equipment/Trial, while the numeric wiki still requires a Signature Trial for Awakening. Until that conflict
 * is resolved, the material-character route must stay blocked instead of inventing a replacement challenge.</p>
 */
public final class AwakeningRouteRules {
    public enum Route { SIGNATURE_TRIAL, CANON_GAP }

    private static final String MATERIAL_CANON_GAP =
            "CANON GAP · 소재형 캐릭터의 각성 효과는 존재하지만, 전용 장비 시련 없이 각성을 여는 별도 조건은 아직 정해지지 않았습니다.";

    private AwakeningRouteRules() { }

    public static boolean defined(String characterId) {
        return SignatureTrialCatalog.contains(characterId) || materialCharacter(characterId);
    }

    public static Route route(String characterId) {
        if (SignatureTrialCatalog.contains(characterId)) return Route.SIGNATURE_TRIAL;
        if (materialCharacter(characterId)) return Route.CANON_GAP;
        throw new IllegalArgumentException("Unknown Awakening route for " + characterId);
    }

    public static boolean signatureTrialRoute(String characterId) {
        return route(characterId) == Route.SIGNATURE_TRIAL;
    }

    public static boolean canonGap(String characterId) {
        return defined(characterId) && route(characterId) == Route.CANON_GAP;
    }

    public static String blockReason(String characterId) {
        return canonGap(characterId) ? MATERIAL_CANON_GAP : "";
    }

    private static boolean materialCharacter(String characterId) {
        return characterId != null && characterId.matches("F0[1-4]");
    }
}
