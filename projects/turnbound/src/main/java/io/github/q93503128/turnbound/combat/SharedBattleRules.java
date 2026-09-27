package io.github.q93503128.turnbound.combat;

/** Canonical co-op PvE scaling rules. */
public final class SharedBattleRules {
    public static final int MAX_PLAYERS = 4;

    private SharedBattleRules() {}

    /** 1 player=x1 HP, 2=x2, 3=x3, 4=x4. ATK/DEF/SPD stay unchanged. */
    public static int enemyHpMultiplier(int playerCount) {
        validatePlayerCount(playerCount);
        return playerCount;
    }

    public static BattleState.Capacity capacity(int playerCount) {
        validatePlayerCount(playerCount);
        int regularAllies = playerCount * 4;
        int summons = playerCount;
        int enemies = 5;
        return new BattleState.Capacity(regularAllies, summons, enemies, regularAllies + summons + enemies);
    }

    public static CombatantDefinition scaleEnemyHp(CombatantDefinition base, int playerCount) {
        if (base == null) throw new IllegalArgumentException("base");
        int multiplier = enemyHpMultiplier(playerCount);
        BattleStats stats = base.stats();
        int hp = (int)Math.min(Integer.MAX_VALUE, Math.max(1L, (long)stats.maxHp() * multiplier));
        return new CombatantDefinition(
                base.id(), base.name(), new BattleStats(hp, stats.attack(), stats.defense(), stats.speed()),
                base.basicSkillId(), base.skills(), base.nativeStars(), base.rules(), base.params());
    }

    public static void validatePlayerCount(int playerCount) {
        if (playerCount < 1 || playerCount > MAX_PLAYERS) {
            throw new IllegalArgumentException("Shared battle player count must be 1.." + MAX_PLAYERS);
        }
    }
}
