package io.github.q93503128.turnbound.combat;

public enum CombatantSide {
    ALLY, ENEMY;
    public CombatantSide opposite() { return this == ALLY ? ENEMY : ALLY; }
}
