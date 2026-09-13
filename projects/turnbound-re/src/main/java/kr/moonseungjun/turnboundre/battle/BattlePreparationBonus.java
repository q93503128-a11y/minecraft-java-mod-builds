package kr.moonseungjun.turnboundre.battle;

import kr.moonseungjun.turnboundre.data.CharacterDefinition;

/**
 * Immutable, battle-local stat preparation applied before BattleParticipant creation.
 * It never mutates persisted CharacterProgress and deliberately leaves SPD untouched.
 */
public record BattlePreparationBonus(
        String id,
        int maxHpPercent,
        int attackPercent,
        int defensePercent,
        int poisePercent
) {
    public static final BattlePreparationBonus NONE = new BattlePreparationBonus("", 0, 0, 0, 0);
    private static final int MAX_PERCENT = 50;

    public BattlePreparationBonus {
        if (id == null) throw new IllegalArgumentException("preparation id must not be null");
        requirePercent("maxHpPercent", maxHpPercent);
        requirePercent("attackPercent", attackPercent);
        requirePercent("defensePercent", defensePercent);
        requirePercent("poisePercent", poisePercent);
        if (id.isBlank() && (maxHpPercent != 0 || attackPercent != 0 || defensePercent != 0 || poisePercent != 0)) {
            throw new IllegalArgumentException("blank preparation id may only represent NONE");
        }
    }

    public CharacterDefinition.Stats apply(CharacterDefinition.Stats base) {
        if (base == null) throw new IllegalArgumentException("base stats required");
        return new CharacterDefinition.Stats(
                scale(base.hp(), maxHpPercent),
                scale(base.atk(), attackPercent),
                scale(base.def(), defensePercent),
                base.spd(),
                scale(base.poise(), poisePercent));
    }

    public boolean active() {
        return !id.isBlank();
    }

    private static void requirePercent(String field, int value) {
        if (value < 0 || value > MAX_PERCENT) {
            throw new IllegalArgumentException(field + " must be 0.." + MAX_PERCENT);
        }
    }

    private static int scale(int base, int percent) {
        if (base < 0) throw new IllegalArgumentException("stat must be >= 0");
        long result = (long) base + ((long) base * percent) / 100L;
        if (result > Integer.MAX_VALUE) throw new IllegalArgumentException("prepared stat overflow");
        return (int) result;
    }
}
