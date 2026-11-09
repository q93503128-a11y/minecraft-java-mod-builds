package io.github.q93503128.turnbound.combat;

public record BattleStats(int maxHp, int attack, int defense, int speed) {
    public BattleStats {
        if (maxHp <= 0 || attack < 0 || defense < 0 || speed <= 0) throw new IllegalArgumentException("Invalid battle stats");
    }
}
