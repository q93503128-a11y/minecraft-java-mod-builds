package kr.moonseungjun.senbonzakura.ability;

import java.util.Locale;

public enum ShowcaseAbility {
    SKYFALL("skyfall", "천락", 140, 220),
    WORLD_DIVIDE("world_divide", "공간절단", 90, 140),
    BLACK_SUN("black_sun", "흑일", 160, 260),
    SWORD_GRAVE("sword_grave", "천검묘", 150, 240),
    GRAVITY_REVERSAL("gravity_reversal", "역천", 120, 200),
    LAST_SECOND("last_second", "시간장례", 130, 220),
    HEAVEN_JUDGMENT("heaven_judgment", "백뢰강림", 125, 200),
    STELLAR_LANCE("stellar_lance", "성창", 110, 180);

    private final String id;
    private final String displayName;
    private final int durationTicks;
    private final int cooldownTicks;

    ShowcaseAbility(String id, String displayName, int durationTicks, int cooldownTicks) {
        this.id = id;
        this.displayName = displayName;
        this.durationTicks = durationTicks;
        this.cooldownTicks = cooldownTicks;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public int durationTicks() { return durationTicks; }
    public int cooldownTicks() { return cooldownTicks; }

    public static ShowcaseAbility byId(String value) {
        if (value == null) return null;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (ShowcaseAbility ability : values()) {
            if (ability.id.equals(normalized)) return ability;
        }
        return null;
    }
}
