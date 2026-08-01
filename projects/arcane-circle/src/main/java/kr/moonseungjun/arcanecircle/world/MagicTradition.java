
package kr.moonseungjun.arcanecircle.world;

public enum MagicTradition {
    UNBOUND("미선택", "아직 어느 학부에도 속하지 않았습니다.", 1.0, 1.0, 1.0, 1.0),
    ARCANE("비전학부", "논리·공식·순수 마력으로 세계의 법칙을 해석합니다.", 0.92, 1.12, 1.0, 0.96),
    DIVINE("성휘학부", "생명·보호·태양의 힘을 기도와 맹세로 다룹니다.", 0.96, 1.08, 1.0, 0.92),
    OCCULT("심령학부", "정신·환영·죽음·예언의 숨은 법칙을 파고듭니다.", 0.94, 1.10, 1.04, 0.90),
    PRIMAL("원초학부", "불·냉기·폭풍·대지와 생명의 원초적 순환을 따릅니다.", 0.95, 1.14, 1.12, 0.95);

    private final String displayName;
    private final String description;
    private final double mana;
    private final double power;
    private final double range;
    private final double cooldown;

    MagicTradition(String displayName, String description, double mana, double power,
                   double range, double cooldown) {
        this.displayName = displayName;
        this.description = description;
        this.mana = mana;
        this.power = power;
        this.range = range;
        this.cooldown = cooldown;
    }

    public String displayName() { return displayName; }
    public String description() { return description; }
    public double manaMultiplier() { return mana; }
    public double powerMultiplier() { return power; }
    public double rangeMultiplier() { return range; }
    public double cooldownMultiplier() { return cooldown; }

    public static MagicTradition parse(String value) {
        if (value == null || value.isBlank()) return UNBOUND;
        try { return valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException ignored) { return UNBOUND; }
    }
}
