package io.github.q93503128.turnbound.content;

/**
 * Presentation-only terminology cleanup for canonical combat copy.
 * Numeric meaning and internal rule identifiers are deliberately untouched.
 */
public final class PlayerFacingTerminology {
    private PlayerFacingTerminology() {}

    public static String mechanics(String value) {
        if (value == null || value.isBlank()) return value == null ? "" : value;
        return value
                .replace("Damage Reduction", "받는 피해 감소")
                .replace("MaxHP", "최대 HP")
                .replace("Gauge", "행동 게이지")
                .replace("Barrier", "보호막")
                .replace("Reaction", "반응 공격")
                .replace("Potency", "위력")
                .replace("Basic", "기본 행동")
                .replace("Active", "액티브")
                .replace("ATK", "공격력")
                .replace("DEF", "방어력")
                .replace("SPD", "속도");
    }
}
