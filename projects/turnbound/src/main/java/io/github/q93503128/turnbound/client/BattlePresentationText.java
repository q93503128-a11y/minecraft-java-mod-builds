package io.github.q93503128.turnbound.client;

/**
 * Player-facing battle terminology only. Canonical combat descriptions and rule keys remain untouched.
 */
final class BattlePresentationText {
    private BattlePresentationText() {}

    static String skillDescription(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String text = raw;
        text = text.replace("Turn Gauge", "행동 게이지");
        text = text.replace("MaxHP", "최대 HP");
        text = text.replace("ATK", "공격력");
        text = text.replace("DEF", "방어력");
        text = text.replace("SPD", "속도");
        return text;
    }

    static String cooldownType(int baseCooldown) {
        return baseCooldown == 0 ? "기본 행동 · 쿨타임 없음" : "쿨타임 " + baseCooldown + "회";
    }
}
