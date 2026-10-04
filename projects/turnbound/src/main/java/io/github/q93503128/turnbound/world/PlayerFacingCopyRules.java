package io.github.q93503128.turnbound.world;

/** Shared player-facing normalization for the J-full-map / M-minimap control contract. */
public final class PlayerFacingCopyRules {
    private PlayerFacingCopyRules() {}

    public static String normalizeMapKeys(String value) {
        if (value == null || value.isBlank()) return value == null ? "" : value;
        return value
                .replace("M키로 지도", "J 전체 지도로")
                .replace("M 키로 지도", "J 전체 지도로")
                .replace("지도 M키", "J 전체 지도")
                .replace("지도 M 키", "J 전체 지도")
                .replace("M키 지도", "J 전체 지도")
                .replace("M 키 지도", "J 전체 지도")
                .replace("M 지도", "J 전체 지도");
    }
}
